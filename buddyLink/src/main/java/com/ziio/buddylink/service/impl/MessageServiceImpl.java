package com.ziio.buddylink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ziio.buddylink.common.ErrorCode;
import com.ziio.buddylink.exception.BusinessException;
import com.ziio.buddylink.model.domain.Blog;
import com.ziio.buddylink.model.domain.Message;
import com.ziio.buddylink.model.domain.User;
import com.ziio.buddylink.model.request.MessageQueryRequest;
import com.ziio.buddylink.model.vo.InteractionMessageVO;
import com.ziio.buddylink.model.vo.MessageVO;
import com.ziio.buddylink.service.BlogService;
import com.ziio.buddylink.service.MessageService;
import com.ziio.buddylink.mapper.MessageMapper;
import com.ziio.buddylink.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author Ziio
* @description 针对表【message】的数据库操作Service实现
* @createDate 2024-08-18 21:13:34
*/
@Service
@Slf4j
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message>
    implements MessageService{

    @Resource
    private UserService userService;

    @Resource
    @Lazy // 懒加载解决循环依赖，生成代理对象，只有在调用方法时候才创建实例
    private BlogService blogService;

    @Override
    public boolean addStarMessage(Message message) {
        // 获取参数
        Long fromId = message.getFromId();
        Long toId = message.getToId();
        String text = message.getText();
        Long blogId = message.getBlogId();
        User user = userService.getById(fromId);
        Blog blog = blogService.getById(blogId);
        // 检查信息是否已经存在，且未读
        Long count = this.lambdaQuery().eq(Message::getToId, toId).eq(Message::getFromId, fromId)
                .eq(Message::getBlogId, blogId).eq(Message::getIsRead, 0).eq(Message::getType, 0).count();
        boolean save = false;
        // 不存在，则保存至数据库
        if(count!=null && count<1){
            message.setAvatarUrl(user.getAvatarUrl());
            message.setText(user.getUsername() + text + blog.getTitle());
            save = this.save(message);
            if (!save) {
                log.error("用户：{} 收藏：{} 的博客：{} 后，添加收藏消息到消息表失败了！", fromId, toId, blogId);
            }
        }
        return save;
    }

    @Override
    public boolean addLikeMessage(Message message) {
        // 获取参数
        Long fromId = message.getFromId();
        Long toId = message.getToId();
        String text = message.getText();
        Long blogId = message.getBlogId();
        User user = userService.getById(fromId);
        Blog blog = blogService.getById(blogId);
        // 检查信息是否已经存在，且未读
        Long count = this.lambdaQuery().eq(Message::getToId, toId).eq(Message::getFromId, fromId)
                .eq(Message::getBlogId, blogId).eq(Message::getIsRead, 0).eq(Message::getType, 1).count();
        boolean save = false;
        // 不存在，则保存至数据库
        if(count!=null && count<1){
            message.setAvatarUrl(user.getAvatarUrl());
            message.setText(user.getUsername() + text + blog.getTitle());
            save = this.save(message);
            if (!save) {
                log.error("用户：{} 点赞：{} 的博客：{} 后，添加点赞消息到消息表失败了！", fromId, toId, blogId);
            }
        }
        return save;
    }

    @Override
    public boolean addFollowMessage(Message message) {
        // 获取参数
        Long fromId = message.getFromId();
        Long toId = message.getToId();
        String text = message.getText();
        User user = userService.getById(fromId);
        // 检查信息是否已经存在，且未读
        Long count = this.lambdaQuery().eq(Message::getToId, toId).eq(Message::getFromId, fromId)
                .eq(Message::getIsRead, 0).eq(Message::getType, 2).count();
        boolean save = false;
        // 不存在，则保存至数据库
        if(count!=null && count<1){
            // 补充用户信息
            message.setAvatarUrl(user.getAvatarUrl());
            message.setText(user.getUsername() + text );
            save = this.save(message);
            if (!save) {
                log.error("用户：{} 关注：{}  后，添加关注消息到消息表失败了！", fromId, toId);
            }
        }
        return save;
    }

    @Override
    public InteractionMessageVO listInteractionMessage(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        long userId = loginUser.getId();
        // 查找数据库,找未读信息
        long starMessageNum = this.baseMapper.selectCount(new QueryWrapper<Message>()
                .eq("toId", userId).eq("type", 0).eq("isRead", 0));
        long likeMessageNum = this.baseMapper.selectCount(new QueryWrapper<Message>()
                .eq("toId", userId).eq("type", 1).eq("isRead", 0));
        long followMessageNum = this.baseMapper.selectCount(new QueryWrapper<Message>()
                .eq("toId", userId).eq("type", 2).eq("isRead", 0));
        return new InteractionMessageVO(likeMessageNum,starMessageNum,followMessageNum);
    }

    @Override
    public List<MessageVO> listMessages(MessageQueryRequest messageQueryRequest, HttpServletRequest request) {
        // 提取效验参数
        Integer type = messageQueryRequest.getType();
        User loginUser = userService.getLoginUser(request);
        if (type == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 查询 message 数据库
        // todo : 改为 page 查询 ？？
        QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("toId", loginUser.getId());
        queryWrapper.eq("type", type);
        List<Message> messageList = this.list(queryWrapper);
        // 转换为 massageVo
        List<MessageVO> messageVOList = messageList.stream().map(message -> {
            MessageVO messageVO = new MessageVO();
            BeanUtils.copyProperties(message, messageVO);
            return messageVO;
        }).collect(Collectors.toList());
        // 查询的消息变为已读 , 并更新数据库
        // todo 可改为 MQ 执行
        List<Message> readedMessageList = messageList.stream().map(message -> {
            Message readedMessage = new Message();
            readedMessage.setId(message.getId());
            readedMessage.setIsRead(1);
            return readedMessage;
        }).collect(Collectors.toList());
        boolean b = this.updateBatchById(readedMessageList, 100);
        if (!b) {
            log.error("用户：{} 读取消息后将消息改为已读失败了！", loginUser.getId());
        }
        return messageVOList;
    }
}




