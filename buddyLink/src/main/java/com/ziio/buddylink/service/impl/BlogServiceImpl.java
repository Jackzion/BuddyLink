package com.ziio.buddylink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ziio.buddylink.common.ErrorCode;
import com.ziio.buddylink.constant.RedisConstant;
import com.ziio.buddylink.exception.BusinessException;
import com.ziio.buddylink.manager.RedisBloomFilter;
import com.ziio.buddylink.model.domain.Blog;
import com.ziio.buddylink.model.domain.Comment;
import com.ziio.buddylink.model.domain.Message;
import com.ziio.buddylink.model.domain.User;
import com.ziio.buddylink.model.request.*;
import com.ziio.buddylink.model.vo.BlogUserVO;
import com.ziio.buddylink.model.vo.BlogVO;
import com.ziio.buddylink.model.vo.CommentVO;
import com.ziio.buddylink.service.*;
import com.ziio.buddylink.mapper.BlogMapper;
import com.ziio.buddylink.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Set;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addBlog(BlogAddRequest blogAddRequest, HttpServletRequest request) {
        // 获取参数
        User loginUser = userService.getLoginUser(request);
        long userId = loginUser.getId();
        String title = blogAddRequest.getTitle();
        String coverImage = blogAddRequest.getCoverImage();
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
        if (StringUtils.isBlank(coverImage) || coverImage.length() > 256) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "封面图片未上传或图片链接太长");
        }
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
        blog.setCoverImage(coverImage);
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
        String title = blogQueryRequest.getTitle();
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
        if (starred || isStarred(blogId, userId)) {
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


}




