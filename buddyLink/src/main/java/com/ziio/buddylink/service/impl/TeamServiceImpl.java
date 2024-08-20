package com.ziio.buddylink.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ziio.buddylink.model.domain.Team;
import com.ziio.buddylink.service.TeamService;
import com.ziio.buddylink.mapper.TeamMapper;
import org.springframework.stereotype.Service;

/**
* @author Ziio
* @description 针对表【team(队伍信息)】的数据库操作Service实现
* @createDate 2024-08-20 12:54:01
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService{

}




