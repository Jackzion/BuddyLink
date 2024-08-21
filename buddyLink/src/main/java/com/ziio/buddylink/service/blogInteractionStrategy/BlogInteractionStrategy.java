package com.ziio.buddylink.service.blogInteractionStrategy;

import com.ziio.buddylink.model.request.BlogQueryRequest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;

/**
 * 策略模式
 */
public interface BlogInteractionStrategy {

    Set<String> interactionMethod(BlogQueryRequest blogQueryRequest, StringRedisTemplate stringRedisTemplate, long userId);

}
