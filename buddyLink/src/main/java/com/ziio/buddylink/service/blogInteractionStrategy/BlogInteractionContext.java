package com.ziio.buddylink.service.blogInteractionStrategy;

import com.ziio.buddylink.model.request.BlogQueryRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;

@Data
@AllArgsConstructor
public class BlogInteractionContext {

    private BlogInteractionStrategy blogInteractionStrategy;

    public Set<String> blogInteractionMethod(BlogQueryRequest blogQueryRequest, StringRedisTemplate stringRedisTemplate, long userId) {
        return this.blogInteractionStrategy.interactionMethod(blogQueryRequest, stringRedisTemplate, userId);
    }
}
