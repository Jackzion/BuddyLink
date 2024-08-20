package com.ziio.buddylink.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class TeamJoinRequest implements Serializable {
    /**
     * 密码
     */
    private String password;
    /**
     * 队伍的id
     */
    private Long teamId;
}