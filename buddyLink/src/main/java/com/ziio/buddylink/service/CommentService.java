package com.ziio.buddylink.service;

import com.ziio.buddylink.model.domain.Comment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ziio.buddylink.model.request.CommentAddRequest;
import com.ziio.buddylink.model.request.DeleteRequest;
import com.ziio.buddylink.model.vo.CommentVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Ziio
* @description 针对表【comment(评论表)】的数据库操作Service
* @createDate 2024-08-18 19:12:11
*/
public interface CommentService extends IService<Comment> {

    /**
     * 查询评论列表
     * @param blogId 博客ID
     * @param request 请求
     * @return
     */
    List<CommentVO> listComments(Long blogId, HttpServletRequest request);

    /**
     * 添加评论
     * @param commentAddRequest
     * @param request
     * @return
     */
    boolean addComment(CommentAddRequest commentAddRequest, HttpServletRequest request);

    /**
     * 删除评论
     * @param deleteRequest
     * @param request
     * @return
     */
    boolean deleteComment(DeleteRequest deleteRequest, HttpServletRequest request);

    /**
     * 判断是否我的评论
     * @param userId
     * @param commentId
     * @return
     */
    boolean isMyComment(long userId, long commentId);
}
