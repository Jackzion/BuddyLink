package com.ziio.buddylink.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ziio.buddylink.common.ErrorCode;
import com.ziio.buddylink.exception.BusinessException;
import com.ziio.buddylink.manager.RedisBloomFilter;
import com.ziio.buddylink.model.domain.Blog;
import com.ziio.buddylink.model.domain.Comment;
import com.ziio.buddylink.model.domain.User;
import com.ziio.buddylink.model.request.CommentAddRequest;
import com.ziio.buddylink.model.request.DeleteRequest;
import com.ziio.buddylink.model.vo.CommentVO;
import com.ziio.buddylink.service.BlogService;
import com.ziio.buddylink.service.CommentService;
import com.ziio.buddylink.mapper.CommentMapper;
import com.ziio.buddylink.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author Ziio
* @description 针对表【comment(评论表)】的数据库操作Service实现
* @createDate 2024-08-18 19:12:11
*/
@Service
@Slf4j
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment>
    implements CommentService{

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private BlogService blogService;

    @Resource
    private RedisBloomFilter redisBloomFilter;

    @Override
    public List<CommentVO> listComments(Long blogId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        long userId = loginUser.getId();
        // 查询数据库
        List<Comment> commentList = this.lambdaQuery().eq(Comment::getBlogId, blogId).list();
        // 转换为VO
        return commentList.stream().map(comment -> {
            CommentVO commentVO = new CommentVO();
            BeanUtils.copyProperties(comment, commentVO);
            User user = userService.getById(comment.getUserId());
            commentVO.setUsername(user.getUsername());
            commentVO.setUserAvatarUrl(user.getAvatarUrl());
            commentVO.setMyComment(comment.getUserId() == userId);
            return commentVO;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean addComment(CommentAddRequest commentAddRequest, HttpServletRequest request) {
        // 获取参数效验
        String text = commentAddRequest.getText();
        Long blogId = commentAddRequest.getBlogId();
        User loginUser = userService.getLoginUser(request);
        long userId = loginUser.getId();
        if (StringUtils.isEmpty(text) || text.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论内容长于512");
        }
        if (!redisBloomFilter.blogIsContained(blogId)) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "博客不存在");
        }
        // 数据库 comment 保存数据
        Comment comment = new Comment();
        comment.setText(text);
        comment.setUserId(userId);
        comment.setBlogId(blogId);
        boolean save = this.save(comment);
        // 更新 blog 评论数 + 1
        UpdateWrapper<Blog> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id",blogId);
        updateWrapper.setSql("commentNum = commentNum + 1");
        boolean update = blogService.update(updateWrapper);
        if (!update) {
            log.error("增加博客：{} 评论数失败", blogId);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"blog 更新失败");
        }
        return save;
    }

    @Override
    public boolean deleteComment(DeleteRequest deleteRequest, HttpServletRequest request) {
        // 获得参数
        Long id = deleteRequest.getId();
        User loginUser = userService.getLoginUser(request);
        long userId = loginUser.getId();
        // 参数效验
        Comment comment = this.getById(id);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "评论不存在");
        }
        if (!userService.isAdmin(request) && userId != comment.getUserId()) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限");
        }
        // 删除评论
        boolean b = this.removeById(id);
        return b;
    }

    @Override
    public boolean isMyComment(long userId, long commentId) {
        Comment comment = this.getById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "评论不存在");
        }
        return userId == comment.getUserId();
    }}




