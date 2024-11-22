package com.ziio.buddylink.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
public class SseController {

    // 连接池，k-v ： userID ， SseEmitter
    private Map<Long,SseEmitter> emitters = new ConcurrentHashMap<>();

    // 创建连接 hook ， 提交用户
    @GetMapping("/sse")
    public SseEmitter streamSse(@RequestParam Long UserId) {
        // 查看是否重复连接。。
        if(emitters.containsKey(UserId)){
            return emitters.get(UserId);
        }
        SseEmitter emitter = new SseEmitter();
        emitters.put(UserId,emitter);
        emitter.onCompletion(() -> emitters.remove(UserId));
//        emitter.onTimeout(() -> emitters.remove(UserId));
        return emitter;
    }

    // 推送消息给所有连接的客户端
    public void sendMessageToAllClients(String message) {
        // 记录断线 UserId
        List<Long> deadUser = new ArrayList<>();
        // 遍历 emitter ， 发送消息
        for(Map.Entry<Long,SseEmitter> entry : emitters.entrySet()){
            SseEmitter emitter = entry.getValue();
            Long userId = entry.getKey();
            try {
                // 消息推送
                emitter.send(SseEmitter.event().name("message").data(message));
            } catch (Exception e) {
                // 记录断线连接
                deadUser.add(userId);
            }
        }
        // 移除断线用户
        deadUser.forEach(userId -> emitters.remove(userId));
    }

    // 推送消息给指定用户
    public void sendMessageToClient(String message , Long UserId) {
        SseEmitter emitter = emitters.get(UserId);
        if(emitter != null){
           try{
               emitter.send(SseEmitter.event().name("message").data(message));
           }catch (Exception e) {
               // 移除断线连接
               emitters.remove(UserId);
           }
        }
        else{
            // 不在线，直接不发送
            return;
        }
    }
}
