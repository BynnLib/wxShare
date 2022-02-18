package com.wechat.share.controller;

import cn.hutool.json.JSONUtil;
import com.wechat.share.service.ShareService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * 参考 https://www.cnblogs.com/pxblog/p/12881454.html
 */

@Controller
@Slf4j
@CrossOrigin
public class ShareController {

    @Autowired
    private ShareService shareService;
    /**
     * 这是跳转到分享的页面
     * @return
     */
    @RequestMapping(value = "/to_detail")
    public String share(){
        return "share";
    }

    /**
     * 获取微信分享配置的请求  方法只写了主要方法，需要根据自己的要求 完善代码
     * @param url 前台传过来的当前页面的请求地址
     * @return
     */
    @RequestMapping(value = "/get_wx_config")
    @ResponseBody
    public String share(String url){
        String ticket = shareService.shareGetJsapiTicket();
        Map map = shareService.shareReturnMap(url, ticket);
        //这里使用了hutool工具将map转为String
        String json = JSONUtil.toJsonStr(map);

        return json;
    }

}
