package io.xiaoyi311.unifiedpass.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.xiaoyi311.unifiedpass.OtherUtil;
import io.xiaoyi311.unifiedpass.dao.OAuthAppRepository;
import io.xiaoyi311.unifiedpass.entity.OAuthApp;
import io.xiaoyi311.unifiedpass.entity.ResponseData;
import io.xiaoyi311.unifiedpass.entity.User;
import io.xiaoyi311.unifiedpass.entity.UserError;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * OAuth 服务
 * @author xiaoyi311
 */
@Service
@Slf4j
public class OAuthService {
    @Value("${oauth.key}")
    private String key;

    final int TIMEOUT = 60 * 60;
    OAuthAppRepository oAuthAppRepository;
    RedisTemplate<String, String> redisTemplate;

    public OAuthService(OAuthAppRepository oAuthAppRepository, RedisTemplate<String, String> redisTemplate){
        this.oAuthAppRepository = oAuthAppRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 新建 OAuth 应用程序
     * @param callback 回调地址
     * @param name 应用名称
     */
    public void create(
            String callback,
            String name
    ){
        if(callback.isEmpty() || name.isEmpty()){
            throw new UserError("lang:oauth.empty");
        }

        OAuthApp app = new OAuthApp();
        app.setName(name);
        app.setCallback(callback);

        oAuthAppRepository.save(app);
    }

    /**
     * 删除 OAuth 应用程序
     * @param clientId Client ID
     */
    public void delete(String clientId){
        oAuthAppRepository.deleteById(clientId);
    }

    /**
     * 更改 OAuth 应用程序
     * @param info OAuth 信息
     */
    public void set(JSONObject info){
        String clientId = (String) info.getOrDefault("client_id", "");
        if(clientId.isEmpty()){
            throw new UserError("lang:oauth.empty");
        }

        Optional<OAuthApp> oApp = oAuthAppRepository.findById(clientId);
        if(oApp.isEmpty()){
            throw new UserError("lang:oauth.empty");
        }

        OAuthApp app = oApp.get();
        app.setName((String) info.getOrDefault("name", app.getName()));
        app.setCallback((String) info.getOrDefault("callback", app.getCallback()));
        app.setDes((String) info.getOrDefault("des", app.getDes()));
        app.setAuthMode((String) info.getOrDefault("auth_mode", app.getAuthMode()));
        app.setPermission((String) info.getOrDefault("scope", app.getPermission()));
        app.setWebsite((String) info.getOrDefault("website", app.getWebsite()));

        oAuthAppRepository.save(app);
    }

    /**
     * 获取 OAuth 应用程序信息
     * @param clientId Client ID
     */
    public JSONObject getInfo(String clientId){
        if(clientId.isEmpty()){
            throw new UserError("lang:oauth.empty");
        }

        Optional<OAuthApp> oApp = oAuthAppRepository.findById(clientId);
        if(oApp.isEmpty()){
            throw new UserError("lang:oauth.empty");
        }

        return oApp.get().getInfo();
    }

    /**
     * 获取所有 OAuth Application 信息
     * @return 所有 OAuth Application 信息
     */
    public JSONArray list(){
        JSONArray array = new JSONArray();
        array.addAll(oAuthAppRepository.findAll());
        return array;
    }

    /**
     * 接受授权
     * @param user 用户
     * @param clientId Client ID
     * @param redirectUri 重定向地址
     * @param state 状态
     * @param scope 权限
     */
    public String accept(
            User user,
            String clientId,
            String redirectUri,
            String state,
            String scope
    ){
        OAuthApp oAuthApp = oAuthAppRepository.findById(clientId).orElse(null);
        if(oAuthApp == null){
            throw new UserError("lang:oauth.empty");
        }

        if(!Arrays.asList(oAuthApp.getPermission().split(" ")).contains(scope)){
            throw new UserError("lang:oauth.permission_not_allowed");
        }

        String code = OtherUtil.randomString(6).toUpperCase(Locale.ROOT);
        redisTemplate.opsForValue().set("oauthCode:" + code, clientId + "|" + redirectUri + "|" + user.getId() + "|" + scope, Duration.ofMinutes(10));

        return redirectUri + "?"
                +"&state="+state
                +"&code="+code;
    }

    /**
     * 生成令牌
     * @param code 授权码
     * @param clientId Client ID
     * @param clientSecret Client Secret
     * @param redirectUri 重定向地址
     * @param response 响应
     * @return 令牌
     */
    public JSONObject token(
            String code,
            String clientId,
            String clientSecret,
            String redirectUri,
            HttpServletResponse response
    ){
        String data = redisTemplate.opsForValue().getAndDelete("oauthCode:" + code);
        if(data == null){
            throw new UserError("Invalid Code!");
        }

        String[] datas = data.split("\\|");
        if(!Objects.equals(datas[0], clientId)){
            throw new UserError("Invalid Client ID!");
        }

        OAuthApp app = oAuthAppRepository.findById(clientId).orElse(null);
        if(app != null && !Objects.equals(app.getClientSecret(), clientSecret)){
            throw new UserError("Invalid Client Secret!");
        }

        if(!Objects.equals(datas[1], redirectUri)){
            throw new UserError("Invalid Redirect URI!");
        }

        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        String accessToken = OtherUtil.randomString(32);
        String refreshToken = OtherUtil.aesEncrypt(
                System.currentTimeMillis() + "|" + clientId + "|" + datas[2] + "|" + datas[3],
                key
        );

        redisTemplate.opsForValue().set("oauthToken:" + accessToken, datas[2] + "|" + datas[3], Duration.ofSeconds(TIMEOUT));

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("access_token", accessToken);
        jsonObject.put("token_type", "Bearer");
        jsonObject.put("expires_in", TIMEOUT);
        jsonObject.put("refresh_token", refreshToken);

        return jsonObject;
    }

    /**
     * 刷新令牌
     * @param refreshToken 刷新令牌
     * @param clientId Client ID
     * @param response 响应
     * @return 令牌
     */
    public JSONObject refreshToken(
            String refreshToken,
            String clientId,
            HttpServletResponse response
    ){
        String data = OtherUtil.aesDecrypt(refreshToken, key);
        if("=== ERROR DECODE AES ===".equals(data)){
            throw new UserError("Invalid Refresh Token!");
        }

        String[] datas = data.split("\\|");
        if(Long.parseLong(datas[0]) + TIMEOUT * 1000 < System.currentTimeMillis()) {
            throw new UserError("Invalid Refresh Token!");
        }

        if(!Objects.equals(datas[1], clientId)){
            throw new UserError("Invalid Client ID!");
        }

        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        String accessToken = OtherUtil.randomString(32);
        String newRefreshToken = OtherUtil.aesEncrypt(
                System.currentTimeMillis() + "|" + clientId + "|" + datas[2] + "|" + datas[3],
                key
        );

        redisTemplate.opsForValue().set("oauthToken:" + accessToken, datas[2] + "|" + datas[3], Duration.ofSeconds(TIMEOUT));

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("access_token", accessToken);
        jsonObject.put("token_type", "Bearer");
        jsonObject.put("expires_in", TIMEOUT);
        jsonObject.put("refresh_token", newRefreshToken);

        return jsonObject;
    }

    /**
     * 获取令牌信息
     * @param accessToken 令牌
     * @return 令牌信息: 用户 ID, 权限
     */
    public String[] getTokenInfo(String accessToken){
        String data = redisTemplate.opsForValue().get("oauthToken:" + accessToken);
        if(data == null){
            return null;
        }

        return data.split("\\|");
    }
}
