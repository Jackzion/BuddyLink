package com.ziio.buddylink.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ziio.buddylink.common.ErrorCode;
import com.ziio.buddylink.exception.BusinessException;
import com.ziio.buddylink.mapper.FollowMapper;
import com.ziio.buddylink.model.domain.Follow;
import com.ziio.buddylink.model.domain.Message;
import com.ziio.buddylink.model.domain.User;
import com.ziio.buddylink.model.request.FollowQueryRequest;
import com.ziio.buddylink.model.vo.FollowVO;
import com.ziio.buddylink.service.FollowService;
import com.ziio.buddylink.service.MessageService;
import com.ziio.buddylink.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Ziio
* @description 针对表【follow(关注表)】的数据库操作Service实现
* @createDate 2024-08-18 18:50:40
*/
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow>
    implements FollowService{

    @Resource
    private UserService userService;

    @Resource
    private MessageService messageService;

    @Override
    public boolean isFollowed(Long userId, long followerId) {
        if (userId <= 0 || followerId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User followee = userService.getById(userId);
        if (followee == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "关注对象不存在");
        }
        return lambdaQuery().eq(Follow::getUserId, userId).eq(Follow::getFollowerId, followerId).count() > 0;
    }

    @Override
    public List<FollowVO> listFollows(FollowQueryRequest followQueryRequest, HttpServletRequest request) {
        // 提取参数
        User loginUser = userService.getLoginUser(request);
        long loginUserId = loginUser.getId();
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addFollow(long followeeId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        long loginUserId = loginUser.getId();
        User followee = userService.getById(followeeId);
        loginUser = userService.getById(loginUserId);
        if(loginUserId==followeeId){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"自己不能关注自己");
        }
        if(isFollowed(followeeId,loginUserId)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"你已经关注");
        }
        // 保存 follow 到数据库
        Follow follow = new Follow();
        follow.setFollowerId(loginUserId);
        follow.setUserId(followeeId);
        boolean save = this.save(follow);
        // 增加被关注者粉丝数
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id",followeeId);
        updateWrapper.eq("fanNum",followee.getFanNum()); // CAS
        updateWrapper.setSql("fanNum = fanNum + 1");
        boolean updateFollowee = userService.update(updateWrapper);
        // 增加关注者的关注数
        updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id",loginUserId);
        updateWrapper.eq("followNum",loginUser.getFollowNum()); // CAS
        updateWrapper.setSql("followNum = followNum + 1");
        boolean updateLoginUser = userService.update(updateWrapper);
        // 保存 message
        Message message = new Message();
        message.setFromId(loginUserId);
        message.setToId(followeeId);
        message.setType(2);
        message.setText("关注了您");
        boolean addMessage = messageService.addFollowMessage(message);
        if(!addMessage || !updateLoginUser || !updateFollowee || !save){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新数据库失败");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFollow(long followeeId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        long loginUserId = loginUser.getId();
        User followee = userService.getById(followeeId);
        loginUser = userService.getById(loginUserId);
        if(loginUserId==followeeId){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"自己取关关注自己");
        }
        if(!isFollowed(followeeId,loginUserId)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"你还未关注");
        }
        // delete follow 到数据库
        Follow follow = new Follow();
        follow.setFollowerId(loginUserId);
        follow.setUserId(followeeId);
        boolean remove = this.lambdaUpdate().eq(Follow::getFollowerId, loginUserId).eq(Follow::getUserId, followeeId).remove();
        // 减少被关注者粉丝数
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id",followeeId);
        updateWrapper.eq("fanNum",followee.getFanNum()); // CAS
        updateWrapper.setSql("fanNum = fanNum - 1");
        boolean updateFollowee = userService.update(updateWrapper);
        // 减少关注者的关注数
        updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id",loginUserId);
        updateWrapper.eq("followNum",loginUser.getFollowNum()); // CAS
        updateWrapper.setSql("followNum = followNum - 1");
        boolean updateLoginUser = userService.update(updateWrapper);

        if( !updateLoginUser || !updateFollowee || !remove){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新数据库失败");
        }
        return true;
    }
}




