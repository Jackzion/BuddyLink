package com.ziio.buddylink.controller;

import com.ziio.buddylink.common.BaseResponse;
import com.ziio.buddylink.common.ErrorCode;
import com.ziio.buddylink.common.ResultUtils;
import com.ziio.buddylink.exception.BusinessException;
import com.ziio.buddylink.model.domain.Feedback;
import com.ziio.buddylink.model.domain.User;
import com.ziio.buddylink.model.request.CommentAddRequest;
import com.ziio.buddylink.model.request.DeleteRequest;
import com.ziio.buddylink.service.CommentService;
import com.ziio.buddylink.service.FeedbackService;
import com.ziio.buddylink.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/feedback")
@Slf4j
public class FeedbackController {

    @Resource
    private UserService userService;

    @Resource
    private FeedbackService feedbackService;

    @PostMapping("/add")
    public BaseResponse<Long> addFeedback(@RequestBody Feedback feedback, HttpServletRequest request) {
        // 参数效验
        if (feedback == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        feedback.setUserId(loginUser.getId());
        Double rate = feedback.getRate();
        String advice = feedback.getAdvice();
        if (rate == null || rate > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评分不合法");
        }
        if (StringUtils.isBlank(advice) || advice.length() > 500) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "建议不合法");
        }
        // 插入数据库
        boolean save = feedbackService.save(feedback);
        if (!save) {
            log.error("用户：{} 添加反馈失败", loginUser.getId());
        }
        return ResultUtils.success(feedback.getId());
    }

}

