package com.ziio.buddylink.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ziio.buddylink.common.ErrorCode;
import com.ziio.buddylink.constant.RedisConstant;
import com.ziio.buddylink.exception.BusinessException;
import com.ziio.buddylink.mapper.UserMapper;
import com.ziio.buddylink.model.domain.Friend;
import com.ziio.buddylink.model.domain.User;
import com.ziio.buddylink.model.request.FriendQueryRequest;
import com.ziio.buddylink.model.vo.UserVO;
import com.ziio.buddylink.service.FriendService;
import com.ziio.buddylink.mapper.FriendMapper;
import com.ziio.buddylink.service.UserService;
import io.lettuce.core.RedisClient;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.StringValue;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.geo.Distance;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
* @author Ziio
* @description 针对表【friend(好友表)】的数据库操作Service实现
* @createDate 2024-08-19 10:46:41
*/
@Service
@Slf4j
public class FriendServiceImpl extends ServiceImpl<FriendMapper, Friend>
    implements FriendService{

    @Resource
    private UserService userService;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private FriendMapper friendMapper;

    @Resource
    private UserMapper userMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean addFriend(long userId, Long friendId) {
        // 效验
        if (userId == friendId) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "自己不能添加自己为好友'");
        }
        // 用户不存在
        User user = userMapper.selectById(friendId);
        if(user==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户不存在'");
        }
        // 设置分布式锁，避免多线程重复添加
        // 设置锁名称，锁范围是同一个人加同一个人为好友
        String addUserLock = RedisConstant.USER_ADD_KEY + userId + friendId;
        RLock lock = redissonClient.getLock(addUserLock);
        boolean result1 = false;
        boolean result2 = false;
        try{
            // 获取锁 -- 0- 表示不等待 false
            if(lock.tryLock(0,30000, TimeUnit.SECONDS)){
                log.info(Thread.currentThread().getId() + "我拿到锁了");
                // 查询是否添加了该用户
                QueryWrapper<Friend> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("userId",userId);
                queryWrapper.eq("friendId",friendId);
                Long count1 = friendMapper.selectCount(queryWrapper);
                if (count1 > 0) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "已添加该用户");
                }
                // 判断该用户是否已经添加了你
                queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("userId",friendId);
                queryWrapper.eq("friendId",userId);
                Long count2 = friendMapper.selectCount(queryWrapper);
                if (count2 > 0) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "已添加该用户");
                }
                // 插入数据库 (两段 friend关系，你和他 ， 他和你 ）
                // 插入id: userId, friendId: friendId
                Friend friendByUserId = new Friend();
                friendByUserId.setUserId(userId);
                friendByUserId.setFriendId(friendId);
                // 插入id:friendId , friendId: userId（注意添加事务，即要么都添加要么都不添加）
                result1 = this.save(friendByUserId);
                Friend friendByFriendId = new Friend();
                friendByFriendId.setUserId(friendId);
                friendByFriendId.setFriendId(userId);
                // 写入数据库
                result2 = this.save(friendByFriendId);
            }
        }
        catch (InterruptedException e){
            log.error("addUser error", e);
        }
        finally {
            if (lock.isHeldByCurrentThread()) {
                log.info(Thread.currentThread().getId() + "锁已经被释放");
                lock.unlock();
            }
        }
        return result1 && result2;
    }

    @Override
    public List<UserVO> listFriends(long userId, HttpServletRequest request) {
        // 获取我 （方便得到地理位置）
        User loginUser = userService.getLoginUser(request);
        // 查询数据库中目标的好友列表
        QueryWrapper<Friend> queryWrapper = new QueryWrapper();
        queryWrapper.eq("userId", userId);
        List<Friend> friendList = friendMapper.selectList(queryWrapper);
        List<User> userList = friendList.stream().map(friend -> {
            User user = userMapper.selectById(friend.getFriendId());
            return user;
        }).collect(Collectors.toList());
        // user 转为 userVO
        String redisUserGeoKey = RedisConstant.USER_GEO_KEY;
        List<UserVO> userVOList = userList.stream().map(user -> {
            UserVO userVO = new UserVO();
            BeanUtil.copyProperties(user, userVO);
            // 补充地理信息
            Distance distance = stringRedisTemplate.opsForGeo().distance(redisUserGeoKey, String.valueOf(loginUser.getId()),
                    String.valueOf(user.getId()), RedisGeoCommands.DistanceUnit.KILOMETERS);
            userVO.setDistance(distance.getValue());
            return userVO;
        }).collect(Collectors.toList());
        return userVOList;
    }

    @Override
    public List<UserVO> searchFriends(FriendQueryRequest friendQueryRequest, long userId) {
        String searchParam = friendQueryRequest.getSearchParam();
        // 查询用户的好友列表
        QueryWrapper<Friend> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        List<Friend> friendList = this.list(queryWrapper);
        List<Long> frinedIdList = friendList.stream().map(Friend::getFriendId).collect(Collectors.toList());
        // 根据 id 和 userName 模糊查询
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", frinedIdList);
        userQueryWrapper.like("username", searchParam);
        List<User> userList = userService.list(userQueryWrapper);
        // user 转为 userVo
        String redisUserGeoKey = RedisConstant.USER_GEO_KEY;
        List<UserVO> userVOList = userList.stream().map(user -> {
            UserVO userVO = new UserVO();
            BeanUtil.copyProperties(user, userVO);
            // 补充地理信息
            Distance distance = stringRedisTemplate.opsForGeo().distance(redisUserGeoKey, String.valueOf(userId),
                    String.valueOf(user.getId()), RedisGeoCommands.DistanceUnit.KILOMETERS);
            userVO.setDistance(distance.getValue());
            return userVO;
        }).collect(Collectors.toList());
        return userVOList;
    }
}




