package com.ziio.buddylink.model.vo;

import lombok.Data;

/**
 * blog 页面作者视图
 */
@Data
public class BlogUserVO {
    /**
     * id
     */
    private long id;

    /**
     * 用户昵称
     */
    private String username;

    /**
     * 用户头像
     */
    private String avatarUrl;

    /**
     * 用户简介
     */
    private String profile;

    /**
     * 标签
     */
    private String tags;
    /**
     * 粉丝数
     */
    private Long fanNum;

    /**
     * 博客数
     */
    private Long blogNum;

    /**
     * 浏览量
     */
    private Long blogViewNum;

    /**
     * 是否关注
     */
    private boolean isFollowed;
}
