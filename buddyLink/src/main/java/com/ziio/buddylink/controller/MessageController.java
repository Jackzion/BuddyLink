package com.ziio.buddylink.controller;

import com.ziio.buddylink.common.BaseResponse;
import com.ziio.buddylink.common.ErrorCode;
import com.ziio.buddylink.common.ResultUtils;
import com.ziio.buddylink.exception.BusinessException;
import com.ziio.buddylink.model.request.DeleteRequest;
import com.ziio.buddylink.model.request.MessageQueryRequest;
import com.ziio.buddylink.model.vo.InteractionMessageVO;
import com.ziio.buddylink.model.vo.MessageVO;
import com.ziio.buddylink.service.MessageService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/message")
public class MessageController {
    @Resource
    private MessageService messageService;

    @GetMapping("/interaction/list")
    public BaseResponse<InteractionMessageVO> listInteractionMessage(HttpServletRequest request) {
        InteractionMessageVO interactionMessageVO = messageService.listInteractionMessage(request);
        return ResultUtils.success(interactionMessageVO);
    }

    @PostMapping("/list")
    public BaseResponse<List<MessageVO>> listMessages(@RequestBody MessageQueryRequest messageQueryRequest, HttpServletRequest request) {
        if (messageQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<MessageVO> messageVOList = messageService.listMessages(messageQueryRequest, request);
        return ResultUtils.success(messageVOList);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteMessage(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = messageService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }
}
