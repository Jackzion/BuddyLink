package com.ziio.buddylink.model.domain;

import lombok.Data;

/**
 * mq 消息格式
 */
@Data
public class MqMessage {

    // 消息本身
    private String message;

    // 消息类型 ： 1 - 点赞 ， 2 - 收藏 ， 3 - 聊天
    private Integer type;

}
