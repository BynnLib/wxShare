package com.wechat.share.service;

import java.util.HashMap;
import java.util.Map;

public interface ShareService {
    String shareGetJsapiTicket();

    Map<String, Object> shareReturnMap(String url, String ticket);
}
