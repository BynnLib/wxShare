package com.wechat.share.utils;


import net.sf.json.JSONObject;

public class AdvancedUtil {
    public static String getTicket() {
        // 获取网页授权凭证
        JSONObject jsonObject = JSONObject.fromObject(HttpRequest.sendGet("http://localhost:3000/ticket", ""));
        String jsapi_ticket = null;
        if (null != jsonObject) {
            jsapi_ticket = jsonObject.getString("ticket");
        }
        return jsapi_ticket;
    }
}
