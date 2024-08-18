package com.ziio.buddylink.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class LikeRequest implements Serializable {

    boolean isLiked;

    long blogId;

    public boolean getIsLiked() {
        return isLiked;
    }
}