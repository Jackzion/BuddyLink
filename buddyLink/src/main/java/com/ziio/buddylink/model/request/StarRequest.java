package com.ziio.buddylink.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class StarRequest implements Serializable {
    private boolean isStarred;

    private long blogId;

    public boolean getIsStarred() {
        return isStarred;
    }
}