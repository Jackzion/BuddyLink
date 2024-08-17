package com.ziio.buddylink.service;

import com.ziio.buddylink.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ziio.buddylink.model.request.UserRegisterRequest;

import javax.servlet.http.HttpServletRequest;

/**
* @author Ziio
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2024-08-16 15:40:56
*/
public interface UserService extends IService<User> {

    long userRegister(HttpServletRequest request, UserRegisterRequest userRegisterRequest);

    User userLogin(String userAccount, String userPassword, HttpServletRequest request);
}
