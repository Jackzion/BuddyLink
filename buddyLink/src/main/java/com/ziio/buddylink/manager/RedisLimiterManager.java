package com.ziio.buddylink.manager;

import com.ziio.buddylink.common.ErrorCode;
import com.ziio.buddylink.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class RedisLimiterManager {

    @Resource
    private RedissonClient redissonClient;

    // 限流器
    public void doRateLimiter(String key, long time, long frequency) {
        // 获取业务限流器
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        // 设置限流窗口和窗口内次数
        rateLimiter.trySetRate(RateType.OVERALL, time, frequency, RateIntervalUnit.MINUTES);
        boolean b = rateLimiter.tryAcquire();
        if (!b) {
            log.error(key + "请求次数过多，请稍后重试");
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
    }

}
