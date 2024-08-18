package com.ziio.buddylink.service;

import com.ziio.buddylink.model.domain.Comment;
import com.baomidou.mybatisplus.extension.service.IService;
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
}
