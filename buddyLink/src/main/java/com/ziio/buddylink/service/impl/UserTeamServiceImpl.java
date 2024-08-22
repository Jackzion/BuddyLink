package com.ziio.buddylink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ziio.buddylink.model.domain.UserTeam;
import com.ziio.buddylink.service.UserTeamService;
import com.ziio.buddylink.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

/**
* @author Ziio
* @description 针对表【user_team(用户队伍关系表)】的数据库操作Service实现
* @createDate 2024-08-20 12:57:18
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService{

    @Override
    public boolean teamHasUser(Long teamId, Long id) {
        return this.lambdaQuery().eq(UserTeam::getTeamId, teamId).eq(UserTeam::getUserId, id).count() > 0;
    }
}




