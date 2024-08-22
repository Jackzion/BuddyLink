package com.ziio.buddylink.service;

import com.ziio.buddylink.model.domain.Chat;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ziio.buddylink.model.domain.User;
import com.ziio.buddylink.model.request.ChatRequest;
import com.ziio.buddylink.model.vo.ChatMessageVO;
import com.ziio.buddylink.model.vo.PrivateMessageVO;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

/**
* @author Ziio
* @description 针对表【chat(聊天消息表)】的数据库操作Service
* @createDate 2024-08-21 20:05:10
*/
public interface ChatService extends IService<Chat> {
    /**
     * 获取私人聊天
     *
     * @param chatRequest 聊天请求
     * @param chatType    聊天类型
     * @param loginUser   登录用户
     * @return {@link List}<{@link ChatMessageVO}>
     */
    List<ChatMessageVO> getPrivateChat(ChatRequest chatRequest, int chatType, User loginUser);


    /**
     * 获取缓存
     *
     * @param redisKey redis键
     * @param id       id
     * @return {@link List}<{@link ChatMessageVO}>
     */
    List<ChatMessageVO> getCache(String redisKey, String id);

    /**
     * 聊天记录保存至缓存
     *
     * @param redisKey       redis键
     * @param id             id
     * @param chatMessageVos 聊天消息vos
     */
    void saveCache(String redisKey, String id, List<ChatMessageVO> chatMessageVos);

    /**
     * 聊天结果封装为 ChatMessageVO
     *
     * @param userId     用户id
     * @param toId       到id
     * @param text       文本
     * @param chatType   聊天类型
     * @param createTime 创建时间
     * @return {@link ChatMessageVO}
     */
    ChatMessageVO chatResult(Long userId, Long toId, String text, Integer chatType, Date createTime);

    /**
     * 删除redis缓存
     *
     * @param key 钥匙
     * @param id  id
     */
    void deleteKey(String key, String id);

    /**
     * 获取团队聊天
     *
     * @param chatRequest 聊天请求
     * @param chatType    团队聊天
     * @param loginUser   登录用户
     * @return {@link List}<{@link ChatMessageVO}>
     */
    List<ChatMessageVO> getTeamChat(ChatRequest chatRequest, int chatType, User loginUser);

    /**
     * 获得大厅聊天
     *
     * @param chatType  聊天类型
     * @param loginUser 登录用户
     * @return {@link List}<{@link ChatMessageVO}>
     */
    List<ChatMessageVO> getHallChat(int chatType, User loginUser);

    /**
     * 获取和好友最后一条聊天记录
     * @param request
     * @return
     */
    List<PrivateMessageVO> listPrivateChat(HttpServletRequest request);
}
