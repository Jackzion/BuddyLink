package com.ziio.buddylink.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ziio.buddylink.common.ErrorCode;
import com.ziio.buddylink.exception.BusinessException;
import com.ziio.buddylink.mapper.FollowMapper;
import com.ziio.buddylink.model.domain.Follow;
import com.ziio.buddylink.model.domain.User;
import com.ziio.buddylink.service.FollowService;
import com.ziio.buddylink.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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
}




