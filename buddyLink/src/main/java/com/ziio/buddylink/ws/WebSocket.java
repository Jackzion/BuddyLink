package com.ziio.buddylink.ws;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import com.ziio.buddylink.config.HttpSessionConfig;
import com.ziio.buddylink.model.domain.Chat;
import com.ziio.buddylink.model.domain.Team;
import com.ziio.buddylink.model.domain.User;
import com.ziio.buddylink.model.request.MessageRequest;
import com.ziio.buddylink.model.vo.ChatMessageVO;
import com.ziio.buddylink.model.vo.WebSocketVO;
import com.ziio.buddylink.publisher.MessagePublisher;
import com.ziio.buddylink.service.ChatService;
import com.ziio.buddylink.service.TeamService;
import com.ziio.buddylink.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.ziio.buddylink.constant.ChatConstant.*;
import static com.ziio.buddylink.constant.UserConstant.ADMIN_ROLE;
import static com.ziio.buddylink.constant.UserConstant.USER_LOGIN_STATE;

/**
 * WebSocket服务
 */
@Component
@Slf4j
@ServerEndpoint(value = "/websocket/{userId}/{teamId}" , configurator = HttpSessionConfig.class)
public class WebSocket {

    /**
     * 保证队伍连接信息 , teamId -> userid , webSocket
     */
    private static Map<String, ConcurrentMap<String,WebSocket>> Rooms = new HashMap<>();

    /**
     * 当前信息
     */
    private Session session;

    /**
     * http会话
     */
    private HttpSession httpSession;


    /**
     * 线程安全集合，保存 Session
     */
    private static final CopyOnWriteArraySet<Session> SESSIONS = new CopyOnWriteArraySet<>();

    /**
     * 会话池 , 缓存了 k-v <userId , session>
     * <userId , session>
     */
    private static final Map<String, Session> SESSION_POOL = new HashMap<>();

    /**
     * 用户服务
     */
    private static UserService userService;
    /**
     * 聊天服务
     */
    private static ChatService chatService;
    /**
     * 团队服务
     */
    private static TeamService teamService;
    /**
     * 消息队列服务
     */
    private static MessagePublisher messagePublisher;

    /**
     * 房间在线人数
     */
    private static int onlineCount = 0;

    /**
     * 上网数
     *
     * @return int , todo : online Count 是否合理 ， 是否需要优化
     */
    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    /**
     * 在线计数++
     */
    public static synchronized void addOnlineCount() {
        WebSocket.onlineCount++;
    }

    /**
     * 在线计数--
     */
    public static synchronized void subOnlineCount() {
        WebSocket.onlineCount--;
    }

    /**
     * 静态类注入 ，websocket 实例不由 spring 注入
     *
     * @param userService 用户服务
     */
    @Resource
    public void setHeatMapService(UserService userService) {
        WebSocket.userService = userService;
    }

    /**
     * 静态类注入 ，websocket 实例不由 spring 注入
     *
     * @param chatService 聊天服务
     */
    @Resource
    public void setHeatMapService(ChatService chatService) {
        WebSocket.chatService = chatService;
    }

    /**
     * 静态类注入 ，websocket 实例不由 spring 注入
     *
     * @param teamService 团队服务
     */
    @Resource
    public void setHeatMapService(TeamService teamService) {
        WebSocket.teamService = teamService;
    }

    /**
     * 静态类注入 ，websocket 实例不由 spring 注入
     *
     * @param messagePublisher 消息队列发布
     */
    @Resource
    public void setMessagePublisher(MessagePublisher messagePublisher) {
        WebSocket.messagePublisher = messagePublisher;
    }

    /**
     * 队伍群内转发消息
     * @param teamId
     * @param message
     */
    public static void broadcast(String teamId,String message) {
        ConcurrentMap<String, WebSocket> teamRoom = Rooms.get(teamId);
        // keySet 获取所有key ， 遍历 key ，获取每一个 webSocket ， 进行 send Message
        for (String key : teamRoom.keySet()) {
            try{
                WebSocket webSocket = teamRoom.get(key);
                webSocket.sendMessage(message);
            }catch (Exception e){
                log.error("消息发送失败",e);
            }
        }
    }

    /**
     * 发送消息
     * @param message
     */
    private void sendMessage(String message) throws IOException {
        User loginUser =  (User) this.httpSession.getAttribute(USER_LOGIN_STATE);
        this.session.getBasicRemote().sendText(message);
        // 添加消息到消息队列
        // todo: 这里先默认，team 由于难得到 this.session
        messagePublisher.sendChatMessage(5L,message);
    }

    /**
     * 对话连接时 ， 钩子函数
     * @param session
     * @param userId
     * @param teamId
     * @param config
     */
    @OnOpen
    public void onOpen(Session session ,
                       @PathParam(value = "userId") String userId ,
                       @PathParam(value = "teamId") String teamId ,
                       EndpointConfig config) {
        log.info("onOpen called with userId: {}, teamId: {}", userId, teamId);
        try{
            // send error message to user
            if (StringUtils.isBlank(userId) || "undefined".equals(userId)) {
                sendError(userId, "参数有误");
                return;
            }
            log.info("Retrieving HttpSession from EndpointConfig...");
            // 获取对方 Httpsession
            HttpSession userHttpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
            log.info("HttpSession: {}", userHttpSession);
            if (userHttpSession == null) {
                log.error("HttpSession is null");
                return;
            }
            User user = (User) userHttpSession.getAttribute(USER_LOGIN_STATE);
            if (user != null) {
                this.session = session;
                this.httpSession = userHttpSession;
            }

            if(!"NaN".equals(teamId)){
                if(!Rooms.containsKey(teamId)){
                    // init team room
                    // todo : ConcurrentMap<String , WebSocket> change to <string , session>
                    ConcurrentMap<String , WebSocket> room = new ConcurrentHashMap<>(0);
                    room.put(userId,this);
                    Rooms.put(teamId,room);
                    addOnlineCount();
                }else if(!Rooms.get(teamId).containsKey(userId)){
                    // add user to team room
                    Rooms.get(teamId).put(userId,this);
                    addOnlineCount();
                }
            }else{
                // 个人连接 ， 将更新用户名单 session ， session poll ， 并将新用户名单 send all user
                SESSIONS.add(session);
                SESSION_POOL.put(userId, session);
                this.sendAllUsers();
            }
        }catch(Exception e){
            log.error("exception message",e);
        }
    }

    /**
     * 对话取消时 ， 钩子函数
     * @param userId
     * @param teamId
     * @param session
     */
    @OnClose
    public void onClose(@PathParam(value = "userId") String userId , @PathParam(value = "teamId") String teamId , Session session) {
        try{
            // 判断队伍是否有效
            if(!"NaN".equals(teamId)){
                // 移除 team Room 中的 user
                Rooms.get(teamId).remove(userId);
                if (getOnlineCount() > 0) {
                    subOnlineCount();
                }
            }else{
                // 个人连接 ， 将更新用户名单 session ， session poll ， 并将新用户名单 send all user
                if(!SESSION_POOL.isEmpty()){
                    SESSIONS.remove(session);
                    SESSION_POOL.remove(userId);
                }
                this.sendAllUsers();
            }
        }catch(Exception e){
            log.error("exception message",e);
        }
    }

    @OnMessage
    public void onMessage(String message, @PathParam("userId") String userId) {
        // ping 消息反馈
        if ("PING".equals(message)) {
            sendOneMessage(userId, "pong");
            return;
        }

        // 对 message(json) 拆封
        MessageRequest messageRequest = new Gson().fromJson(message, MessageRequest.class);
        Long toId = messageRequest.getToId();
        Long teamId = messageRequest.getTeamId();
        String text = messageRequest.getText();
        Integer chatType = messageRequest.getChatType();
        User fromUser = userService.getById(userId);
        Team team = teamService.getById(teamId);
        // 根据 type ， 发送消息
        if (chatType == PRIVATE_CHAT) {
            // 私聊
            privateChat(fromUser, toId, text, chatType);
        } else if (chatType == TEAM_CHAT) {
            // 队伍内聊天
            teamChat(fromUser, text, team, chatType);
        } else {
            // 群聊
            hallChat(fromUser, text, chatType);

        }
    }

    /**
     * 队伍聊天
     * @param fromUser
     * @param text
     * @param team
     * @param chatType
     */
    private void teamChat(User fromUser, String text, Team team, Integer chatType) {
        // 封装 chatMessageVO
        ChatMessageVO chatMessageVo = new ChatMessageVO();
        WebSocketVO fromWebSocketVO = new WebSocketVO();
        BeanUtils.copyProperties(fromUser, fromWebSocketVO);
        chatMessageVo.setFromUser(fromWebSocketVO);
        chatMessageVo.setText(text);
        chatMessageVo.setTeamId(team.getId());
        chatMessageVo.setChatType(chatType);
        chatMessageVo.setCreateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        // 添加 Admin ， mine 标识
        if(fromUser.getUserRole() == ADMIN_ROLE){
            chatMessageVo.setIsAdmin(true);
        }
        User loginUser = (User) this.httpSession.getAttribute(USER_LOGIN_STATE);
        if(Objects.equals(loginUser.getId(), fromUser.getId())){
            chatMessageVo.setIsMy(true);
        }
        String json = new Gson().toJson(chatMessageVo);
        try{
            // 发送消息
            broadcast(String.valueOf(team.getId()), json);
            // 保存数据库 ，删除 redis 缓存 ，保证数据一致性
            saveChat(fromUser.getId(), null, text, team.getId(), chatType);
            chatService.deleteKey(CACHE_CHAT_TEAM, String.valueOf(team.getId()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 大厅聊天
     * @param fromUser
     * @param text
     * @param chatType
     */
    private void hallChat(User fromUser, String text, Integer chatType) {
        // 封装 chatMessageVO
        ChatMessageVO chatMessageVo = new ChatMessageVO();
        WebSocketVO fromWebSocketVO = new WebSocketVO();
        BeanUtils.copyProperties(fromUser, fromWebSocketVO);
        chatMessageVo.setFromUser(fromWebSocketVO);
        chatMessageVo.setText(text);
        chatMessageVo.setChatType(chatType);
        chatMessageVo.setCreateTime(DateUtil.format(new Date(), "yyyy年MM月dd日 HH:mm:ss"));
        if (fromUser.getUserRole() == ADMIN_ROLE) {
            chatMessageVo.setIsAdmin(true);
        }
        User loginUser = (User) this.httpSession.getAttribute(USER_LOGIN_STATE);
        if (Objects.equals(loginUser.getId(), fromUser.getId())) {
            chatMessageVo.setIsMy(true);
        }
        // 发送消息
        String json = new Gson().toJson(chatMessageVo);
        sendAllMessage(json);
        // 保存数据库 ，删除 redis 缓存 ，保证数据一致性
        saveChat(fromUser.getId(), null, text, null, chatType);
        chatService.deleteKey(CACHE_CHAT_HALL, String.valueOf(fromUser.getId()));
    }

    /**
     * 私人聊天
     * @param fromUser
     * @param toId
     * @param text
     * @param chatType
     */
    private void privateChat(User fromUser, Long toId, String text, Integer chatType) {
        // 封装 chatMessageVO
        ChatMessageVO chatMessageVO = chatService.chatResult(fromUser.getId(), toId, text, chatType, DateUtil.date(System.currentTimeMillis()));
        User loginUser = (User) this.httpSession.getAttribute(USER_LOGIN_STATE);
        if (Objects.equals(loginUser.getId(), fromUser.getId())) {
            chatMessageVO.setIsMy(true);
        }
        // 发送消息
        String toJson = new Gson().toJson(chatMessageVO);
        sendOneMessage(toId.toString(), toJson);
        // 更新数据库，同时删除缓存 --- 保证一致性
        saveChat(fromUser.getId(), toId, text, null, chatType);
        chatService.deleteKey(CACHE_CHAT_PRIVATE, fromUser.getId() + "" + toId);
        chatService.deleteKey(CACHE_CHAT_PRIVATE, toId + "" + fromUser.getId());
    }

    /**
     * 保存聊天
     *
     * @param userId   用户id
     * @param toId     为id
     * @param text     文本
     * @param teamId   团队id
     * @param chatType 聊天类型
     */
    private void saveChat(Long userId, Long toId, String text, Long teamId, Integer chatType) {
//        if (chatType == PRIVATE_CHAT) {
//            User user = userService.getById(userId);
//            Set<Long> userIds = stringJsonListToLongSet(user.getFriendIds());
//            if (!userIds.contains(toId)) {
//                sendError(String.valueOf(userId), "该用户不是你的好友");
//                return;
//            }
//        }
        Chat chat = new Chat();
        chat.setFromId(userId);
        chat.setText(String.valueOf(text));
        chat.setChatType(chatType);
        chat.setCreateTime(new Date());
        if (toId != null && toId > 0) {
            chat.setToId(toId);
        }
        if (teamId != null && teamId > 0) {
            chat.setTeamId(teamId);
        }
        chatService.save(chat);
    }

    /**
     * 給 在线所有用户发送新的用户名单 ， 更新用户名单
     */
    private void sendAllUsers() {
        HashMap<String, List<WebSocketVO>> stringListHashMap = new HashMap<>(0);
        List<WebSocketVO> webSocketVos = new ArrayList<>();
        stringListHashMap.put("users", webSocketVos);
        // 向当前活跃用户 ， webSocketVos 发送消息
        for (String key : SESSION_POOL.keySet()) {
            // 根据 user ， 封装好 webSocketVo
            User user = userService.getById(key);
            WebSocketVO webSocketVO = new WebSocketVO();
            BeanUtils.copyProperties(user, webSocketVO);
            webSocketVos.add(webSocketVO);
        }
        // 这里将用户名单 ， 发送出去 , 就是将更新过后的用户名单发给 all user
        // todo : 上线检测
        sendAllMessage(JSONUtil.toJsonStr(stringListHashMap));
    }

    /**
     * 广播消息
     * @param message 要广播的消息
     */
    public void sendAllMessage(String message) {
        User fromUser = (User)httpSession.getAttribute(USER_LOGIN_STATE);
        // 获取所有 活跃的 session
        for (Session session : SESSIONS) {
            try {
                if (session.isOpen()) { // 查看是否活跃
                    // 加锁，防掉连接
                    synchronized (session) {
                        session.getBasicRemote().sendText(message);
                        // 添加消息到消息队列
                        messagePublisher.sendChatMessage(fromUser.getId(),message);
                    }
                }
            } catch (Exception e) {
                log.error("exception message", e);
            }
        }
    }

    /**
     * 发送失败 , feedBack 给用户
     *
     * @param userId       用户id
     * @param errorMessage 错误消息
     */
    private void sendError(String userId, String errorMessage) {
        JSONObject obj = new JSONObject();
        obj.set("error", errorMessage);
        sendOneMessage(userId, obj.toString());
    }

    private void sendOneMessage(String userId, String message) {
        // 获取 receiver session
        Session userSession = SESSION_POOL.get(userId);
        User fromUser =(User) httpSession.getAttribute(USER_LOGIN_STATE);
        if(userSession != null && userSession.isOpen()){
            try{
                // 对 userSession 接受方设置锁 , 避免另一线程尝试关闭连接， 到时候 ， 客户端 session id remove ， 对方就接收不了消息了
                synchronized (userSession){
                    userSession.getBasicRemote().sendText(message);
                }
            }catch (Exception e){
                log.error("exception message", e);
            }
        }
        // 添加消息到消息队列
        messagePublisher.sendChatMessage(fromUser.getId(),message);
    }
}
