package com.ziio.buddylink.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class WebSocketVO implements Serializable {

    private static final long serialVersionUID = 4696612253320170315L;

    /**
     * 用户 id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String username;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String avatarUrl;

}

