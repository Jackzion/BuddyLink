package com.ziio.buddylink.service;

import com.ziio.buddylink.model.domain.Friend;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ziio.buddylink.model.request.FriendQueryRequest;
import com.ziio.buddylink.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Ziio
* @description 针对表【friend(好友表)】的数据库操作Service
* @createDate 2024-08-19 10:46:41
*/
public interface FriendService extends IService<Friend> {

    /**
     * 添加好友（分布式锁)
     * @param userId
     * @param friendId
     * @return
     */
    boolean addFriend(long userId, Long friendId);

    /**
     * 获取目标好友列表
     * @param userId 对象目标（不一定是我）
     * @param request 我
     * @return
     */
    List<UserVO> listFriends(long userId, HttpServletRequest request);

    /**
     * 根据用户名查询本人的好友
     * @param friendQueryRequest
     * @param userId 本人
     * @return
     */
    List<UserVO> searchFriends(FriendQueryRequest friendQueryRequest, long userId);
}
