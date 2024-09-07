package com.ziio.buddylink.service;

import com.ziio.buddylink.model.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ziio.buddylink.model.domain.User;
import com.ziio.buddylink.model.request.TeamJoinRequest;
import com.ziio.buddylink.model.request.TeamQueryRequest;
import com.ziio.buddylink.model.request.TeamQuitRequest;
import com.ziio.buddylink.model.request.TeamUpdateRequest;
import com.ziio.buddylink.model.vo.TeamUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Ziio
* @description 针对表【team(队伍信息)】的数据库操作Service
* @createDate 2024-08-20 12:54:01
*/
public interface TeamService extends IService<Team> {

    /**
     * 创建队伍
     * @param team
     * @param loginUser
     * @return
     */
    long addTeam(Team team, User loginUser);

    /**
     * 解散 or 刪除 隊伍
     * @param id
     * @param loginUser
     * @return
     */
    boolean deleteTeam(long id, User loginUser);

    /**
     * 更新队伍信息
     * @param teamUpdateRequest
     * @param loginUser
     * @return
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    /**
     * 查询所有队伍列表
     * @param teamQuery
     * @param isAdmin
     * @return
     */
    List<TeamUserVO> listTeam(TeamQueryRequest teamQuery, boolean isAdmin);

    /**
     * 获取我所在的队伍
     * @param loginUser
     * @return
     */
    List<TeamUserVO> listMyJoinTeam(User loginUser);

    /**
     * 列举我创建的队伍
     * @param loginUser
     * @return
     */
    List<TeamUserVO> listMyCreateTeam(User loginUser);

    /**
     * 加入队伍
     * @param teamJoinRequest
     * @param loginUser
     * @return
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    /**
     * 退出队伍
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    List<TeamUserVO> listTeamsFromEs(TeamQueryRequest teamQueryRequest, HttpServletRequest request);
}
