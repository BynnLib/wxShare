package com.wechat.share;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.wechat.share.mapper")
public class wechatShareApplication {
    public static void main(String[] args) {
        SpringApplication.run(wechatShareApplication.class, args);
    }
}
