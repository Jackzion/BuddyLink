package com.ziio.buddylink.model.request;

import lombok.Data;

@Data
public class StarRequest {
    private boolean isStarred;

    private long blogId;

    public boolean getIsStarred() {
        return isStarred;
    }
}