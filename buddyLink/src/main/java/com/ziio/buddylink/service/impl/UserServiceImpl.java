package com.ziio.buddylink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.ziio.buddylink.common.ErrorCode;
import com.ziio.buddylink.constant.RedisConstant;
import com.ziio.buddylink.exception.BusinessException;
import com.ziio.buddylink.manager.RedisBloomFilter;
import com.ziio.buddylink.manager.RedisLimiterManager;
import com.ziio.buddylink.model.domain.User;
import com.ziio.buddylink.model.request.UserRegisterRequest;
import com.ziio.buddylink.service.UserService;
import com.ziio.buddylink.mapper.UserMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * 用户脱敏
     * @param originUser 用户信息
     * @return 用户简略信息
     */
    private User getSafetyUser(User originUser) {
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
}




