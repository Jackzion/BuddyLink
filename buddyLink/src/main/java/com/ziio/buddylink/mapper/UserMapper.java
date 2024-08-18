package com.ziio.buddylink.mapper;

import com.ziio.buddylink.model.domain.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author Ziio
* @description 针对表【user(用户)】的数据库操作Mapper
* @createDate 2024-08-16 15:40:56
* @Entity generator.domain.User
*/
public interface UserMapper extends BaseMapper<User> {

    long hasBlogCount(long userId);

    long hasFollowerCount(long userId);

    @Select("select * from user where score >= 0 order by score desc")
    List<User> selectUserTop10Score();
}




