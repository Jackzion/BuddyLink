package com.ziio.buddylink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ziio.buddylink.common.ErrorCode;
import com.ziio.buddylink.constant.RedisConstant;
import com.ziio.buddylink.exception.BusinessException;
import com.ziio.buddylink.manager.RedisBloomFilter;
import com.ziio.buddylink.model.domain.Blog;
import com.ziio.buddylink.model.domain.Comment;
import com.ziio.buddylink.model.domain.Message;
import com.ziio.buddylink.model.domain.User;
import com.ziio.buddylink.model.es.UserEsDTO;
import com.ziio.buddylink.model.request.*;
import com.ziio.buddylink.model.vo.BlogUserVO;
import com.ziio.buddylink.model.vo.BlogVO;
import com.ziio.buddylink.model.vo.CommentVO;
import com.ziio.buddylink.model.vo.UserBlogVO;
import com.ziio.buddylink.service.*;
import com.ziio.buddylink.mapper.BlogMapper;
import com.ziio.buddylink.service.blogInteractionStrategy.BlogInteractionContext;
import com.ziio.buddylink.service.blogInteractionStrategy.BlogInteractionStrategy;
import com.ziio.buddylink.service.blogInteractionStrategy.impl.BlogLikedStrategy;
import com.ziio.buddylink.service.blogInteractionStrategy.impl.BlogStarredStrategy;
import com.ziio.buddylink.service.blogInteractionStrategy.impl.BlogViewedStrategy;
import com.ziio.buddylink.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author Ziio
* @description 针对表【blog(博客表)】的数据库操作Service实现
* @createDate 2024-08-18 17:11:33
*/
@Service
@Slf4j
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog>
    implements BlogService{

    private static final double TIME_UNIT = 1000.0;

    @Resource
    private UserService userService;

    @Resource
    private CommentService commentService;

    @Resource
    private FollowService followService;

    @Resource
    private MessageService messageService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisBloomFilter redisBloomFilter;

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Resource
    private BlogMapper blogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addBlog(BlogAddRequest blogAddRequest, HttpServletRequest request) {
        // 获取参数
        User loginUser = userService.getLoginUser(request);
        long userId = loginUser.getId();
        String title = blogAddRequest.getTitle();
        List<String> images = blogAddRequest.getImages();
        String content = blogAddRequest.getContent();
        List<String> tags = blogAddRequest.getTags();
        // 1. 效验参数
        loginUser = userService.getById(userId);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        if (title.length() < 1 || title.length() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题过长");
        }
//        todo:图片上传默认值
//        if (StringUtils.isBlank(coverImage) || coverImage.length() > 256) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "封面图片未上传或图片链接太长");
//        }
        if (images.size() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片过多");
        }
        if (StringUtils.isBlank(content) || content.length() > 100000) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "博客内容长度超过 100000");
        }
        if (tags.size() > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签不得超过 5 个");
        }
        // 2. 保存到数据库
        Blog blog = new Blog();
        blog.setTitle(title);
        blog.setContent(content);
        blog.setUserId(userId);
        // 处理 tags ,..( list to jsonStr)
        blog.setTags(JsonUtils.ListToJson(tags));
        blog.setImages(JsonUtils.ListToJson(images));
        boolean save = this.save(blog);
        if (!save) {
            log.error("用户：{} 创建博客失败", userId);
        } else {
        // 3. update user
            User user = new User();
            user.setId(userId);
            user.setBlogNum(loginUser.getBlogNum() + 1);
            user.setScore(loginUser.getScore() + 10);
            boolean updateUser = userService.updateById(user);
            if (!updateUser) {
                log.error("用户：{} 发布博客：{}后， 更新博客数量和积分失败", userId, blog.getId());
            }
        }
        // 4. 添加 blog id 到布隆过滤器
        redisBloomFilter.addBlogToFilter(blog.getId());
        return blog.getId();
    }

    @Override
    public List<BlogVO> listBlogs(BlogQueryRequest blogQueryRequest, HttpServletRequest request) {
        // 获取参数
        // todo : 改为 es 查询 ，非数据库模糊查询
        String title = blogQueryRequest.getSearchText();
        int pageSize = blogQueryRequest.getPageSize();
        int pageNum = blogQueryRequest.getPageNum();
        long start = (pageNum - 1) * pageSize;
        long end = pageSize;
        // 查询数据库 start to end
        QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotEmpty(title), "title", title);
        List<Blog> blogList = this.baseMapper.selectBlogByPage(start, end, title);
        List<BlogVO> bloVOsList = blogList.stream().map(blog -> {
            // 封装为 blogVO
            // todo: 没有补充 点赞 ， 收藏
            BlogVO blogVO = new BlogVO();
            BeanUtils.copyProperties(blog, blogVO);
            // 补充 userVo
            User user = userService.getById(blog.getUserId());
            BlogUserVO blogUserVO = new BlogUserVO();
            BeanUtils.copyProperties(user, blogUserVO);
            blogVO.setBlogUserVO(blogUserVO);
            return blogVO;
        }).collect(Collectors.toList());
        return bloVOsList;
    }

    @Override
    public List<BlogVO> listBlogsFromEs(BlogQueryRequest blogQueryRequest, HttpServletRequest request) {
        // 获取参数
        String searchText = blogQueryRequest.getSearchText();
        int pageSize = blogQueryRequest.getPageSize();
        int pageNum = blogQueryRequest.getPageNum();
        // es 查询
        // 构造 query
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.filter(QueryBuilders.termQuery("isdelete", 0));
        if(searchText != null){
            boolQueryBuilder.should(QueryBuilders.matchQuery("profile", searchText));
            boolQueryBuilder.should(QueryBuilders.matchQuery("tags", searchText));
            boolQueryBuilder.should(QueryBuilders.matchQuery("userName", searchText));
            boolQueryBuilder.minimumShouldMatch(1);
        }
        // 分页
        org.springframework.data.domain.PageRequest pageRequest = PageRequest.of(pageNum-1, pageSize);
        // 排序器 , 默认按 相似度 score 排序
        SortBuilder<?> sortBuilder = SortBuilders.scoreSort().order(SortOrder.DESC);
        // 构造查询
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(pageRequest)
                .withSorts(sortBuilder).build();
        // 查找并拆分
        SearchHits<UserEsDTO> searchHits = elasticsearchRestTemplate.search(searchQuery, UserEsDTO.class);
        List<SearchHit<UserEsDTO>> searchHits1 = searchHits.getSearchHits();
        List<Long> esIdList = searchHits1.stream().map(searchHit -> searchHit.getContent().getId()).collect(Collectors.toList());
        System.out.println(esIdList);
        // 从数据库获取完整数据
        List<Blog> blogList = blogMapper.selectBatchIds(esIdList);
        // 对 blogList 重排序
        List<Blog> sortBlogList = blogList.stream()
                .sorted(Comparator.comparingInt(blog -> esIdList.indexOf(blog.getId()))).collect(Collectors.toList());
        // 封装为 blogVO
        List<BlogVO> blogVOList = blogList.stream().map(blog -> {
            // 封装为 blogVO
            // todo: 没有补充 点赞 ， 收藏
            BlogVO blogVO = new BlogVO();
            BeanUtils.copyProperties(blog, blogVO);
            // 补充 userVo
            User user = userService.getById(blog.getUserId());
            BlogUserVO blogUserVO = new BlogUserVO();
            BeanUtils.copyProperties(user, blogUserVO);
            blogVO.setBlogUserVO(blogUserVO);
            return blogVO;
        }).collect(Collectors.toList());
        return blogVOList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BlogVO getBlogDetailById(Long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        long userId = loginUser.getId();
        // 效验 blog 是否存在
        if (!redisBloomFilter.blogIsContained(id)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该博客不存在");
        }
        Blog blog = this.getById(id);
        if (blog == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该博客不存在");
        }
        // 封装为 blogVO
        Long blogId = blog.getId();
        BlogVO blogVO = new BlogVO();
        BlogUserVO blogUserVO = new BlogUserVO();
        BeanUtils.copyProperties(blog, blogVO);
        Long blogUserId = blog.getUserId();
        User user = userService.getById(blogUserId);
        BeanUtils.copyProperties(user, blogUserVO);
        // 查询博客作者的博客数、粉丝数、总浏览量和是否关注他
        // todo 将查询作者的博客数粉丝数，总浏览量都改为查询数据库（发布博客后，关注后，浏览后都要把数据存在数据库中）
        blogUserVO.setFollowed(followService.isFollowed(blogUserId, userId));
        blogVO.setBlogUserVO(blogUserVO);
        // 查询博客是否点赞、收藏
        blogVO.setIsLiked(isLiked(blogId, userId));
        blogVO.setIsStarred(isStarred(blogId, userId));
        // 查询博客的相关评论
        List<CommentVO> commentVOList = commentService.listComments(blogId, request);
        blogVO.setCommentVOList(commentVOList);
        // todo 后续改为 MQ 处理
        Boolean isViewed = stringRedisTemplate.opsForSet().isMember(RedisConstant.REDIS_BLOG_VIEW_KEY + blogId, String.valueOf(userId));
        // 没浏览过，增加总浏览量
        if (isViewed!=null && !isViewed) {
            // 更新 redis 博客的被浏览记录
            stringRedisTemplate.opsForSet().add(RedisConstant.REDIS_BLOG_VIEW_KEY + blogId, String.valueOf(userId));
            // 更新 mysql 博客的浏览量啊
            Blog newBlog = new Blog();
            newBlog.setId(blog.getId());
            newBlog.setViewNum(blog.getViewNum() + 1);
            boolean flag = this.updateById(newBlog);
            if (!flag) {
                log.error("用户：{} 浏览博客：{} 后，更新博客的浏览量失败了", userId, blogId);
            }
        }
        // 增加用户的浏览记录 (zset 记录 ，毫秒时间搓为分数)
        stringRedisTemplate.opsForZSet().add(RedisConstant.REDIS_USER_VIEW_BLOG_KEY + userId,
                String.valueOf(blogId), Instant.now().toEpochMilli() / TIME_UNIT);
        return blogVO;
    }

    // 判断是否收藏
    @Override
    public boolean isStarred(long blogId, long userId) {
        Boolean b1 = stringRedisTemplate.opsForSet().isMember(RedisConstant.REDIS_USER_STAR_BLOG_KEY + userId, String.valueOf(blogId));
        Boolean b2 = stringRedisTemplate.opsForSet().isMember(RedisConstant.REDIS_BLOG_STAR_KEY + blogId, String.valueOf(userId));
        // 博客收藏 set 和用户收藏 set 比如都存在对方，否则删除
        // todo : 不一致，删除 redis blog id
        if (b1 != null && !b1 && b2 != null && b2) {
            stringRedisTemplate.opsForSet().remove(RedisConstant.REDIS_BLOG_STAR_KEY + blogId, String.valueOf(userId));
        }
        if (b2 != null && !b2 && b1 != null && b1) {
            stringRedisTemplate.opsForSet().remove(RedisConstant.REDIS_USER_STAR_BLOG_KEY + userId, String.valueOf(blogId));
        }
        return b1 != null && b1 && b2 != null && b2;
    }

    // 判断是否点赞
    @Override
    public boolean isLiked(long blogId, long userId) {
        Boolean b1 = stringRedisTemplate.opsForSet().isMember(RedisConstant.REDIS_USER_LIKE_BLOG_KEY + userId, String.valueOf(blogId));
        Boolean b2 = stringRedisTemplate.opsForSet().isMember(RedisConstant.REDIS_BLOG_LIKE_KEY + blogId, String.valueOf(userId));
        // 博客收藏 set 和用户收藏 set 比如都存在对方，否则删除
        // todo : 不一致，删除 redis blog id
        if (b1 != null && !b1 && b2 != null && b2) {
            stringRedisTemplate.opsForSet().remove(RedisConstant.REDIS_BLOG_LIKE_KEY + blogId, String.valueOf(userId));
        }
        if (b2 != null && !b2 && b1 != null && b1) {
            stringRedisTemplate.opsForSet().remove(RedisConstant.REDIS_USER_LIKE_BLOG_KEY + userId, String.valueOf(blogId));
        }
        return b1 != null && b1 && b2 != null && b2;
    }

    @Override
    public boolean deleteBlog(DeleteRequest deleteRequest, HttpServletRequest request) {
        // 获取参数
        User loginUser = userService.getLoginUser(request);
        long userId = loginUser.getId();
        long id = deleteRequest.getId();
        // 效验
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "博客不存在");
        }
        Blog blog = this.getById(id);
        if (blog == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "博客不存在");
        }
        // 身份效验
        if (blog.getUserId() != userId && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "你没有权限删除该博客");
        }
        // 数据库中删除
        boolean b = this.removeById(id);
        if (!b) {
            log.error("用户：{} 删除博客 {} 失败", userId, id);
        }
        // 删除和博客相关的记录，用户关于此博客的浏览记录、点赞记录、收藏记录（redis)
        // 获取 view set
        Set<String> viewedMembersId = null;
        Boolean hasKey1 = stringRedisTemplate.hasKey(RedisConstant.REDIS_BLOG_VIEW_KEY + id);
        if(hasKey1!=null && hasKey1){
            viewedMembersId = stringRedisTemplate.opsForSet().members(RedisConstant.REDIS_BLOG_VIEW_KEY + id);
        }
        // 获取 like set
        Set<String> likedMembersId = null;
        Boolean hasKey2 = stringRedisTemplate.hasKey(RedisConstant.REDIS_BLOG_LIKE_KEY + id);
        if (hasKey2 != null && hasKey2) {
            likedMembersId = stringRedisTemplate.opsForSet().members(RedisConstant.REDIS_BLOG_LIKE_KEY + id);
        }
        // 获取 star set
        Set<String> starredMembersId = null;
        Boolean hasKey3 = stringRedisTemplate.hasKey(RedisConstant.REDIS_BLOG_STAR_KEY + id);
        if (hasKey3 != null && hasKey3) {
            starredMembersId = stringRedisTemplate.opsForSet().members(RedisConstant.REDIS_BLOG_STAR_KEY + id);
        }
        // 删除其他用户博客浏览记录、点赞记录、收藏记录 , delete zset (key--userId , value--blogId)
        if (!CollectionUtils.isEmpty(viewedMembersId)) {
            for (String viewedMemberId : viewedMembersId) {
                stringRedisTemplate.opsForZSet().remove(RedisConstant.REDIS_USER_VIEW_BLOG_KEY + Long.parseLong(viewedMemberId), String.valueOf(id));
            }
        }
        if (!CollectionUtils.isEmpty(likedMembersId)) {
            for (String likedMemberId : likedMembersId) {
                stringRedisTemplate.opsForSet().remove(RedisConstant.REDIS_USER_LIKE_BLOG_KEY + Long.parseLong(likedMemberId), String.valueOf(id));
            }
        }
        if (!CollectionUtils.isEmpty(starredMembersId)) {
            for (String starredMemberId : starredMembersId) {
                stringRedisTemplate.opsForSet().remove(RedisConstant.REDIS_USER_STAR_BLOG_KEY + Long.parseLong(starredMemberId), String.valueOf(id));
            }
        }
        // 删除博客浏览记录、点赞记录、收藏记录
        stringRedisTemplate.delete(RedisConstant.REDIS_BLOG_STAR_KEY + id);
        stringRedisTemplate.delete(RedisConstant.REDIS_BLOG_LIKE_KEY + id);
        stringRedisTemplate.delete(RedisConstant.REDIS_BLOG_VIEW_KEY + id);
        // 删除评论数据
        QueryWrapper<Comment> queryWrapper = new QueryWrapper();
        queryWrapper.eq("blogId", id);
        boolean remove = commentService.remove(queryWrapper);
        if (!remove) {
            log.error("用户：{} 删除博客 {} 后，删除博客的评论记录失败了", userId, id);
        }
        return b;
    }

    @Override
    public boolean starBlog(StarRequest starRequest, HttpServletRequest request) {
        // 获取参数
        User loginUser = userService.getLoginUser(request);
        long userId = loginUser.getId();
        long blogId = starRequest.getBlogId();
        boolean starred = starRequest.getIsStarred();
        Blog blog = this.getById(blogId);
        // 校验参数
        if (blog == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "博客不存在");
        }
        // 判断用户是否已经收藏
        if (starred || isStarred(blogId, userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您已收藏");
        }
        // 添加用户收藏记录
        Long count1 = stringRedisTemplate.opsForSet()
                .add(RedisConstant.REDIS_USER_STAR_BLOG_KEY + userId, String.valueOf(blogId));
        // 添加博客收藏记录
        Long count2 = stringRedisTemplate.opsForSet()
                .add(RedisConstant.REDIS_BLOG_STAR_KEY + blogId, String.valueOf(userId));
        if (count1 == null || count1 < 1) {
            log.error("用户：{} 收藏博客：{} 失败了！", userId, blogId);
        }
        if (count2 == null || count2 < 1) {
            log.error("博客：{} 收藏添加用户：{} 失败了！", blogId, userId);
        }
        // 更新数据库 , blog starNum + 1
        // todo 后续改为 MQ 处理
        if (count1 != null && count1 > 0 && count2 != null && count2 > 0) {
            UpdateWrapper<Blog> updateWrapper = new UpdateWrapper<>();
            updateWrapper.setSql("starNum = starNum + 1");
            updateWrapper.eq("id", blogId);
            boolean update = this.update(updateWrapper);
            if (!update) {
                log.error("用户：{} 收藏博客：{} 后，更新博客收藏数失败了！", userId, blogId);
            }
        }
        // 发送消息,添加收藏消息到消息表
        Message message = new Message();
        message.setFromId(userId);
        message.setToId(blog.getUserId());
        message.setType(0);
        message.setText("收藏了你的博客");
        message.setBlogId(blogId);
        messageService.addStarMessage(message);
        return (count1 != null && count1 >= 1) && (count2 != null && count2 >= 1);
    }

    @Override
    public boolean likeBlog(LikeRequest likeRequest, HttpServletRequest request) {
        // 获取参数
        User loginUser = userService.getLoginUser(request);
        long userId = loginUser.getId();
        long blogId = likeRequest.getBlogId();
        boolean liked = likeRequest.getIsLiked();
        Blog blog = this.getById(blogId);
        // 校验参数
        if (blog == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "博客不存在");
        }
        // 判断是否已经点赞
        if (liked || isLiked(blogId,userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您已点赞过");
        }
        // 添加用户喜欢记录
        Long count1 = stringRedisTemplate.opsForSet()
                .add(RedisConstant.REDIS_USER_LIKE_BLOG_KEY + userId, String.valueOf(blogId));
        // 添加blog喜欢记录
        Long count2 = stringRedisTemplate.opsForSet()
                .add(RedisConstant.REDIS_BLOG_LIKE_KEY + blogId, String.valueOf(userId));
        if (count1 == null || count1 < 1) {
            log.error("用户：{} 点赞博客：{} 失败了！", userId, blogId);
        }
        if (count2 == null || count2 < 1) {
            log.error("博客：{} 点赞添加用户：{} 失败了！", blogId, userId);
        }
        // 更新数据库 blog likes 记录
        // todo 后续改为 MQ 处理
        if (count1 != null && count1 > 0 && count2 != null && count2 > 0) {
            UpdateWrapper<Blog> updateWrapper = new UpdateWrapper<>();
            updateWrapper.setSql("likeNum = likeNum + 1");
            updateWrapper.eq("id", blogId);
            boolean update = this.update(updateWrapper);
            if (!update) {
                log.error("用户：{} 点赞博客：{} 后，更新博客点赞数失败了！", userId, blogId);
            }
        }
        // 发送消息,添加点赞消息到消息表
        Message message = new Message();
        message.setFromId(userId);
        message.setToId(blog.getUserId());
        message.setType(1);
        message.setText("点赞了你的博客");
        message.setBlogId(blogId);
        messageService.addLikeMessage(message);
        return (count1 != null && count1 >= 1) && (count2 != null && count2 >= 1);
    }

    @Override
    public boolean cancelStarBlog(StarRequest starRequest, HttpServletRequest request) {
        // 获取参数
        User loginUser = userService.getLoginUser(request);
        long userId = loginUser.getId();
        long blogId = starRequest.getBlogId();
        boolean starred = starRequest.getIsStarred();
        Blog blog = this.getById(blogId);
        // 校验参数
        if (blog == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "博客不存在");
        }
        // 判断用户是否已经收藏
        if (!starred || !isStarred(blogId, userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您未收藏");
        }
        // 删除用户收藏记录
        Long count1 = stringRedisTemplate.opsForSet()
                .remove(RedisConstant.REDIS_USER_STAR_BLOG_KEY + userId, String.valueOf(blogId));
        // 删除博客收藏记录
        Long count2 = stringRedisTemplate.opsForSet()
                .remove(RedisConstant.REDIS_BLOG_STAR_KEY + blogId, String.valueOf(userId));
        // 更新数据库 , blog starNum - 1
        // todo 后续改为 MQ 处理
        if (count1 != null && count1 > 0 && count2 != null && count2 > 0) {
            UpdateWrapper<Blog> updateWrapper = new UpdateWrapper<>();
            updateWrapper.setSql("starNum = starNum - 1");
            updateWrapper.eq("id", blogId);
            boolean update = this.update(updateWrapper);
            if (!update) {
                log.error("用户：{} 收藏博客：{} 后，更新博客收藏数失败了！", userId, blogId);
            }
        }
        return (count1 != null && count1 >= 1) && (count2 != null && count2 >= 1);
    }

    @Override
    public boolean cancelLikeBlog(LikeRequest likeRequest, HttpServletRequest request) {
        // 获取参数
        User loginUser = userService.getLoginUser(request);
        long userId = loginUser.getId();
        long blogId = likeRequest.getBlogId();
        boolean liked = likeRequest.getIsLiked();
        Blog blog = this.getById(blogId);
        // 校验参数
        if (blog == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "博客不存在");
        }
        // 判断是否没有点赞
        if (!liked || !isLiked(blogId,userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您未点赞过");
        }
        // 删除用户喜欢记录
        Long count1 = stringRedisTemplate.opsForSet()
                .remove(RedisConstant.REDIS_USER_LIKE_BLOG_KEY + userId, String.valueOf(blogId));
        // 删除blog喜欢记录
        Long count2 = stringRedisTemplate.opsForSet()
                .remove(RedisConstant.REDIS_BLOG_LIKE_KEY + blogId, String.valueOf(userId));
        // 更新数据库 blog likes 记录
        // todo 后续改为 MQ 处理
        if (count1 != null && count1 > 0 && count2 != null && count2 > 0) {
            UpdateWrapper<Blog> updateWrapper = new UpdateWrapper<>();
            updateWrapper.setSql("likeNum = likeNum - 1");
            updateWrapper.eq("id", blogId);
            boolean update = this.update(updateWrapper);
            if (!update) {
                log.error("用户：{} 点赞博客：{} 后，更新博客点赞数失败了！", userId, blogId);
            }
        }
        return (count1 != null && count1 >= 1) && (count2 != null && count2 >= 1);
    }

    @Override
    public List<BlogVO> listUserBlogs(Long id, BlogQueryRequest blogQueryRequest, HttpServletRequest request) {
        // 提取效验参数
        User loginUser = userService.getLoginUser(request);
        long loginUserId = loginUser.getId();
        String title = blogQueryRequest.getSearchText();
        int pageSize = blogQueryRequest.getPageSize();
        int pageNum = blogQueryRequest.getPageNum();
        if (!redisBloomFilter.userIsContained(id)) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "用户不存在");
        }
        // todo : userId 统一用于查询对象如何？ queryWrapper 不生效问题
        // blog 分页查询 ， myself
        QueryWrapper<Blog> queryWrapper = null;
        if (loginUserId == id) {
            queryWrapper = new QueryWrapper<>();
            queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
            queryWrapper.eq("userId", loginUserId);
            List<Blog> blogList = this.page(new Page<>(pageNum, pageSize), queryWrapper).getRecords();
            User user = userService.getById(loginUserId);
            // 补充 UserVo
            return blogList.stream().map(blog -> {
                BlogVO blogVO = new BlogVO();
                BeanUtils.copyProperties(blog, blogVO);
                BlogUserVO blogUserVO = new BlogUserVO();
                BeanUtils.copyProperties(user, blogUserVO);
                blogVO.setBlogUserVO(blogUserVO);
                return blogVO;
            }).collect(Collectors.toList());
        }
        // blog 分页查询 ， other
        queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.eq("userId", id);
        List<Blog> blogList = this.page(new Page<>(pageNum, pageSize), queryWrapper).getRecords();
        User user = userService.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "用户存在");
        }
        return blogList.stream().map(blog -> {
            BlogVO blogVO = new BlogVO();
            BeanUtils.copyProperties(blog, blogVO);
            BlogUserVO blogUserVO = new BlogUserVO();
            BeanUtils.copyProperties(user, blogUserVO);
            blogVO.setBlogUserVO(blogUserVO);
            return blogVO;
        }).collect(Collectors.toList());
    }

    @Override
    public List<BlogVO> listInteractionBlogs(BlogQueryRequest blogQueryRequest, HttpServletRequest request) {
        // 提取效验参数
        String title = blogQueryRequest.getSearchText();
        Long userId = blogQueryRequest.getUserId();
        Integer type = blogQueryRequest.getType();
        int pageSize = blogQueryRequest.getPageSize();
        int pageNum = blogQueryRequest.getPageNum();
        User loginUser = userService.getLoginUser(request);
        long loginUserId = loginUser.getId();
        if (type == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Set<String> blogStringIds = null;
        // 查询谁的博客的 map
        Map<Integer, Long> blogInteractionUserMap = new LinkedHashMap<>();
        blogInteractionUserMap.put(0, loginUserId);
        blogInteractionUserMap.put(1, userId);
        // 查询博客类型的 map
        Map<Integer, BlogInteractionStrategy> blogInteractionTypeMap = new LinkedHashMap<>();
        blogInteractionTypeMap.put(0, new BlogStarredStrategy());
        blogInteractionTypeMap.put(1, new BlogLikedStrategy());
        blogInteractionTypeMap.put(2, new BlogViewedStrategy());
        // 选取 策略类型 %3
        BlogInteractionContext blogInteractionContext = new BlogInteractionContext(blogInteractionTypeMap.get(this.getSelectBlogType(type)));
        //  选取 user类型 /3 , 查询博客 ids
        blogStringIds = blogInteractionContext.blogInteractionMethod(blogQueryRequest, stringRedisTemplate,
                blogInteractionUserMap.get(this.getSelectUserType(type)));
        // 如果没有任何收藏或者点赞的博客 id，就直接返回空的 List
        if (CollectionUtils.isEmpty(blogStringIds)) {
            return new ArrayList<>();
        }
        // 查询 blogs , todo: 没模糊查询 title ， 查全部
        List<Long> blogIds = blogStringIds.stream().map(Long::valueOf).collect(Collectors.toList());
        List<Blog> blogList = this.listByIds(blogIds);
        // 封装为 blogVOs
        return blogList.stream().map(blog -> {
            BlogVO blogVO = new BlogVO();
            BeanUtils.copyProperties(blog, blogVO);
            BlogUserVO blogUserVO = new BlogUserVO();
            User user = userService.getById(blog.getUserId());
            BeanUtils.copyProperties(user, blogUserVO);
            blogVO.setBlogUserVO(blogUserVO);
            return blogVO;
        }).collect(Collectors.toList());
    }

    @Override
    public UserBlogVO listUserInteractionBlogs(BlogQueryRequest blogQueryRequest, HttpServletRequest request) {
        // 提取效验参数
        User loginUser = userService.getLoginUser(request);
        long loginUserId = loginUser.getId();
        Long userId = blogQueryRequest.getUserId();
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "用户不存在");
        }
        UserBlogVO userBlogVO = new UserBlogVO();
        // 封装 blogUserVO
        BlogUserVO blogUserVO = new BlogUserVO();
        BeanUtils.copyProperties(user, blogUserVO);
        blogUserVO.setFollowed(followService.isFollowed(userId, loginUserId));
        // 查找 blogVOList
        List<BlogVO> blogVOList = null;
        if (blogQueryRequest.getType() == -1) {
            // 查 userId 创建的
            blogVOList = listUserBlogs(userId, blogQueryRequest, request);
        } else {
            // 查 点赞，收藏，浏览过的
            blogVOList = listInteractionBlogs(blogQueryRequest, request);
        }
        userBlogVO.setBlogVOList(blogVOList);
        userBlogVO.setBlogUserVO(blogUserVO);
        return userBlogVO;
    }

    @Override
    public long editBlog(BlogEditRequest blogEditRequest, HttpServletRequest request) {
        List<String> tags = blogEditRequest.getTags();
        List<String> images = blogEditRequest.getImages();
        Blog blog = new Blog();
        BeanUtils.copyProperties(blogEditRequest, blog);
        // 对 tags ， images 进行 list -> json string 转换
        if(!CollectionUtils.isEmpty(tags)) {
            String tagsStr = JsonUtils.ListToJson(tags);
            blog.setTags(tagsStr);
        }
        if(!CollectionUtils.isEmpty(images)) {
            String imagesStr = JsonUtils.ListToJson(images);
            blog.setImages(imagesStr);
        }
        // 保存 blog
        boolean b = this.updateById(blog);
        if (!b) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return blog.getId();
    }

    private int getSelectUserType(int type) {
        return type / 3;
    }

    private int getSelectBlogType(int type) {
        return type % 3;
    }
}




