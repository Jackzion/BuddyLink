package com.ziio.buddylink.publisher;

import cn.hutool.json.JSONUtil;
import com.ziio.buddylink.constant.MqMessageType;
import com.ziio.buddylink.model.domain.MqMessage;
import com.ziio.buddylink.model.domain.User;
import com.ziio.buddylink.service.UserService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class MessagePublisher {

    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private UserService userService;

    // todo : 队列按 topic 分开？ 没有区分各个用户的消息，且没有设置消息存活时间 -- 这样用户不上线应该是看不到消息的
    public void sendStarMessage(Long fromId , Long tuId){
        // 队列名称
        String queueName = "simple.queue";
        User fromUser = userService.getById(fromId);
        // 构造 mq 消息 (json)
        MqMessage mqMessage = new MqMessage();
        String message = "Your Blogs is Started by " + fromUser.getUsername();
        mqMessage.setType(MqMessageType.MESSAGE_STAR);
        mqMessage.setMessage(message);
        mqMessage.setToUserId(tuId);
        String jsonStr = JSONUtil.toJsonStr(mqMessage);
        // 发送消息
        rabbitTemplate.convertAndSend(queueName, jsonStr);
    }

    public void sendLikeMessage(Long fromId , Long tuId){
        // 队列名称
        String queueName = "simple.queue";
        User fromUser = userService.getById(fromId);
        // 构造 mq 消息 (json
        MqMessage mqMessage = new MqMessage();
        String message = "Your Blogs is Liked by " + fromUser.getUsername();
        mqMessage.setType(MqMessageType.MESSAGE_LIKE);
        mqMessage.setMessage(message);
        mqMessage.setToUserId(tuId);
        String jsonStr = JSONUtil.toJsonStr(mqMessage);
        // 发送消息
        rabbitTemplate.convertAndSend(queueName, jsonStr);
    }

    public void sendChatMessage(Long fromId  , String chatMessage , Long tuId){
        // 队列名称
        String queueName = "simple.queue";
        User fromUser = userService.getById(fromId);
        // 构造 mq Message
        MqMessage mqMessage = new MqMessage();
        String message = fromUser.getUsername() + ":  " + chatMessage;
        mqMessage.setType(MqMessageType.MESSAGE_LIKE);
        mqMessage.setMessage(message);
        mqMessage.setToUserId(tuId);
        String jsonStr = JSONUtil.toJsonStr(mqMessage);
        // 发送消息
        rabbitTemplate.convertAndSend(queueName, jsonStr);
    }

}
