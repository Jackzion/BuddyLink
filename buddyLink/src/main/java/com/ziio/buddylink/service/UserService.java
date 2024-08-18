package com.ziio.buddylink.service;

import com.ziio.buddylink.model.vo.SignInInfoVO;
import com.ziio.buddylink.model.vo.UserInfoVO;
import com.ziio.buddylink.model.vo.UserVO;
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

    /**
     * 分页获取用户
     * @param pageSize
     * @param pageNum
     * @param request
     * @return
     */
    List<UserVO> recommendUsers(long pageSize, long pageNum, HttpServletRequest request);

    /**
     * 按 tags 推荐指定数量的用户
     * @param num 返回个数
     * @param loginUser 用户
     * @return
     */
    List<UserVO> matchUsers(long num, User loginUser);

    /**
     * 查询附件用户
     * @param radius 半径距离
     * @param loginUser 用户
     * @return
     */
    List<UserVO> searchNearby(int radius, User loginUser);

    /**
     * 查询用户博客总数量
     * @param userId
     * @return
     */
    long hasBlogCount(long userId);

    /**
     * 查询用户粉丝数量
     * @param userId
     * @return
     */
    long hasFollowerCount(long userId);

    /**
     * 根据 session 查询用户 blogs and stars 信息
     * @param request
     * @return
     */
    UserInfoVO getUserInfo(HttpServletRequest request);

    /**
     * 获取数据库 score 排名前十的 users
     * @return
     */
    List<User> getUsersScoreRank();


    boolean userSigIn(HttpServletRequest request);

    SignInInfoVO getSignedDates(HttpServletRequest request);
}
