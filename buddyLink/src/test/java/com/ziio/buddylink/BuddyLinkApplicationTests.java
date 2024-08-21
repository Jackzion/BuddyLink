package com.ziio.buddylink;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ziio.buddylink.model.domain.Blog;
import com.ziio.buddylink.service.BlogService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest
@Slf4j
class BuddyLinkApplicationTests {

    @Autowired
    private BlogService blogService;

    @Test
    void contextLoads() {
        System.out.println("hello world");
    }

}
