package com.ziio.buddylink.service;

import com.ziio.buddylink.model.domain.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Ziio
* @description 针对表【follow(关注表)】的数据库操作Service
* @createDate 2024-08-18 18:50:40
*/
public interface FollowService extends IService<Follow> {

    /**
     * 判断是否关注
     * @param blogUserId 作者
     * @param userId 用户
     * @return
     */
    boolean isFollowed(Long blogUserId, long userId);
}
