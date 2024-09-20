package com.ziio.buddylink.publisher;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class MessagePublisher {

    @Resource
    private RabbitTemplate rabbitTemplate;

    // todo : 队列按逻辑分开？ 消息过多可能撑不住
    public void sendStarMessage(){
        // 队列名称
        String queueName = "simple.queue";
        // 消息
        String message = "Your Blogs is Started by ...";
        // 发送消息
        rabbitTemplate.convertAndSend(queueName, message);
    }

    public void sendLikeMessage(){
        // 队列名称
        String queueName = "simple.queue";
        // 消息
        String message = "Your Blogs is Liked by ...";
        // 发送消息
        rabbitTemplate.convertAndSend(queueName, message);
    }

    public void sendChatMessage(){
        // 队列名称
        String queueName = "simple.queue";
        // 消息
        String message = "Someone is talking to you ...";
        // 发送消息
        rabbitTemplate.convertAndSend(queueName, message);
    }

}
