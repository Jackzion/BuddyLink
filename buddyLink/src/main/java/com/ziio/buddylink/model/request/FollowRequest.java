package com.ziio.buddylink.model.request;

import lombok.Data;

@Data
public class FollowRequest {

    private boolean isFollowed;

    private long userId;

    public boolean getIsFollowed() {
        return this.isFollowed;
    }

    public void setIsFollowed(boolean followed) {
        this.isFollowed = followed;
    }

}