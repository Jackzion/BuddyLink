package com.ziio.buddylink.model.request;

import lombok.Data;

import java.util.Date;

@Data
public class TeamUpdateRequest {
    /**
     * id
     */
    private Long id;
    /**
     * 队伍名称
     */
    private String teamName;

    /**
     *  描述
     */
    private String description;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 队伍状态 - 0 - 公开， 1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 队伍密码
     */
    private String password;
}
