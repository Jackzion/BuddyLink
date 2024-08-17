package com.ziio.buddylink.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID=3191241716373120793L;

    private String userAccount;

    private String userPassword;

}

