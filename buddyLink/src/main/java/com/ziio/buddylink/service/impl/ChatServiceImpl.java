package com.ziio.buddylink.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ziio.buddylink.common.ErrorCode;
import com.ziio.buddylink.exception.BusinessException;
import com.ziio.buddylink.model.domain.Chat;
import com.ziio.buddylink.model.domain.Friend;
import com.ziio.buddylink.model.domain.Team;
import com.ziio.buddylink.model.domain.User;
import com.ziio.buddylink.model.request.ChatRequest;
import com.ziio.buddylink.model.vo.ChatMessageVO;
import com.ziio.buddylink.model.vo.PrivateMessageVO;
import com.ziio.buddylink.model.vo.WebSocketVO;
import com.ziio.buddylink.service.*;
import com.ziio.buddylink.mapper.ChatMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Var;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ziio.buddylink.constant.ChatConstant.*;
import static com.ziio.buddylink.constant.RedisConstant.*;
import static com.ziio.buddylink.constant.UserConstant.ADMIN_ROLE;

/**
* @author Ziio
* @description 针对表【chat(聊天消息表)】的数据库操作Service实现
* @createDate 2024-08-21 20:05:10
*/
@Service
@Slf4j
public class ChatServiceImpl extends ServiceImpl<ChatMapper, Chat>
    implements ChatService{

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Resource
    private FriendService friendService;

    @Resource
    private UserTeamService userTeamService;

    @Override
    public List<ChatMessageVO> getPrivateChat(ChatRequest chatRequest, int chatType, User loginUser) {
        // 提取效验参数
        Long toId = chatRequest.getToId();
        if (toId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 从 redis 中获取缓存
        List<ChatMessageVO> chatRecords = getCache(CACHE_CHAT_PRIVATE, loginUser.getId() + String.valueOf(toId));
        if (chatRecords != null) {
            saveCache(CACHE_CHAT_PRIVATE, loginUser.getId() + String.valueOf(toId), chatRecords);
            return chatRecords;
        }
        // 缓存没数据 ， 从数据库获取
        LambdaQueryWrapper<Chat> chatLambdaQueryWrapper = new LambdaQueryWrapper<>();
        chatLambdaQueryWrapper.and(chatquery -> chatquery
                .eq(Chat::getFromId , loginUser.getId())
                .eq(Chat::getToId, toId)
                .or()
                .eq(Chat::getFromId , toId)
                .eq(Chat::getToId, loginUser.getId())
        ).eq(Chat::getChatType, chatType);
        List<Chat> chatList = this.list(chatLambdaQueryWrapper);
        // 转换为 VO
        List<ChatMessageVO> chatMessageVOList = chatList.stream().map(chat -> {
            ChatMessageVO chatMessageVO = chatResult(loginUser.getId(),
                    toId, chat.getText(), chatType,
                    chat.getCreateTime());
            if (chat.getFromId().equals(loginUser.getId())) {
                chatMessageVO.setIsMy(true);
            }
            return chatMessageVO;
        }).collect(Collectors.toList());
        // 保存到 redis 中
        saveCache(CACHE_CHAT_PRIVATE, loginUser.getId() + String.valueOf(toId), chatMessageVOList);
        return chatMessageVOList;
    }

    @Override
    public List<ChatMessageVO> getCache(String redisKey, String id) {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        List<ChatMessageVO> chatRecords;
        if(redisKey.equals(CACHE_CHAT_HALL)) {
            // 获取 大厅聊天数据 ， 不需要 key
            chatRecords = (List<ChatMessageVO>)valueOperations.get(redisKey);
        }else{
            // 获取 private , team 数据 ， 需要拼接 id
            chatRecords = (List<ChatMessageVO>)valueOperations.get(redisKey + id);
        }
        return chatRecords;
    }

    @Override
    public void saveCache(String redisKey, String id, List<ChatMessageVO> chatMessageVos) {
        try{
            ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
            // 注 ： 默认对象存入redis string 结构会以 json str 存储 ， 提取 也会进行自动封装 object default
            // 设置随机过期时间 ， 解决缓存雪崩问题
            int i = RandomUtil.randomInt(MINIMUM_CACHE_RANDOM_TIME, MAXIMUM_CACHE_RANDOM_TIME);
            if(redisKey.equals(CACHE_CHAT_HALL)) {
                // 获取 大厅聊天数据 ， 不需要 key
                valueOperations.set(redisKey, chatMessageVos , MINIMUM_CACHE_RANDOM_TIME + i / CACHE_TIME_OFFSET , TimeUnit.SECONDS);
            }else{
                // 获取 private , team 数据 ， 需要拼接 id
                valueOperations.set(redisKey + id, chatMessageVos , MINIMUM_CACHE_RANDOM_TIME + i / CACHE_TIME_OFFSET , TimeUnit.SECONDS);
            }
        }catch (Exception e){
            log.error("redis save cache error : {}", e.getMessage());
        }
    }

    @Override
    public ChatMessageVO chatResult(Long userId, Long toId, String text, Integer chatType, Date createTime) {
        ChatMessageVO chatMessageVo = new ChatMessageVO();
        User fromUser = userService.getById(userId);
        User toUser = userService.getById(toId);
        WebSocketVO fromWebSocketVo = new WebSocketVO();
        WebSocketVO toWebSocketVo = new WebSocketVO();
        BeanUtils.copyProperties(fromUser, fromWebSocketVo);
        BeanUtils.copyProperties(toUser, toWebSocketVo);
        chatMessageVo.setFromUser(fromWebSocketVo);
        chatMessageVo.setToUser(toWebSocketVo);
        chatMessageVo.setChatType(chatType);
        chatMessageVo.setText(text);
        chatMessageVo.setCreateTime(DateUtil.format(createTime, "yyyy-MM-dd HH:mm:ss"));
        return chatMessageVo;
    }
    private ChatMessageVO chatResult(Long userId, String text) {
        ChatMessageVO chatMessageVo = new ChatMessageVO();
        User fromUser = userService.getById(userId);
        WebSocketVO fromWebSocketVo = new WebSocketVO();
        BeanUtils.copyProperties(fromUser, fromWebSocketVo);
        chatMessageVo.setFromUser(fromWebSocketVo);
        chatMessageVo.setText(text);
        return chatMessageVo;
    }

    @Override
    public void deleteKey(String key, String id) {
        // 删除缓存
        if (key.equals(CACHE_CHAT_HALL)) {
            redisTemplate.delete(key);
        } else {
            redisTemplate.delete(key + id);
        }
    }

    @Override
    public List<ChatMessageVO> getTeamChat(ChatRequest chatRequest, int chatType, User loginUser) {
        // 提取效验参数
        Long teamId = chatRequest.getTeamId();
        if(teamId == null){
            throw  new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数有误");
        }
        // 判断用户是否在队伍中
        if (!userTeamService.teamHasUser(teamId, loginUser.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您还未加入此队伍");
        }
        // 从 redis 缓存获取
        List<ChatMessageVO> chatRecords = getCache(CACHE_CHAT_TEAM, String.valueOf(teamId));
        if(chatRecords != null){
            // 每次获取 ，添加 isMyMessage 标识 ，因为是公共聊天记录 -team ， hall 。。。, 每次获取 ， isMine会改变
            List<ChatMessageVO> chatMessageVOS = checkIsMyMessage(loginUser, chatRecords);
            // 多存一遍？保证 isMy 实时性
            saveCache(CACHE_CHAT_TEAM, String.valueOf(teamId), chatMessageVOS);
            return chatMessageVOS;
        }
        // redis 缓存没有 ， 从数据库获取
        Team team = teamService.getById(teamId);
        LambdaQueryWrapper<Chat> chatLambdaQueryWrapper = new LambdaQueryWrapper<>();
        chatLambdaQueryWrapper.eq(Chat::getChatType, chatType).eq(Chat::getTeamId, teamId);
        // 查询 ， vo and 标识处理
        List<ChatMessageVO> chatMessageVOS = returnMessage(loginUser, team.getUserId(), chatLambdaQueryWrapper);
        // 保存到 redis 缓存
        saveCache(CACHE_CHAT_TEAM, String.valueOf(teamId), chatMessageVOS);
        return chatMessageVOS;
    }

    /**
     * 数据库查询对应 type 的聊天记录
     * @param loginUser              登录用户
     * @param userId                 用户id
     * @param chatLambdaQueryWrapper 聊天lambda查询包装器
     * @return
     */
    private List<ChatMessageVO> returnMessage(User loginUser, Long userId, LambdaQueryWrapper<Chat> chatLambdaQueryWrapper) {
        // 按包装查询请求
        List<Chat> chatList = this.list(chatLambdaQueryWrapper);
        // 转换为 VO
        return chatList.stream().map(chat -> {
            // 封装 VO
            ChatMessageVO chatMessageVo = chatResult(chat.getFromId(), chat.getText());
            // 添加 管理员 or 群主 标识
            boolean isCaptain = userId != null && userId.equals(chat.getFromId());
            if (userService.getById(chat.getFromId()).getUserRole() == ADMIN_ROLE || isCaptain) {
                chatMessageVo.setIsAdmin(true);
            }
            // 添加 isMyMessage 标识
            if (chat.getFromId().equals(loginUser.getId())) {
                chatMessageVo.setIsMy(true);
            }
            chatMessageVo.setCreateTime(DateUtil.format(chat.getCreateTime(), "yyyy年MM月dd日 HH:mm:ss"));
            return chatMessageVo;
        }).collect(Collectors.toList());
    }

    /**
     * 添加 isMyMessage 标识
     * @param loginUser
     * @param chatRecords
     * @return
     */
    private List<ChatMessageVO> checkIsMyMessage(User loginUser, List<ChatMessageVO> chatRecords) {
        return chatRecords.stream().map(chat -> {
            // 更新原有 isMyMessage 标识
            if (!chat.getFromUser().getId().equals(loginUser.getId()) && chat.getIsMy()) {
                chat.setIsMy(false);
            }
            if (chat.getFromUser().getId().equals(loginUser.getId()) && !chat.getIsMy()) {
                chat.setIsMy(true);
            }
            return chat;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ChatMessageVO> getHallChat(int chatType, User loginUser) {
        // 从 redis 缓存获取
        List<ChatMessageVO> chatRecords = getCache(CACHE_CHAT_HALL, String.valueOf(loginUser.getId()));
        if (chatRecords != null) {
            // 重新处理 isMyMessage 标识
            List<ChatMessageVO> chatMessageVOS = checkIsMyMessage(loginUser, chatRecords);
            saveCache(CACHE_CHAT_HALL, String.valueOf(loginUser.getId()), chatMessageVOS);
            return chatMessageVOS;
        }
        // redis 缓存没有 ， 从数据库获取
        LambdaQueryWrapper<Chat> chatLambdaQueryWrapper = new LambdaQueryWrapper<>();
        chatLambdaQueryWrapper.eq(Chat::getChatType, chatType);
        // 查询 ， vo and 标识处理
        List<ChatMessageVO> chatMessageVOS = returnMessage(loginUser, null, chatLambdaQueryWrapper);
        // 保存到 redis 缓存
        saveCache(CACHE_CHAT_HALL, String.valueOf(loginUser.getId()), chatMessageVOS);
        return chatMessageVOS;
    }

    @Override
    public List<PrivateMessageVO> listPrivateChat(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        long userId = loginUser.getId();
        // 获取好友列表
        List<Long> frinedIdList = friendService.list(new QueryWrapper<Friend>().eq("userId", userId))
                .stream().map(Friend::getFriendId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(frinedIdList)) {
            return new ArrayList<>();
        }
        // 获取和好友最后一条聊天记录
        List<Chat> chatList = this.baseMapper.getLastPrivateChatMessages(userId, frinedIdList);
        // 转换为 VO
        return chatList.stream().map(chat -> {
            PrivateMessageVO privateMessageVO = new PrivateMessageVO();
            privateMessageVO.setId(chat.getId());
            privateMessageVO.setText(chat.getText());
            privateMessageVO.setCreateTime(DateUtil.format(chat.getCreateTime(), "yyyy年MM月dd日 HH:mm:ss"));
            Long fromId = chat.getFromId();
            Long toId = chat.getToId();
            long friendId = 0L;
            if (fromId != userId) {
                friendId = fromId;
            }
            if (toId != userId) {
                friendId = toId;
            }
            privateMessageVO.setFriendId(friendId);
            User friend = userService.getById(friendId);
            privateMessageVO.setAvatarUrl(friend.getAvatarUrl());
            privateMessageVO.setUsername(friend.getUsername());
            return privateMessageVO;
        }).collect(Collectors.toList());
    }
}




