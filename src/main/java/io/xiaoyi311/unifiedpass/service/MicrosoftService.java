package io.xiaoyi311.unifiedpass.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.xiaoyi311.unifiedpass.OtherUtil;
import io.xiaoyi311.unifiedpass.entity.UserError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 微软服务
 * @author xiaoyi311
 */
@Service
@Slf4j
public class MicrosoftService {
    @Autowired
    RedisTemplate<String, String> redisTemplate;

    static final long VERIFY_CHECK = 5 * 1000;

    Map<String, String> verifying = new ConcurrentHashMap<>();

    /**
     * 生成代码
     * @return 代码
     */
    public String spawnCode(){
        Map<String, String> args = new HashMap<>();
        //a3728d6-27a3-4180-99bb-479895b8f88e 3a9b7dbf-4ea2-45ad-9b9a-d211275205ed
        args.put("client_id", "3a9b7dbf-4ea2-45ad-9b9a-d211275205ed");
        args.put("scope", "XboxLive.signin");

        JSONObject result = OtherUtil.postArgs("https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode", args);
        if(result == null){
            throw new UserError("lang:microsoft_error");
        }

        log.info(result.toJSONString());
        verifying.put(result.getString("user_code"), result.getString("device_code"));
        return result.getString("user_code");
    }

    /**
     * 验证代码
     * @param code 代码
     * @return 结果
     */
    public String checkCode(String code){
        if(verifying.containsKey(code)){
            return "VERIFYING";
        }

        String data = redisTemplate.opsForValue().get("miCode:" + code);
        if(data == null){
            return "UNKNOWN";
        }

        return data;
    }

    /**
     * 获取 Minecraft 正版 UUID
     * @param miAccess 微软 AccessToken
     * @return UUID
     */
    public String getMinecraftUuid(String miAccess){
        try{
            //Xbox Live Auth
            JSONObject xboxLive = OtherUtil.postJson(
                    "https://user.auth.xboxlive.com/user/authenticate",
                    "{\"Properties\": {\"AuthMethod\": \"RPS\",\"SiteName\": \"user.auth.xboxlive.com\",\"RpsTicket\": \"d=<" + miAccess + ">\"},\"RelyingParty\": \"http://auth.xboxlive.com\",\"TokenType\": \"JWT\"}"
            );
            if(xboxLive == null){
                log.info("xboxLive Fail");
                return null;
            }

            String xblToken = xboxLive.getString("Token");
            if(xblToken == null){
                log.info("xblToken Fail");
                return null;
            }

            //XSTS Token For Minecraft
            JSONObject xsts = OtherUtil.postJson(
                    "https://xsts.auth.xboxlive.com/xsts/authorize",
                    "{\"Properties\": {\"SandboxId\": \"RETAIL\",\"UserTokens\": [\"" + xblToken + "\"]},\"RelyingParty\": \"rp://api.minecraftservices.com/\",\"TokenType\": \"JWT\"}"
            );
            if(xsts == null){
                log.info("xsts Fail");
                return null;
            }

            String xstsToken = xsts.getString("Token");
            String userHash = xsts.getJSONObject("DisplayClaims").getJSONArray("xui").getJSONObject(0).getString("uhs");
            if(xstsToken == null || userHash == null){
                log.info("xstsToken Fail");
                return null;
            }

            //Auth Minecraft
            JSONObject minecraft = OtherUtil.postJson(
                    "https://api.minecraftservices.com/authentication/login_with_xbox",
                    "{\"identityToken\": \"XBL3.0 x=" + userHash + ";" + xstsToken + "\"}"
            );
            if(minecraft == null){
                log.info("minecraft Fail");
                return null;
            }

            log.info(minecraft.toJSONString());
            String minecraftAccess = minecraft.getString("access_token");
            if(minecraftAccess == null){
                log.info("minecraftAccess Fail");
                return null;
            }

            //Get Minecraft UUID
            HttpClient cli = HttpClient.newHttpClient();
            HttpResponse<String> response;
            try {
                response = cli.send(
                        HttpRequest.newBuilder()
                                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/json")
                                .header("Authorization", "Bearer " + minecraftAccess)
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString()
                );
            } catch (IOException | InterruptedException e) {
                return null;
            }
            JSONObject minecraftUuid = JSON.parseObject(response.body());

            return minecraftUuid.getString("id");
        } catch (Exception e){
            log.info("Unknown Fail: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取验证状态
     */
    //@Scheduled(fixedDelay = VERIFY_CHECK)
    public void cleanToken(){
        for (Map.Entry<String, String> entry : verifying.entrySet()) {
            Map<String, String> args = new HashMap<>();
            args.put("client_id", "3a9b7dbf-4ea2-45ad-9b9a-d211275205ed");
            args.put("grant_type", "urn:ietf:params:oauth:grant-type:device_code");
            args.put("device_code", entry.getValue());

            JSONObject result = OtherUtil.postArgs("https://login.microsoftonline.com/consumers/oauth2/v2.0/token", args);
            if(result == null){
                log.info("CANT GET MICROSOFT CODE STATUS!!!");
                continue;
            }

            String error = result.getString("error");
            if(error != null){
                switch (error) {
                    case "authorization_pending" -> {}
                    case "authorization_declined" -> {
                        verifying.remove(entry.getKey());
                        redisTemplate.opsForValue().set("miCode:" + entry.getKey(), "CANCEL", Duration.ofMinutes(1));
                    }
                    case "bad_verification_code" -> {
                        verifying.remove(entry.getKey());
                        redisTemplate.opsForValue().set("miCode:" + entry.getKey(), "ERROR", Duration.ofMinutes(1));
                    }
                    case "expired_token" -> {
                        verifying.remove(entry.getKey());
                        redisTemplate.opsForValue().set("miCode:" + entry.getKey(), "OUTDATED", Duration.ofMinutes(1));
                    }
                }
            }else{
                redisTemplate.opsForValue().set(
                        "miCode:" + entry.getKey(),
                        ((String) result.getOrDefault("scope", "")).contains("XboxLive.signin") ?
                                result.getString("access_token") : "REFUSE"
                        ,
                        Duration.ofMinutes(1)
                );
                verifying.remove(entry.getKey());
            }
        }
    }
}
