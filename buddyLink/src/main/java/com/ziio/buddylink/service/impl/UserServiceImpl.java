package com.ziio.buddylink.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.ziio.buddylink.common.ErrorCode;
import com.ziio.buddylink.constant.RedisConstant;
import com.ziio.buddylink.constant.UserConstant;
import com.ziio.buddylink.exception.BusinessException;
import com.ziio.buddylink.manager.RedisBloomFilter;
import com.ziio.buddylink.manager.RedisLimiterManager;
import com.ziio.buddylink.model.VO.UserVO;
import com.ziio.buddylink.model.domain.User;
import com.ziio.buddylink.model.request.UserEditRequest;
import com.ziio.buddylink.model.request.UserRegisterRequest;
import com.ziio.buddylink.service.UserService;
import com.ziio.buddylink.mapper.UserMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.ziio.buddylink.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author Ziio
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2024-08-16 15:40:56
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisBloomFilter redisBloomFilter;

    @Resource
    private Retryer<Boolean> retryer;
    /**
     * 盐值为'ziio'，用以混淆密码
     */
    private static final String SALT = "ziio";

    // 表示 Redis 是否有 recommend user 数据
    private boolean redisHasData = false;

    @Override
    public long userRegister(HttpServletRequest request, UserRegisterRequest userRegisterRequest) {
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        List<String> tagNameList = userRegisterRequest.getTagNameList();
        String username = userRegisterRequest.getUsername();
        Double longitude = userRegisterRequest.getLongitude();
        Double dimension = userRegisterRequest.getDimension();
        String ip = request.getRemoteHost();

        //1.校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短，至少要4位");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短，至少要8位");
        }
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请至少选择一个标签");
        }
        if (StringUtils.isBlank(username) || username.length() > 10) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称不合法，长度不得超过 10");
        }
        if (longitude == null || longitude > 180 || longitude < -180) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "坐标经度不合法");
        }
        if (dimension == null || dimension > 90 || dimension < -90) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "坐标维度不合法");
        }
        // 限流 ( ip 两分钟 一次请求）
        redisLimiterManager.doRateLimiter(RedisConstant.REDIS_LIMITER_REGISTER + ip, 2, 1);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        Long userCount = this.baseMapper.selectCount(queryWrapper);
        if (userCount > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }
        //2.密码加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        //3.封装数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setLongitude(longitude);
        user.setDimension(dimension);
        user.setUsername(username);
        //4.处理用户标签(list 轉 json)
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('[');
        for (int i = 0; i < tagNameList.size(); i++) {
            stringBuilder.append('"').append(tagNameList.get(i)).append('"');
            if (i < tagNameList.size() - 1) {
                stringBuilder.append(',');
            }
        }
        stringBuilder.append(']');
        user.setTags(stringBuilder.toString());
        //5.保存数据
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "添加失败");
        }
        //6.保存 redis geo 信息
        if(saveResult){
            Long addToRedisResult = stringRedisTemplate.opsForGeo().add(RedisConstant.USER_GEO_KEY,
                    new Point(user.getLongitude(), user.getDimension()), String.valueOf(user.getId()));
            if (addToRedisResult == null || addToRedisResult <= 0) {
                log.error("用户注册时坐标信息存入Redis失败");
            }
        }
        long userId = user.getId();
        // 7. 添加到布隆过滤器
        redisBloomFilter.addUserToFilter(userId);
        // 8. todo: why 删除用户缓存 , why 重试
        Set<String> keys = stringRedisTemplate.keys(RedisConstant.USER_RECOMMEND_KEY + ":*");
        for (String key : keys) {
            try {
                // 重试机制，保证删除成功
                retryer.call(() -> stringRedisTemplate.delete(key));
            } catch (ExecutionException e) {
                log.error("用户注册后删除缓存重试时失败");
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            } catch (RetryException e) {
                log.error("用户注册后删除缓存达到最大重试次数或超过时间限制");
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        }
        return userId;
    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1.校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号少于4位");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码少于8位");
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户不能包含特殊字符");
        }
        // 2.加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号密码不匹配");
        }
        // 3.用户脱敏
        User safetyUser = getSafetyUser(user);
        // 4.记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);
        return safetyUser;
    }

    @Override
    public int userLogout(HttpServletRequest request) {
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 用户脱敏
     * @param originUser 用户信息
     * @return 用户简略信息
     */
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setProfile(originUser.getProfile());
        safetyUser.setTags(originUser.getTags());
        safetyUser.setLongitude(originUser.getLongitude());
        safetyUser.setDimension(originUser.getDimension());
        return safetyUser;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return (User) userObj;
    }

    @Override
    public boolean isAdmin(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && user.getUserRole() == UserConstant.ADMIN_ROLE;
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && user.getUserRole() == UserConstant.ADMIN_ROLE;
    }

    @Override
    public int updateUser(UserEditRequest userEditRequest, User loginUser) {
        long userId = userEditRequest.getId();
        // 如果是管理员允许更新任意用户信息
        if (userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (this.getById(userId) == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "用户不存在");
        }
        // todo 补充更多校验，如果用户传的值只有id，没有其它参数则不执行更新操作
        // 如果是管理员，允许更新任意用户信息，只允许更新当前用户信息
        if (!isAdmin(loginUser) && userId != loginUser.getId()) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        User oldUser = this.baseMapper.selectById(userId);
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "用户不存在");
        }
        Double longitude = userEditRequest.getLongitude();
        Double dimension = userEditRequest.getDimension();
        if (longitude != null && (longitude > 180 || longitude < -180)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "坐标经度不合法");
        }
        if (dimension != null && (dimension > 90 || dimension < -90)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "坐标维度不合法");
        }
        User user = new User();
        BeanUtils.copyProperties(userEditRequest, user);
        // 注：前端传来的 tag list 处理（无法 copy 更新）
        List<String> tags = userEditRequest.getTags();
        if (!CollectionUtils.isEmpty(tags)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append('[');
            for (int i = 0; i < tags.size(); i++) {
                stringBuilder.append('"').append(tags.get(i)).append('"');
                if (i < tags.size() - 1) {
                    stringBuilder.append(',');
                }
            }
            stringBuilder.append(']');
            user.setTags(stringBuilder.toString());
        }
        int i = this.baseMapper.updateById(user);
        if (i > 0) {
            Set<String> keys = stringRedisTemplate.keys(RedisConstant.USER_RECOMMEND_KEY + ":*");
            for (String key : keys) {
                try {
                    retryer.call(() -> stringRedisTemplate.delete(key));
                } catch (ExecutionException e) {
                    log.error("用户修改信息后删除缓存重试时失败");
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                } catch (RetryException e) {
                    log.error("用户修改信息后删除缓存达到最大重试次数或超过时间限制");
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
            }
        }
        return i;
    }

    @Override
    public List<UserVO> recommendUsers(long pageSize, long pageNum, HttpServletRequest request) {
        User loginUser = this.getLoginUser(request);
        String redisKey = RedisConstant.USER_RECOMMEND_KEY + ":" + loginUser.getId();
        // 如果缓存中有数据，直接读缓存
        long start = (pageNum - 1) * pageSize;
        long end = start + pageSize - 1;
        List<String> userVOJsonListRedis = stringRedisTemplate.opsForList().range(redisKey, start, end);
        // 将查询的缓存反序列化为 User 对象
        List<UserVO> userVOList = new ArrayList<>();
        userVOList = userVOJsonListRedis.stream()
                .map(UserServiceImpl::transferToUserVO).collect(Collectors.toList());
        // 判断 Redis 中是否有数据
        redisHasData = !CollectionUtils.isEmpty(stringRedisTemplate.opsForList().range(redisKey, 0, -1));
        if (!CollectionUtils.isEmpty(userVOJsonListRedis)) {
            return userVOList;
        }
        // 缓存无数据再走数据库 ， 先写入缓存，在从缓存中拿数据
        if (!redisHasData) {
            // 无缓存，查询数据库，并将数据写入缓存
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.ne("id", loginUser.getId());
            List<User> userList = this.list(queryWrapper);

            String redisUserGeoKey = RedisConstant.USER_GEO_KEY;

            // 将User转换为UserVO，在进行序列化
            userVOList = userList.stream()
                    .map(user -> {
                        // 查询距离
                        Distance distance = stringRedisTemplate.opsForGeo().distance(redisUserGeoKey,
                                String.valueOf(loginUser.getId()), String.valueOf(user.getId()),
                                RedisGeoCommands.DistanceUnit.KILOMETERS);
                        Double value = distance.getValue();
                        // 创建UserVO对象并设置属性
                        UserVO userVO = new UserVO();
                        userVO.setId(user.getId());
                        userVO.setUsername(user.getUsername());
                        userVO.setUserAccount(user.getUserAccount());
                        userVO.setAvatarUrl(user.getAvatarUrl());
                        userVO.setGender(user.getGender());
                        userVO.setProfile(user.getProfile());
                        userVO.setPhone(user.getPhone());
                        userVO.setEmail(user.getEmail());
                        userVO.setUserStatus(user.getUserStatus());
                        userVO.setCreateTime(user.getCreateTime());
                        userVO.setUpdateTime(user.getUpdateTime());
                        userVO.setUserRole(user.getUserRole());
                        userVO.setTags(user.getTags());
                        if (value != null) {
                            userVO.setDistance(value); // 设置距离值
                        } else {
                            userVO.setDistance(0.0);
                        }
                        return userVO;
                    })
                    .collect(Collectors.toList());
            // 将序列化的 List 写入缓存
            List<String> userVOJsonList = userVOList.stream().map(JSONUtil::toJsonStr).collect(Collectors.toList());
            try {
                stringRedisTemplate.opsForList().rightPushAll(redisKey, userVOJsonList);
            } catch (Exception e) {
                log.error("redis set key error", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "缓存写入失败");
            }
        }
        userVOList = stringRedisTemplate.opsForList().range(redisKey, start, end)
                .stream().map(UserServiceImpl::transferToUserVO).collect(Collectors.toList());
        return userVOList;
    }

    private static UserVO transferToUserVO(String jsonStr) {
        return JSONUtil.toBean(jsonStr, UserVO.class);
    }

}




