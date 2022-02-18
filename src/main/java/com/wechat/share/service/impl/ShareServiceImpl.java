package com.wechat.share.service.impl;

import com.wechat.share.service.ShareService;
import com.wechat.share.utils.wx.RandomStr;
import com.wechat.share.utils.wx.Sha1;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ShareServiceImpl implements ShareService {
    /**
     * 微信公众号的appid
     */
    @Value("${wx.open.app_id}")
    private String appid;

    /**
     * 微信公众号的appSecret
     */
    @Value("${wx.open.app_secret}")
    private String secret;

    /**
     * 官方文档：https://developers.weixin.qq.com/doc/offiaccount/OA_Web_Apps/JS-SDK.html#62
     *
     * 获取jsapi_ticket
     *
     *  生成签名之前必须先了解一下jsapi_ticket，jsapi_ticket是公众号用于调用微信JS接口的临时票据。
     *  正常情况下，jsapi_ticket的有效期为7200秒，通过access_token来获取。
     *  由于获取jsapi_ticket的api调用次数非常有限，频繁刷新jsapi_ticket会导致api调用受限，影响自身业务，开发者必须在自己的服务全局缓存jsapi_ticket 。
     *
     * 参考以下文档获取access_token（有效期7200秒，开发者必须在自己的服务全局缓存access_token）：
     * https://developers.weixin.qq.com/doc/offiaccount/Basic_Information/Get_access_token.html
     *
     * 用第一步拿到的access_token 采用http GET方式请求获得jsapi_ticket（有效期7200秒，开发者必须在自己的服务全局缓存jsapi_ticket）：
     * https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=ACCESS_TOKEN&type=jsapi
     */
    @Override
    public String shareGetJsapiTicket() {
        String tokenUrl = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential";
        tokenUrl = tokenUrl + "&appid=" + appid + "&secret=" + secret;
        JSONObject tokenJson = new JSONObject();
        tokenJson = getUrlResponse(tokenUrl);
        log.info(tokenJson.toString());
        //{"access_token":"53_-aUhRDJ2DORtiyiBWGU3wNBeViRVIpq3v0BH-Yd_Bff4VtzWJrRC7q6t-OA5YY0A8vJRKvNEpLD4UWTEyxgkBGLsn8twmU-EBzR97X5cccYY9t1bo7DZGYUP3tkfKp1_kEiTE1f_L06UP3OuYWEdAAAIGO","expires_in":7200}
        log.info("tokenJson:"+tokenJson.toString());
        //tokenJson:{"access_token":"53_-aUhRDJ2DORtiyiBWGU3wNBeViRVIpq3v0BH-Yd_Bff4VtzWJrRC7q6t-OA5YY0A8vJRKvNEpLD4UWTEyxgkBGLsn8twmU-EBzR97X5cccYY9t1bo7DZGYUP3tkfKp1_kEiTE1f_L06UP3OuYWEdAAAIGO","expires_in":7200}
        String token="";
        try {
            /**
             * TODO:access_token应该存入缓存，设置有效期为7200s
             */
            token = tokenJson.getString("access_token");
        } catch (JSONException e) {
            e.printStackTrace();
            log.error("报错了");
            return null;
        }

        String jsapiTicketUrl="https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=ACCESS_TOKEN&type=jsapi";
        JSONObject jsapiTickeJson=new JSONObject();
        log.info("getJsapiTicket：获取token："+token);
        //getJsapiTicket：获取token：53_-aUhRDJ2DORtiyiBWGU3wNBeViRVIpq3v0BH-Yd_Bff4VtzWJrRC7q6t-OA5YY0A8vJRKvNEpLD4UWTEyxgkBGLsn8twmU-EBzR97X5cccYY9t1bo7DZGYUP3tkfKp1_kEiTE1f_L06UP3OuYWEdAAAIGO
        if(StringUtils.isNotBlank(token)){
            jsapiTicketUrl = jsapiTicketUrl.replace("ACCESS_TOKEN",token);
            jsapiTickeJson=getUrlResponse(jsapiTicketUrl);
            log.info("tokenJson:"+jsapiTickeJson.toString());
            //签名str：jsapi_ticket=O3SMpm8bG7kJnF36aXbe8wMwjgr84lYDTsRZ8sNoECmqkoljMN7hDcdoflMvBuRLKCX7m3mC2vJYqgIDZHTeFQ&noncestr=wsV0GNj9I3MPRvHM&timestamp=1643472635&url=http://cxyabc.vaiwan.com/to_detail
            try {
                return (String) jsapiTickeJson.get("ticket");
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }else{
            return null;
        }
    }

    @Override
    public Map<String, Object> shareReturnMap(String url, String ticket) {
        long timestamp = System.currentTimeMillis() / 1000;
        String noncestr = RandomStr.createRandomString(16);
        String str = "jsapi_ticket=" + ticket + "&noncestr=" + noncestr + "&timestamp=" + timestamp + "&url=" + url;
        log.info("签名str：" + str);
        String signature = Sha1.encode(str);

        //这只是简单写法，没有做错误判断
        Map map=new HashMap();
//         * 可以返回自定义内容
        map.put("backTitle", "中共北京市委 北京市人民政府印发《关于促进中医药传承创新发展的实施方案》的通知");
//        map.put("backLink", requestUrl);
        map.put("backImgUrl", "http://wx.qlogo.cn/mmopen/ciaIftfPzwlo0coPuwwLS5Fw9UwGMlxY2ziaWpqXzevJI8dKeDvk4n3NxtZS4D8dNHSYUhbiaA6IIGnFsiagEbRlaExselicC3pEA/64");
        map.put("backDesc", "为贯彻落实《中共中央、国务院关于促进中医药传承创新发展的意见》精神，结合本市实际，制定如下实施方案。");
        map.put("appId",appid);
        map.put("timestamp",timestamp);
        map.put("nonceStr",noncestr);
        map.put("signature",signature);
//        map.put("appId",appid);
//        map.put("timestamp","1644310609");
//        map.put("nonceStr","EcGMAEWoO07ZvfiD");
//        map.put("signature","be03f6100ad2676c49cb5b5d1f89986688c432c2");

        return map;
    }


    /**
     * 以下为工具函数
     */
    private JSONObject getUrlResponse(String url){
        CharsetHandler handler = new CharsetHandler("UTF-8");
        try {
            HttpGet httpget = new HttpGet(new URI(url));
            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
            //HttpClient
            CloseableHttpClient client = httpClientBuilder.build();
            client = (CloseableHttpClient) wrapClient(client);
            return new JSONObject(client.execute(httpget, handler));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private static HttpClient wrapClient(HttpClient base) {
        try {
            SSLContext ctx = SSLContext.getInstance("TLSv1");
            X509TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] xcs,
                                               String string) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] xcs,
                                               String string) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            ctx.init(null, new TrustManager[] { tm }, null);
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(ctx, new String[] { "TLSv1" }, null,
                    SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
            CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
            return httpclient;

        } catch (Exception ex) {
            return null;
        }
    }

    private class CharsetHandler implements ResponseHandler<String> {
        private String charset;

        public CharsetHandler(String charset) {
            this.charset = charset;
        }

        public String handleResponse(HttpResponse response)
                throws ClientProtocolException, IOException {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() >= 300) {
                throw new HttpResponseException(statusLine.getStatusCode(),
                        statusLine.getReasonPhrase());
            }
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                if (!StringUtils.isBlank(charset)) {
                    return EntityUtils.toString(entity, charset);
                } else {
                    return EntityUtils.toString(entity);
                }
            } else {
                return null;
            }
        }
    }
}
