package com.ziio.buddylink.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class RabbitMqTest {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void sendMessage(){
        // 队列名称
        String queueName = "simple.queue";
        // 消息
        String message = "hello, spring ziio!";
        // 发送消息
        rabbitTemplate.convertAndSend(queueName, message);
    }
}
