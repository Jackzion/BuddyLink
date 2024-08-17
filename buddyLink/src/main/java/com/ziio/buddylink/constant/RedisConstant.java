package com.ziio.buddylink.constant;

/**
 * redis KEY 常量
 */
public interface RedisConstant {
    String SYSTEM_ID = "buddy:team:join";
    String USER_JOIN_TEAM = "buddy:team:join:";
    String USER_GEO_KEY = "buddy:user:geo";
    String USER_ADD_KEY = "buddy:user:add";
    String USER_RECOMMEND_KEY = "buddy:user:recommend";

    String REDIS_LIMITER_REGISTER = "buddy:limiter:register:";
    String REDIS_BLOG_STAR_KEY = "buddy:blog:star:";
    String REDIS_BLOG_LIKE_KEY = "buddy:blog:like:";
    String REDIS_BLOG_VIEW_KEY = "buddy:blog:view:";
    String REDIS_USER_LIKE_BLOG_KEY = "buddy:user:like:blog:";
    String REDIS_USER_VIEW_BLOG_KEY = "buddy:user:view:blog:";
    String REDIS_USER_STAR_BLOG_KEY = "buddy:user:star:blog:";
    String USER_SIGNIN_KEY = "buddy:user:signin:";
    String BLOG_BLOOM_FILTER_KEY = "buddy:blog:bloomfilter";
    String USER_BLOOM_FILTER_KEY = "buddy:user:bloomfilter";
    String BLOG_COVER_IMAGE_UPLOAD_KEY = "buddy:blog:cover:image:upload:";
    String BLOG_IMAGE_UPLOAD_KEY = "buddy:blog:image:upload:";
    /**
     * 用户推荐缓存
     */
    /**
     * 最小缓存随机时间
     */
    public static final int MINIMUM_CACHE_RANDOM_TIME = 2;
    /**
     * 最大缓存随机时间
     */
    public static final int MAXIMUM_CACHE_RANDOM_TIME = 3;
    /**
     * 缓存时间偏移
     */
    public static final int CACHE_TIME_OFFSET = 10;

}
