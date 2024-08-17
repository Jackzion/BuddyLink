package com.ziio.buddylink.service;

import com.ziio.buddylink.common.ErrorCode;
import com.ziio.buddylink.exception.BusinessException;
import com.ziio.buddylink.model.VO.UserVO;
import com.ziio.buddylink.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ziio.buddylink.model.request.UserEditRequest;
import com.ziio.buddylink.model.request.UserRegisterRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Ziio
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2024-08-16 15:40:56
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param request
     * @param userRegisterRequest
     * @return
     */
    long userRegister(HttpServletRequest request, UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录
     * @param userAccount
     * @param userPassword
     * @param request
     * @return
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    int userLogout(HttpServletRequest request);

    /**
     * 用户脱敏
     * @param originUser 用户信息
     * @return 用户简略信息
     */
    User getSafetyUser(User originUser);

    /**
     * 从 session 获取用户信息
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 从session判断是否为管理员
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 从User判断是否为管理员
     * @param loginUser
     * @return
     */
    boolean isAdmin(User loginUser);

    /**
     * 更新用户信息
     * @param userEditRequest
     * @param loginUser
     * @return
     */
    int updateUser(UserEditRequest userEditRequest, User loginUser);

    List<UserVO> recommendUsers(long pageSize, long pageNum, HttpServletRequest request);
}
