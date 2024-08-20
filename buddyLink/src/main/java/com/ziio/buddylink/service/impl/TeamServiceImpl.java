package com.ziio.buddylink.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ziio.buddylink.common.BaseResponse;
import com.ziio.buddylink.common.ErrorCode;
import com.ziio.buddylink.common.ResultUtils;
import com.ziio.buddylink.constant.RedisConstant;
import com.ziio.buddylink.exception.BusinessException;
import com.ziio.buddylink.model.domain.Team;
import com.ziio.buddylink.model.domain.User;
import com.ziio.buddylink.model.domain.UserTeam;
import com.ziio.buddylink.model.enums.TeamStatusEnum;
import com.ziio.buddylink.model.request.TeamJoinRequest;
import com.ziio.buddylink.model.request.TeamQueryRequest;
import com.ziio.buddylink.model.request.TeamQuitRequest;
import com.ziio.buddylink.model.request.TeamUpdateRequest;
import com.ziio.buddylink.model.vo.TeamUserVO;
import com.ziio.buddylink.model.vo.UserVO;
import com.ziio.buddylink.service.TeamService;
import com.ziio.buddylink.mapper.TeamMapper;
import com.ziio.buddylink.service.UserService;
import com.ziio.buddylink.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
* @author Ziio
* @description 针对表【team(队伍信息)】的数据库操作Service实现
* @createDate 2024-08-20 12:54:01
*/
@Service
@Slf4j
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService{

    @Resource
    private UserService userService;

    @Resource
    private UserTeamService userTeamService;

    @Resource
    private RedissonClient redissonClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addTeam(Team team, User loginUser) {
        // 1. 请求参数是否为空
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2. 是否登录，未登录不允许创建
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        final long userId = loginUser.getId();
        // 3. 校验信息
        // 1. 队伍人数 > 1，且 <= 20
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if (maxNum < 1 || maxNum > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不满足要求");
        }
        // 2. 队伍标题 <= 20
        String teamName = team.getTeamName();
        if (StringUtils.isBlank(teamName) || teamName.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍标题不符合要求");
        }
        // 3. 描述 <= 512
        String description = team.getDescription();
        if (StringUtils.isBlank(description) || description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述不符合要求");
        }
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不符合要求");
        }
        // 5. 如果status是加密状态，一定要有密码，且密码 <= 10
        String password = team.getPassword();
        if (TeamStatusEnum.SECRET.equals(statusEnum) &&
                (StringUtils.isBlank( password) || password.length() > 10)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码格式不正确");
        }
        // 6. 超时时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if (new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "过期时间不能小于当前时间");
        }
        // 7. 校验用户只能创建5个队伍
        // todo 有bug，可能同时创建100个队伍
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        long hasTeamNum = this.count(queryWrapper);
        if (hasTeamNum >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多创建5个队伍");
        }
        // 8. 插入队伍信息到 team 表
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team);
        Long teamId = team.getId();
        if (!result || teamId == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建队伍失败");
        }
        // 8. 插入队伍用户信息到 user_team 表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建队伍失败");
        }
        return teamId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTeam(long id, User loginUser) {
        // 1. 校验请求参数
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2. 校验队伍是否存在
        Team team = this.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        Long teamId = team.getId();
        // 3.效验你是否为队长
        Long userId = team.getUserId();
        if (userId != loginUser.getId()) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        // 4. 移除所有 user_team 表的关联信息
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        boolean removeUserTeam = userTeamService.remove(userTeamQueryWrapper);
        if (!removeUserTeam) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍关联信息失败");
        }
        // 5. 删除队伍
        boolean removeTeam = this.removeById(teamId);
        if (!removeTeam) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍信息失败");
        }
        return true;
    }

    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        // 提取效验参数
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = teamUpdateRequest.getId();
        if (id == null || id <=0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team oldTeam = this.getById(id);
        if (oldTeam == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        // 只有管理员和队伍创建人才能更新队伍信息
        if (!oldTeam.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(teamUpdateRequest.getStatus());
        if (statusEnum.equals(TeamStatusEnum.SECRET)) {
            if (StringUtils.isBlank(teamUpdateRequest.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密房间需要设置密码");
            }
        }
        Team updateTeam = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, updateTeam);
        return this.updateById(updateTeam);
    }

    @Override
    public List<TeamUserVO> listTeam(TeamQueryRequest teamQuery, boolean isAdmin) {
        // 1. 组合查询条件
        QueryWrapper<Team> queryWrapper =  new QueryWrapper<>();
        if (teamQuery != null){
            Long id = teamQuery.getId();
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }
            List<Long> idList = teamQuery.getIdList();
            if (CollectionUtils.isNotEmpty(idList)) {
                queryWrapper.in("id" ,idList);
            }
            // 搜索关键词从name字段和description两个字段里面查
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                queryWrapper.and(qw -> qw.like("teamName", searchText).or().like("description", searchText));
            }
            String teamName = teamQuery.getTeamName();
            if (StringUtils.isNotBlank(teamName)) {
                queryWrapper.like("teamName", teamName);
            }
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }
            Integer maxNum = teamQuery.getMaxNum();
            if (maxNum != null && maxNum > 0) {
                // 查询人数相等的
                queryWrapper.eq("maxNum", maxNum);
            }
            // 根据创建人查询
            Long userId = teamQuery.getUserId();
            if (userId != null && userId > 0) {
                queryWrapper.eq("userId", userId);
            }
            // 根据状态查询，只查询公开的队伍
            Integer status = teamQuery.getStatus();
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            // statusEnum为空说明其是公开的
            if (statusEnum == null){
                statusEnum = TeamStatusEnum.PUBLIC;
            }
            if (!isAdmin && statusEnum.equals(TeamStatusEnum.PRIVATE)) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
            queryWrapper.eq("status", statusEnum.getValue());
        }
        // 不展示已过期队伍 and
        queryWrapper.and(qw -> qw.gt("expireTime",new Date()).or().isNull("expireTime"));
        // todo : 改为分页查询
        List<Team> teamList = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }
        // 关联查询创建用户信息 ，转为 TeamUserVo
        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        for(Team team : teamList){
            Long userId = team.getUserId();
            User user = userService.getById(userId);
            UserVO userVO = new UserVO();
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtil.copyProperties(user,userVO);
            BeanUtil.copyProperties(team,teamUserVO);
            teamUserVO.setCreateUser(userVO);
            teamUserVOList.add(teamUserVO);
        }
        return teamUserVOList;
    }

    @Override
    public List<TeamUserVO> listMyJoinTeam(User loginUser) {
        long loginUserId = loginUser.getId();
        User user = userService.getById(loginUserId);
        // teamUser 表中查询我加入的队伍 -- teamUserList
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUserId);
        List<Long> teamIdList = userTeamService.list(queryWrapper).stream().map(UserTeam::getTeamId).collect(Collectors.toList());
        // 转为 TeamUserVOList -- set userVO and teamVO
        List<TeamUserVO> teamUserVOList = teamIdList.stream().map(teamId -> {
            UserVO userVO = new UserVO();
            Team team = this.getById(teamId);
            User createUser = userService.getById(team.getUserId());
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);
            BeanUtils.copyProperties(createUser, userVO);
            teamUserVO.setCreateUser(userVO);
            // todo : 查询 UserVOList？
            int hasJoinNum = (int) teamHasUserNum(teamId);
            teamUserVO.setHasJoinNum(hasJoinNum);
            QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
            // 不能重复加入已加入的队伍
            // todo: 多余效验？
            userTeamQueryWrapper.eq("userId", loginUserId);
            userTeamQueryWrapper.eq("teamId", teamId);
            boolean hasJoin = userTeamService.count(userTeamQueryWrapper) == 1; // 同一个用户加入同一个队伍的数量
            teamUserVO.setHasJoin(hasJoin);
            return teamUserVO;
        }).collect(Collectors.toList());
        return teamUserVOList;
    }

    @Override
    public List<TeamUserVO> listMyCreateTeam(User loginUser) {
        long userId = loginUser.getId();
        User user = userService.getById(userId);
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        teamQueryWrapper.eq("userId", userId);
        List<Team> myCreateTeamList = this.list(teamQueryWrapper);
        List<Long> teamIdList = myCreateTeamList.stream().map(Team::getId).collect(Collectors.toList());
        // 封装为 TeamUserVOList
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        List<TeamUserVO> teamUserVOList = teamIdList.stream().map(teamId -> {
            UserVO userVO = new UserVO();
            Team team = this.getById(teamId);
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);
            BeanUtils.copyProperties(user, userVO);
            teamUserVO.setCreateUser(userVO);
            int hasJoinNum = (int) teamHasUserNum(teamId);
            teamUserVO.setHasJoinNum(hasJoinNum);
            QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
            // 不能重复加入已加入的队伍
            userTeamQueryWrapper.eq("userId", userId);
            userTeamQueryWrapper.eq("teamId", teamId);
            boolean hasJoin = userTeamService.count(userTeamQueryWrapper) == 1; // 同一个用户加入同一个队伍的数量
            teamUserVO.setHasJoin(hasJoin);
            return teamUserVO;
        }).collect(Collectors.toList());
        return teamUserVOList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        long loginUserId = loginUser.getId();
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamJoinRequest.getTeamId();
        Team team = this.getById(teamId);
        if(team==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍不存在");
        }
        if (team.getExpireTime() != null && team.getExpireTime().before(new Date())) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍已过期");
        }
        Integer status = team.getStatus();
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (TeamStatusEnum.PRIVATE.equals(statusEnum)) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "禁止加入私有队伍");
        }
        String password = teamJoinRequest.getPassword();
        if (statusEnum.equals(TeamStatusEnum.SECRET)) {
            if (StringUtils.isBlank(password) || !password.equals(team.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }
        // 避免重复入队 ，获取分布式锁(对同一队伍加锁）
        String lockKey = RedisConstant.USER_JOIN_TEAM + teamId;
        RLock lock = redissonClient.getLock(lockKey);
        try{
            // 一个线程获取队伍锁 ， 剩下线程重试
            while (true){
                if(lock.tryLock(0,30000, TimeUnit.SECONDS)){
                    log.info(Thread.currentThread().getId() + "我拿到锁了");
                    // 查询 user-team 表 ， 用户加入队伍数量 , 限制五个
                    QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("userId", loginUserId);
                    long userJoinTeamNum = userTeamService.count(queryWrapper); // 用户加入的队伍数量
                    if (userJoinTeamNum >= 5) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多加入5个队伍");
                    }
                    // 查询 user-team 表 ， 避免重复入队
                    queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("userId", loginUserId);
                    queryWrapper.eq("teamId", teamId);
                    long hasJoinNum = userTeamService.count(queryWrapper); // 同一个用户加入同一个队伍的数量
                    if (hasJoinNum > 0) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "已经加入该队伍");
                    }
                    // 查询 team 表 ， 队伍人数判断
                    // 队伍中的用户数量
                    long teamHasUserNum = this.teamHasUserNum(teamId);
                    if (teamHasUserNum >= team.getMaxNum()) {
                        throw new BusinessException(ErrorCode.NULL_ERROR, "队伍已满");
                    }
                    // 将用户加入队伍的信息添加到user_team表中
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(loginUserId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    return userTeamService.save(userTeam);
                }
            }
        } catch (InterruptedException e) {
            log.error("User join team error，获取分布式锁后执行逻辑异常", e);
            return  false;
        }finally {
            // 只释放自己的锁 ， 避免锁提前释放释放了别人的锁
            // redisson 用了 线程名作为判断
            if(lock.isHeldByCurrentThread()){
                log.info(Thread.currentThread().getId() + "锁已经释放了");
                lock.unlock(); // 执行业务逻辑后，要释放锁
            }
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        // 提取效验参数
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamQuitRequest.getTeamId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        long userId = loginUser.getId();
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("teamId", teamId);
        long count = userTeamService.count(queryWrapper);
        if (count < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "你还未加入该队伍");
        }
        long teamHasUserNum = teamHasUserNum(teamId);
        boolean updateTeam = false;
        if(teamHasUserNum == 1){
            // 队伍还剩一人 ， 解散队伍
            updateTeam = this.removeById(teamId);
        }else{
            // 队伍还有两人以上
            if(team.getUserId() == loginUser.getId()){
                // 队长 ， 转让队长给第二早的队员
                // 查询 user-team 表 teamId ，按 joinTime 升序排序 ，第二个就是下一个队长
                queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("teamId", teamId);
                queryWrapper.last("order by joinTime asc limit 2");
                List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
                if (CollectionUtils.isEmpty(userTeamList) || userTeamList.size() <= 1) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
                UserTeam nextUserTeam = userTeamList.get(1);
                Long nextLeaderId = nextUserTeam.getUserId();
                // update team 数据库
                Team newTeam = new Team();
                newTeam.setId(teamId);
                newTeam.setUserId(nextLeaderId);
                updateTeam = this.updateById(newTeam);
            }
        }
        if (!updateTeam) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新 team 数据库失败 ");
        }
        // 移除前队长或者队员在user_team中的关系
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId);
        queryWrapper.eq("userId", userId);
        boolean removeUserTeam = userTeamService.remove(queryWrapper);
        if(!removeUserTeam){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "failed to remove user-team ");
        }
        return true;
    }

    private long teamHasUserNum(Long teamId) {
        // 队伍里的用户人数
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId);
        long teamHasUserNum = userTeamService.count(queryWrapper); // 队伍里的用户人数
        return teamHasUserNum;
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request){
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);

        boolean result = this.updateTeam(teamUpdateRequest, loginUser);
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
        Team team = this.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return ResultUtils.success(team);
    }
}




