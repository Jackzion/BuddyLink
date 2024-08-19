package com.ziio.buddylink.controller;

import com.ziio.buddylink.common.BaseResponse;
import com.ziio.buddylink.common.ErrorCode;
import com.ziio.buddylink.common.ResultUtils;
import com.ziio.buddylink.exception.BusinessException;
import com.ziio.buddylink.model.request.CommentAddRequest;
import com.ziio.buddylink.model.request.DeleteRequest;
import com.ziio.buddylink.service.CommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/comment")
@Slf4j
public class CommentController {
    @Resource
    private CommentService commentService;

    @PostMapping("/add")
    public BaseResponse<Boolean> addComment(@RequestBody CommentAddRequest commentAddRequest,
                                            HttpServletRequest request) {
        if (commentAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = commentService.addComment(commentAddRequest, request);
        return ResultUtils.success(b);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteComment(@RequestBody DeleteRequest deleteRequest,
                                               HttpServletRequest request) {
        if (deleteRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = commentService.deleteComment(deleteRequest, request);
        return ResultUtils.success(b);
    }

}

