package com.ziio.buddylink.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应封装
 * @param <T> 返回的数据类型
 */
@Data
public class BaseResponse<T> implements Serializable {
    private int code;//错误码

    private T data;//前端传来的数据

    private String message;// 简略信息

    private String description;// 详细信息（报错）

    public BaseResponse(int code, T data, String message,String description) {
        this.code = code;
        this.data = data;
        this.message = message;
        this.description=description;
    }

    public BaseResponse(int code, T data, String message) {
        this(code,data,message,"");
    }
    public BaseResponse(int code, T data) {
        this(code, data, "","");
    }

    public BaseResponse(ErrorCode errorCode){
        this(errorCode.getCode(),null,errorCode.getMessage(), errorCode.getDescription());
    }

}
