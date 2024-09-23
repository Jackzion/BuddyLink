package com.ziio.buddylink.listener;

import com.google.gson.Gson;
import com.ziio.buddylink.controller.SseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 监听 rabbit mq 信息，进行 SSE 消息推送给前端
 */
@Slf4j
@Component
public class SpringRabbitListener {

    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private SseController sseController;

    @RabbitListener(queues = "simple.queue")
    public void listenSimpleQueueMessage(String msg) throws InterruptedException {
        // 向前端推送消息
        log.info("ziio 已经收到消息!!");
        sseController.sendMessageToClients(msg);
    }
}
