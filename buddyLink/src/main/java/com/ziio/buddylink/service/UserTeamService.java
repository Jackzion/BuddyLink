package com.ziio.buddylink.service;

import com.ziio.buddylink.model.domain.UserTeam;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Ziio
* @description 针对表【user_team(用户队伍关系表)】的数据库操作Service
* @createDate 2024-08-20 12:57:18
*/
public interface UserTeamService extends IService<UserTeam> {

    /**
     * 判断用户是否在队伍中
     * @param teamId
     * @param id
     * @return
     */
    boolean teamHasUser(Long teamId, Long id);
}
