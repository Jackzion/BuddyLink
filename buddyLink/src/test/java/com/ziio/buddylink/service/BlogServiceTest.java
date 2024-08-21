package com.ziio.buddylink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ziio.buddylink.model.domain.Blog;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.redisson.api.annotation.RRemoteAsync;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class BlogServiceTest {

    @Autowired
    BlogService blogService;

    @Test
    public void test() {
        String title = "";
        Long userId = 4L;
        QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.eq("userId", userId);
        List<Blog> blogList = blogService.page(new Page<>(1, 3), queryWrapper).getRecords();
        log.info("blogList:{}", blogList);
    }
}