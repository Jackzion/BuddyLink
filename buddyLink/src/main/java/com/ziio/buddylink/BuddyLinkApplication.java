package com.ziio.buddylink;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.ziio.buddylink.mapper")//扫描mapper包下的文件
@EnableScheduling
public class BuddyLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(BuddyLinkApplication.class, args);
    }

}
