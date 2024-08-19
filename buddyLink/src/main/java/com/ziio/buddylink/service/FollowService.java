package com.ziio.buddylink.service;

import com.ziio.buddylink.model.domain.Follow;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ziio.buddylink.model.request.FollowQueryRequest;
import com.ziio.buddylink.model.vo.FollowVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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

    /**
     * 查看 userId 的关注列表
     * @param followQueryRequest -- userid
     * @param request -- 用来效验 isMine 权限
     * @return
     */
    List<FollowVO> listFollows(FollowQueryRequest followQueryRequest, HttpServletRequest request);

    /**
     * 添加关注
     * @param followeeId up 主
     * @param request
     * @return
     */
    boolean addFollow(long followeeId, HttpServletRequest request);

    /**
     * 取消关注
     * @param followeeId up 主
     * @param request
     * @return
     */
    boolean deleteFollow(long followeeId, HttpServletRequest request);
}
