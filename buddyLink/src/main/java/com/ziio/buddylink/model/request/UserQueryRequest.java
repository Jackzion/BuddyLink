package com.ziio.buddylink.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserQueryRequest extends PageRequest implements Serializable {
    // searchText for userName , tags or profile
    private String searchText;

    private Long userId;
}
