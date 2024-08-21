package com.ziio.buddylink.service.blogInteractionStrategy.impl;

import com.ziio.buddylink.constant.RedisConstant;
import com.ziio.buddylink.model.request.BlogQueryRequest;
import com.ziio.buddylink.service.blogInteractionStrategy.BlogInteractionStrategy;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;

public class BlogViewedStrategy implements BlogInteractionStrategy {
    /**
     * 查询自己浏览的博客
     * @return
     */
    // todo : request 是否有用 ？
    @Override
    public Set<String> interactionMethod(BlogQueryRequest blogQueryRequest, StringRedisTemplate stringRedisTemplate, long userId) {
        return stringRedisTemplate.opsForZSet().range(RedisConstant.REDIS_USER_VIEW_BLOG_KEY + userId, 0, -1);
    }
}
