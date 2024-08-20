package com.ziio.buddylink.model.request;

import java.io.Serializable;

public class FollowQueryRequest implements Serializable {
    private int type; // 查询类型（0 查自己粉丝，1 查自己关注，2 查别人粉丝，3 查别人关注）

    private long userId;
}
