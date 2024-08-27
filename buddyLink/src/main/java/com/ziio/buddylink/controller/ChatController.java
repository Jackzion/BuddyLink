package com.ziio.buddylink.controller;

import com.ziio.buddylink.common.BaseResponse;
import com.ziio.buddylink.common.ErrorCode;
import com.ziio.buddylink.common.ResultUtils;
import com.ziio.buddylink.constant.ChatConstant;
import com.ziio.buddylink.exception.BusinessException;
import com.ziio.buddylink.model.domain.User;
import com.ziio.buddylink.model.request.ChatRequest;
import com.ziio.buddylink.model.vo.ChatMessageVO;
import com.ziio.buddylink.model.vo.PrivateMessageVO;
import com.ziio.buddylink.service.ChatService;
import com.ziio.buddylink.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/chat")
@Api(tags = "聊天记录模块")
public class ChatController {

    /**
     * 聊天服务
     */
    @Resource
    private ChatService chatService;

    /**
     * 用户服务
     */
    @Resource
    private UserService userService;

    @PostMapping("/privateChat")
    @ApiOperation(value = "获取私聊")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "chatRequest",
                    value = "聊天请求"),
                    @ApiImplicitParam(name = "request",
                            value = "request请求")})
    public BaseResponse<List<ChatMessageVO>> getPrivateChat(@RequestBody ChatRequest chatRequest,
                                                            HttpServletRequest request) {
        if (chatRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        List<ChatMessageVO> privateChat = chatService.getPrivateChat(chatRequest, ChatConstant.PRIVATE_CHAT, loginUser);
        return ResultUtils.success(privateChat);
    }

    @PostMapping("/teamChat")
    @ApiOperation(value = "获取队伍聊天")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "chatRequest",
                    value = "聊天请求"),
                    @ApiImplicitParam(name = "request",
                            value = "request请求")})
    public BaseResponse<List<ChatMessageVO>> getTeamChat(@RequestBody ChatRequest chatRequest,
                                                         HttpServletRequest request) {
        if (chatRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求有误");
        }
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        List<ChatMessageVO> teamChat = chatService.getTeamChat(chatRequest, ChatConstant.TEAM_CHAT, loginUser);
        return ResultUtils.success(teamChat);
    }

    @GetMapping("/hallChat")
    @ApiOperation(value = "获取大厅聊天")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<List<ChatMessageVO>> getHallChat(HttpServletRequest request) {
//         todo : getLoginUser 效验做成 AOP or 拦截器
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        List<ChatMessageVO> hallChat = chatService.getHallChat(ChatConstant.HALL_CHAT, loginUser);
        return ResultUtils.success(hallChat);
    }

    @GetMapping("/private/list")
    public BaseResponse<List<PrivateMessageVO>> listPrivateChat(HttpServletRequest request) {
        List<PrivateMessageVO> privateMessageVOList = chatService.listPrivateChat(request);
        return ResultUtils.success(privateMessageVOList);
    }
}
