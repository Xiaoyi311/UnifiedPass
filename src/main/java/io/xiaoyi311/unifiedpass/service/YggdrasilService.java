package io.xiaoyi311.unifiedpass.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.xiaoyi311.unifiedpass.OtherUtil;
import io.xiaoyi311.unifiedpass.UnifiedPassApplication;
import io.xiaoyi311.unifiedpass.dao.ProfileRepository;
import io.xiaoyi311.unifiedpass.dao.TokenRepository;
import io.xiaoyi311.unifiedpass.entity.ServerSetting;
import io.xiaoyi311.unifiedpass.entity.User;
import io.xiaoyi311.unifiedpass.entity.UserError;
import io.xiaoyi311.unifiedpass.entity.yggdrasil.YggdrasilError;
import io.xiaoyi311.unifiedpass.entity.yggdrasil.YggdrasilProfile;
import io.xiaoyi311.unifiedpass.entity.yggdrasil.YggdrasilToken;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Yggdrasil 服务
 * @author xiaoyi311
 */
@Service
@Slf4j
@EnableScheduling
public class YggdrasilService {
    static final long TOKEN_OUT_DATE = 15 * 24 * 60 * 60 * 1000;
    static final long TOKEN_INVALID_CHECK = 30 * 60 * 1000;
    static final long USER_TOKEN_MAX = 5;

    final SettingsService settingsService;
    final TokenRepository tokenRepository;
    final ProfileRepository profileRepository;
    final UserService userService;
    final RedisTemplate<String, SessionCheck> redisTemplate;

    public YggdrasilService(SettingsService settingsService, TokenRepository tokenRepository, ProfileRepository profileRepository, UserService userService, RedisTemplate<String, SessionCheck> redisTemplate) {
        this.settingsService = settingsService;
        this.tokenRepository = tokenRepository;
        this.profileRepository = profileRepository;
        this.userService = userService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 设置指定角色的所有令牌临时失效
     * @param profile 角色 UUID
     */
    public void setTempInvalid(String profile){
        tokenRepository.getYggdrasilTokensByProfile(profile).forEach((token) -> {
            token.setTempInvalid(true);
            tokenRepository.save(token);
        });
    }

    /**
     * 保存角色
     * @param profile 角色
     */
    public void saveProfile(YggdrasilProfile profile){
        profileRepository.save(profile);
    }

    /**
     * 获取服务器元数据
     * @return 元数据
     */
    public JSONObject getInfo(){
        String domain = settingsService.get(ServerSetting.Settings.Website).value;
        JSONObject root = new JSONObject();

        JSONObject meta = new JSONObject();
        meta.put("serverName", settingsService.get(ServerSetting.Settings.ServerName).value);
        meta.put("implementationName", "BackroomsMC_UnifiedPass");
        meta.put("implementationVersion", UnifiedPassApplication.VERSION);
        meta.put("feature.username_check", true);

        JSONObject links = new JSONObject();
        links.put("homepage", domain + "/#/");
        links.put("register", domain + "/#/login");

        meta.put("links", links);

        JSONArray skinDomains = new JSONArray(List.of(domain.replace("http://", "").replace("https://", "")));
        root.put("meta", meta);
        root.put("skinDomains", skinDomains);
        root.put("signaturePublickey", settingsService.get(ServerSetting.Settings.PublicKey).value);

        return root;
    }

    /**
     * 玩家是否加入服务器
     * @param username 用户名
     * @param serverId 服务器 Id
     * @param ip IP
     * @return 角色
     */
    public YggdrasilProfile hasJoined(
            String username,
            String serverId,
            String ip
    ){
        SessionCheck check = redisTemplate.opsForValue().get("sessionCheck:" + serverId);
        if (check == null){
            return null;
        }

        if(!ip.isEmpty() && !ip.equals(check.getIp())){
            return null;
        }

        YggdrasilToken token = tokenRepository.getYggdrasilTokenByAccessToken(check.getAccessToken());
        if(token == null || token.isTempInvalid()){
            return null;
        }

        YggdrasilProfile profile = profileRepository.getYggdrasilProfileByUuid(token.getProfile());
        if(profile == null){
            return null;
        }

        if(!Objects.equals(profile.getName(), username)){
            return null;
        }

        return profile;
    }

    /**
     * 加入服务器
     * @param accessToken Access Token
     * @param selectedProfile 角色 UUID
     * @param serverId 服务器 Id
     * @param request 请求
     */
    public void join(
            String accessToken,
            String selectedProfile,
            String serverId,
            HttpServletRequest request
    ){
        String ip = OtherUtil.getRealIp(request);
        if(ip == null || serverId.isEmpty()){
            throw YggdrasilError.Errors.TOKEN_INVALID.getMsg();
        }

        YggdrasilToken token = tokenRepository.getYggdrasilTokenByAccessToken(accessToken);
        if(token == null || token.isTempInvalid()){
            throw YggdrasilError.Errors.TOKEN_INVALID.getMsg();
        }

        YggdrasilProfile profile = profileRepository.getYggdrasilProfileByUuid(token.getProfile());
        if(!Objects.equals(profile.getUuid(), selectedProfile)){
            throw YggdrasilError.Errors.PLAYER_INVALID.getMsg();
        }

        SessionCheck check = new SessionCheck(accessToken, ip);
        redisTemplate.opsForValue().set("sessionCheck:" + serverId, check, Duration.ofSeconds(30));
    }

    /**
     * 获取角色
     * @param uuid 角色 UUID
     * @return 角色
     */
    public YggdrasilProfile getProfile(
            String uuid
    ){
        return profileRepository.getYggdrasilProfileByUuid(uuid);
    }

    /**
     * 注销用户所有 Token
     * @param username 用户名
     * @param password 密码
     * @param request 请求
     */
    public void signout(
            String username,
            String password,
            HttpServletRequest request
    ){
        try{
            User user = userService.banUserCheck(username);

            if(Objects.equals(user.getPassword(), OtherUtil.sha256(password))){
                log.info("Yggdrasil Logout: " + username);

                tokenRepository.deleteAllByUser(user.getId());
            }else{
                log.info("Yggdrasil Logout Failed: " + username);
                userService.addUserBanTime(username, request);
                throw YggdrasilError.Errors.AUTH_FAILED.getMsg();
            }
        }catch (UserError e){
            throw YggdrasilError.Errors.AUTH_FAILED.getMsg();
        }
    }

    /**
     * 注销 Token
     * @param accessToken AccessToken
     */
    public void invalidate(
            String accessToken
    ){
        YggdrasilToken token = tokenRepository.getYggdrasilTokenByAccessToken(accessToken);
        if(token == null){
            throw YggdrasilError.Errors.TOKEN_INVALID.getMsg();
        }

        log.info("Yggdrasil Token Invalidate: " + token.getAccessToken());
        tokenRepository.delete(token);
    }

    /**
     * 验证 Token 是否有效
     * @param accessToken Access Token
     * @param clientToken Client Token
     * @return 是否有效
     */
    public boolean validate(
            String accessToken,
            String clientToken
    ){
        YggdrasilToken token = tokenRepository.getYggdrasilTokenByAccessToken(accessToken);
        if(token == null){
            return false;
        }

        return clientToken.isEmpty() || (token.clientToken.equals(clientToken) && !token.isTempInvalid());
    }

    /**
     * 刷新 Token
     * @param accessToken Access Token
     * @param clientToken Client Token
     * @param requestUser 是否需要用户信息
     * @param selectedProfile 绑定的用户
     * @return 响应内容
     */
    public JSONObject refresh(
            String accessToken,
            String clientToken,
            boolean requestUser,
            JSONObject selectedProfile
    ){
        YggdrasilToken token = tokenRepository.getYggdrasilTokenByAccessToken(accessToken);
        if(token == null){
            throw YggdrasilError.Errors.TOKEN_INVALID.getMsg();
        }

        if(!clientToken.isEmpty() && !token.clientToken.equals(clientToken)){
            throw YggdrasilError.Errors.TOKEN_INVALID.getMsg();
        }

        if(selectedProfile != null){
            if(token.getProfile() != null){
                throw YggdrasilError.Errors.PROFILE_BIND.getMsg();
            } else {
                //不制作绑定系统
                throw YggdrasilError.Errors.PROFILE_NOT_FOUND.getMsg();
            }
        }

        String old = token.getAccessToken();
        tokenRepository.delete(token);

        YggdrasilToken fresh = new YggdrasilToken();
        fresh.setUser(token.getUser());
        fresh.setProfile(token.getProfile());
        fresh.setClientToken(token.getClientToken());
        tokenRepository.save(fresh);

        log.info("Yggdrasil Token Refresh: " + old + " -> " + fresh.getAccessToken());

        JSONObject root = new JSONObject();
        root.put("accessToken", fresh.getAccessToken());
        root.put("clientToken", fresh.getClientToken());
        root.put("selectedProfile", profileRepository.getYggdrasilProfileByUuid(fresh.getProfile()).getJsonData("", settingsService.get(ServerSetting.Settings.Website).getValue()));
        if(requestUser){
            root.put("user", userService.userTable.getUserById(fresh.getUser()).getJsonData());
        }

        return root;
    }

    /**
     * 登录
     * @param username 用户名
     * @param password 密码
     * @param clientToken Client Token
     * @param requestUser 是否需要用户信息
     * @param request 请求
     * @return 响应内容
     */
    public JSONObject login(
            String username,
            String password,
            String clientToken,
            boolean requestUser,
            HttpServletRequest request
    ){
        try{
            User user = userService.banUserCheck(username);

            if(Objects.equals(user.getPassword(), OtherUtil.sha256(password))){
                if(tokenRepository.countByUser(user.getId()) >= USER_TOKEN_MAX){
                    tokenRepository.removeByUserAndTimeMin(user.getId());
                }

                YggdrasilToken token = new YggdrasilToken();
                token.setClientToken(clientToken);
                token.setUser(user.getId());
                token.setProfile(user.getProfile());
                tokenRepository.save(token);

                log.info("Yggdrasil Login: " + username + " -> " + token.getAccessToken());

                JSONObject root = new JSONObject();
                root.put("accessToken", token.accessToken);
                root.put("clientToken", token.clientToken);
                JSONArray ava = new JSONArray();
                ava.add(profileRepository.getYggdrasilProfileByUuid(user.getProfile()).getJsonData("", settingsService.get(ServerSetting.Settings.Website).getValue()));
                root.put("availableProfiles", ava);
                root.put("selectedProfile", profileRepository.getYggdrasilProfileByUuid(user.getProfile()).getJsonData("", settingsService.get(ServerSetting.Settings.Website).getValue()));
                if(requestUser){
                    root.put("user", user.getJsonData());
                }

                return root;
            }else{
                log.info("Yggdrasil Login Failed: " + username);
                userService.addUserBanTime(username, request);
                throw YggdrasilError.Errors.AUTH_FAILED.getMsg();
            }
        }catch (UserError e){
            throw YggdrasilError.Errors.AUTH_FAILED.getMsg();
        }
    }

    /**
     * 清理过期 Token
     */
    @Scheduled(fixedDelay = TOKEN_INVALID_CHECK)
    public void cleanToken(){
        log.info("Cleaning Invalid Token...");
        List<YggdrasilToken> tokens = tokenRepository.findAll();
        for (YggdrasilToken token : tokens) {
            if(token.getTime() + TOKEN_OUT_DATE < System.currentTimeMillis()){
                tokenRepository.delete(token);
            }
        }
        log.info("Cleaned Invalid Token Finish");
    }

    /**
     * 会话验证数据
     */
    @Data
    @AllArgsConstructor
    public static class SessionCheck {
        /**
         * Access Token
         */
        String accessToken;

        /**
         * Ip
         */
        String ip;
    }
}
