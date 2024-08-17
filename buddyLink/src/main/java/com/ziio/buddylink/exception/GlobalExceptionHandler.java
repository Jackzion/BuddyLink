package com.ziio.buddylink.exception;

import com.ziio.buddylink.common.BaseResponse;
import com.ziio.buddylink.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    // 只暴露 业务错误信息
    @ExceptionHandler(BusinessException.class)
    public BaseResponse businessExceptionHandler(BusinessException e){
        log.error("businsessException"+ e.getMessage(), e);
        return ResultUtils.error(e.getCode(),e.getMessage(),e.getDescription());
    }

    // 不暴露 runtime exception 信息
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse runtimeExceptionHandler(BusinessException e){
        log.error("RuntimeException"+ e.getMessage(), e);
        return ResultUtils.error(e.getCode(),e.getMessage(),"");
    }
}
