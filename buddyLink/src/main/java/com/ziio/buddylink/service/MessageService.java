package com.ziio.buddylink.service;

import com.ziio.buddylink.model.domain.Message;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ziio.buddylink.model.request.MessageQueryRequest;
import com.ziio.buddylink.model.vo.InteractionMessageVO;
import com.ziio.buddylink.model.vo.MessageVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Ziio
* @description 针对表【message】的数据库操作Service
* @createDate 2024-08-18 21:13:34
*/
public interface MessageService extends IService<Message> {

    /**
     * 添加 收藏信息
     * @param message
     * @return
     */
    public boolean addStarMessage(Message message);

    /**
     * 添加点赞信息
     * @param message
     * @return
     */
    public boolean addLikeMessage(Message message);

    /**
     * 添加关注信息
     * @param message
     * @return
     */
    public boolean addFollowMessage(Message message);

    /**
     * 查找 点赞，收藏，关注信息数量
     * @param request
     * @return
     */
    InteractionMessageVO listInteractionMessage(HttpServletRequest request);

    /**
     * 获得某种类型的 message
     * @param messageQueryRequest
     * @param request
     * @return
     */
    List<MessageVO> listMessages(MessageQueryRequest messageQueryRequest, HttpServletRequest request);
}
