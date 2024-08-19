package com.ziio.buddylink.service;

import com.ziio.buddylink.model.domain.Message;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Ziio
* @description 针对表【message】的数据库操作Service
* @createDate 2024-08-18 21:13:34
*/
public interface MessageService extends IService<Message> {

    public boolean addStarMessage(Message message);

    public boolean addLikeMessage(Message message);

    public boolean addFollowMessage(Message message);
}
