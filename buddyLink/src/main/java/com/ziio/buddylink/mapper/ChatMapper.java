package com.ziio.buddylink.mapper;

import com.ziio.buddylink.model.domain.Chat;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
* @author Ziio
* @description 针对表【chat(聊天消息表)】的数据库操作Mapper
* @createDate 2024-08-21 20:05:10
* @Entity com.ziio.buddylink.model.domain.Chat
*/
public interface ChatMapper extends BaseMapper<Chat> {

    /**
     * 获取和好友最后一条的聊天消息, todo : 未能解决 fromUserId 最新为自己的情况
     * @param userId
     * @param frinedIdList
     * @return
     */
    List<Chat> getLastPrivateChatMessages(long userId, List<Long> frinedIdList);
}




