package com.ziio.buddylink.service.blogInteractionStrategy.impl;

import com.ziio.buddylink.constant.RedisConstant;
import com.ziio.buddylink.model.request.BlogQueryRequest;
import com.ziio.buddylink.service.blogInteractionStrategy.BlogInteractionStrategy;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;

public class BlogLikedStrategy implements BlogInteractionStrategy {

    /**
     * 查询自己点赞的博客
     *
     * @return
     */
    @Override
    public Set<String> interactionMethod(BlogQueryRequest blogQueryRequest, StringRedisTemplate stringRedisTemplate, long userId) {
        return stringRedisTemplate.opsForSet().members(RedisConstant.REDIS_USER_LIKE_BLOG_KEY + userId);
    }
}