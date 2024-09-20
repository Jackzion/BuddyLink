package com.ziio.buddylink.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
public class SseController {

    private List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    // todo : SSE 推送 + rabbit mq 实现监听
    @GetMapping("/sse")
    public SseEmitter handleSse() {
        // 设置超时时间 (30分钟)
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                // 发送消息给客户端
                emitter.send("Hello, the time is " + System.currentTimeMillis());
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }, 0, 5, TimeUnit.SECONDS); // 每 5 秒发送一次消息

        return emitter;
    }

    // 创建连接 hook ， 提交用户
    @GetMapping("/sss")
    public SseEmitter streamSse() {
        SseEmitter emitter = new SseEmitter();
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));

        return emitter;
    }

    // 推送消息给所有连接的客户端
    public void sendMessageToClients(String message) {
        List<SseEmitter> deadEmitters = new ArrayList<>();

        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("message").data(message));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        });
        // 移除失效的连接
        emitters.removeAll(deadEmitters);
    }
}
