package com.wechat.share.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@Slf4j
@RestController
@CrossOrigin
public class IndexController {
    @RequestMapping("MP_verify_KguGBKmjDHvB2Swj.txt")
    public String wxPrivateKey(){
        //直接返回你下载的授权文件里的内容就好
        return "KguGBKmjDHvB2Swj";
    }


}
