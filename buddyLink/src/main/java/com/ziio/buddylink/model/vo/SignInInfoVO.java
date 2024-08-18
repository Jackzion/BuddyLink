package com.ziio.buddylink.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class SignInInfoVO implements Serializable {

    private List<Date> signedInDates; // 签到的具体天数

    private int signedInDayNum; // 已签到天数

    private boolean isSignedIn; // 今天天是否签到

    public boolean getIsSignedIn() {
        return isSignedIn;
    }

    public void setIsSignedIn(boolean signedIn) {
        isSignedIn = signedIn;
    }
}
