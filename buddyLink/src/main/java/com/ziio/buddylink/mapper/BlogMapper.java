package com.ziio.buddylink.mapper;

import com.ziio.buddylink.model.domain.Blog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ziio.buddylink.model.domain.Follow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.elasticsearch.annotations.Query;

import java.util.List;

/**
* @author Ziio
* @description 针对表【blog(博客表)】的数据库操作Mapper
* @createDate 2024-08-18 17:11:33
* @Entity com.ziio.buddylink.model.domain.Blog
*/
public interface BlogMapper extends BaseMapper<Blog> {

    /**
     * 模糊分页查询
     * @param start
     * @param end
     * @param title
     * @return
     */
    List<Blog> selectBlogByPage(@Param("start") long start, @Param("end") long end, @Param("title") String title);


}




