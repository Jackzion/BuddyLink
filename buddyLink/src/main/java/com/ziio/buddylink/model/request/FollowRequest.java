package com.ziio.buddylink.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class FollowRequest implements Serializable {

    private boolean isFollowed;

    private long userId;

    public boolean getIsFollowed() {
        return this.isFollowed;
    }

    public void setIsFollowed(boolean followed) {
        this.isFollowed = followed;
    }

}