package com.ziio.buddylink.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ziio.buddylink.common.BaseResponse;
import com.ziio.buddylink.common.ErrorCode;
import com.ziio.buddylink.common.ResultUtils;
import com.ziio.buddylink.exception.BusinessException;
import com.ziio.buddylink.model.domain.Team;
import com.ziio.buddylink.model.domain.User;
import com.ziio.buddylink.model.domain.UserTeam;
import com.ziio.buddylink.model.request.*;
import com.ziio.buddylink.model.vo.TeamUserVO;
import com.ziio.buddylink.model.vo.UserVO;
import com.ziio.buddylink.service.TeamService;
import com.ziio.buddylink.service.UserService;
import com.ziio.buddylink.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/team")
@Slf4j
public class TeamController {
    @Resource
    private TeamService teamService;

    @Resource
    private UserService userService;

    @Resource
    private UserTeamService userTeamService;

    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request){
        // 提取效验参数
        if(teamAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        // 封装数据
        Team team = new Team();
        try {
            BeanUtils.copyProperties(team, teamAddRequest);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        long teamId = teamService.addTeam(team, loginUser);
        return ResultUtils.success(teamId);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request){
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);

        long id = deleteRequest.getId();
        boolean result = teamService.deleteTeam(id, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解散失败！");
        }
        return ResultUtils.success(result);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request){
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);

        boolean result = teamService.updateTeam(teamUpdateRequest, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败！");
        }
        return ResultUtils.success(result);
    }

    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(long id){
        if (id <=0 ) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return ResultUtils.success(team);
    }

    @GetMapping("/list")
    public BaseResponse<List<TeamUserVO>> listTeams(TeamQueryRequest teamQuery, HttpServletRequest request) {
        // 效验参数
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (userService.getLoginUser(request) == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        boolean isAdmin = userService.isAdmin(request);
        // 1、查询所有相关关键词队伍列表
        List<TeamUserVO> teamList = teamService.listTeam(teamQuery, isAdmin);
        if (CollectionUtils.isEmpty(teamList)) {
            return ResultUtils.success(teamList);
        }
        final List<Long> teamIdList = teamList.stream().map(TeamUserVO::getId).collect(Collectors.toList());
        // 2、在 teamIdList 基础上 ，判断当前用户是否已加入队伍
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        try {
            // 找出用户相关的队伍
            User loginUser = userService.getLoginUser(request);
            userTeamQueryWrapper.eq("userId", loginUser.getId());
            userTeamQueryWrapper.in("teamId", teamIdList);
            List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
            // 已加入的队伍 id 集合
            Set<Long> hasJoinTeamIdSet = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
            teamList.forEach(team -> {
                boolean hasJoin = hasJoinTeamIdSet.contains(team.getId());
                team.setHasJoin(hasJoin);
            });
        } catch (Exception e) {}
        // 3、查询加入队伍的用户信息（人数）
        QueryWrapper<UserTeam> userTeamJoinQueryWrapper = new QueryWrapper<>();
        userTeamJoinQueryWrapper.in("teamId", teamIdList);
        List<UserTeam> userTeamList = userTeamService.list(userTeamJoinQueryWrapper);
        // 队伍id => 加入这个队伍的用户列表
        Map<Long, List<UserTeam>> teamIdUserTeamList = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        teamList.forEach(team ->{
            List<UserVO> UserVOList = teamIdUserTeamList.get(team.getId())
                    .stream().map(UserTeam -> {
                        User user = userService.getById(UserTeam.getUserId());
                        UserVO userVO = new UserVO();
                        BeanUtil.copyProperties(user, userVO);
                        return userVO;
                    }).collect(Collectors.toList());
            team.setUserList(UserVOList);
        });
        teamList.forEach(team -> {
            team.setHasJoinNum(teamIdUserTeamList.getOrDefault(team.getId(), new ArrayList<>()).size());
        });
        return ResultUtils.success(teamList);
    }

    @PostMapping("/search/es")
    public BaseResponse<List<TeamUserVO>> listTeamsFromEs(@RequestBody TeamQueryRequest teamQueryRequest, HttpServletRequest request) {
        if (teamQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (teamQueryRequest.getPageNum() <= 0 || teamQueryRequest.getPageSize() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<TeamUserVO> userVOList = teamService.listTeamsFromEs(teamQueryRequest, request);
        return ResultUtils.success(userVOList);
    }

    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVO>> listMyJoinTeams(HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        List<TeamUserVO> teamList = teamService.listMyJoinTeam(loginUser);
        return ResultUtils.success(teamList);
    }

    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamUserVO>> listMyCreateTeams(HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        List<TeamUserVO> teamList = teamService.listMyCreateTeam(loginUser);
        return ResultUtils.success(teamList);
    }

    // todo : 没做 is not null sql 效验 , searchText 失效
    @GetMapping("/list/page")
    public BaseResponse<IPage<Team>> listTeamsByPage(TeamQueryRequest teamQuery, HttpServletRequest request){
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (userService.getLoginUser(request) == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Team team = new Team();
        try {
            BeanUtils.copyProperties(team, teamQuery);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        IPage<Team> page = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize());
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
        IPage<Team> teamIPage = teamService.page(page, queryWrapper);
        return ResultUtils.success(teamIPage);
    }


    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request){
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (userService.getLoginUser(request) == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        User loginUser = userService.getLoginUser(request);

        boolean result = teamService.joinTeam(teamJoinRequest, loginUser);
        return ResultUtils.success(result);
    }

    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request){
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (userService.getLoginUser(request) == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.quitTeam(teamQuitRequest, loginUser);
        return ResultUtils.success(result);
    }
}
