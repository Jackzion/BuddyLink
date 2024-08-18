package com.ziio.buddylink.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ziio.buddylink.model.domain.Comment;
import com.ziio.buddylink.model.domain.User;
import com.ziio.buddylink.model.vo.CommentVO;
import com.ziio.buddylink.service.CommentService;
import com.ziio.buddylink.mapper.CommentMapper;
import com.ziio.buddylink.service.UserService;
import org.springframework.beans.BeanUtils;
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
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment>
    implements CommentService{

    @Resource
    private UserService userService;

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
}




