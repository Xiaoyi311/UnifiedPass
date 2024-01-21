package io.xiaoyi311.unifiedpass.service;

import io.xiaoyi311.unifiedpass.OtherUtil;
import io.xiaoyi311.unifiedpass.dao.ProfileRepository;
import io.xiaoyi311.unifiedpass.dao.UserRepository;
import io.xiaoyi311.unifiedpass.entity.User;
import io.xiaoyi311.unifiedpass.entity.UserError;
import io.xiaoyi311.unifiedpass.entity.yggdrasil.YggdrasilProfile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 用户服务
 * @author xiaoyi311
 */
@Service
@Slf4j
public class UserService {
    static final int LOGIN_OUT_DATE = 24 * 60 * 60 * 1000;
    static final int PRE_LOGIN_OUT_DATE = 7 * 24 * 60 * 60 * 1000;
    static final int LOGIN_FAIL_BAN = 5;
    static final int LOGIN_FAIL_BAN_TIME = 60 * 60 * 1000;

    @Autowired
    UserRepository userTable;

    @Autowired
    ProfileRepository profileRepository;

    @Autowired
    RedisTemplate<String, AuthCode> redisTemplate;

    Map<String, BanUser> banUserList = new HashMap<>();

    /**
     * 由 Session 获取用户
     * @param request 请求
     * @return 用户
     */
    public User getUserBySession(HttpServletRequest request){
        String id = (String) request.getSession().getAttribute("userId");
        if(id == null){
            return null;
        }

        return userTable.getUserById(id);
    }

    /**
     * 登录
     * @param username 用户名
     * @param password 密码
     * @param persistent 持久
     * @param request 请求
     */
    public void login(String username, String password, Boolean persistent, HttpServletRequest request){
        User user = banUserCheck(username);

        if(username.length() <= 5 || username.length() > 20 && OtherUtil.isValidStr(username)){
            throw new UserError("lang:user.username_invalid");
        }

        if(password.length() != 32 && OtherUtil.isValidStr(password)){
            throw new UserError("lang:user.password_invalid");
        }

        if(user != null && Objects.equals(user.getPassword(), OtherUtil.sha256(password))){
            log.info("User Login: " + username);
            HttpSession session = request.getSession();
            session.setMaxInactiveInterval(persistent ? PRE_LOGIN_OUT_DATE : LOGIN_OUT_DATE);
            session.setAttribute("isLogin", true);
            session.setAttribute("userId", user.id);
        }else{
            log.info("Login Failed: " + username);
            addUserBanTime(username, request);
            throw new UserError("lang:user.login_fail");
        }
    }

    /**
     * 注册账户
     * @param username 用户名
     * @param password 密码
     * @param code 授权码
     */
    public void register(String username, String password, Integer code){
        if(username.length() <= 5 || username.length() > 20 && OtherUtil.isValidStr(username)){
            throw new UserError("lang:user.username_invalid");
        }

        if(password.length() != 32 && OtherUtil.isValidStr(password)){
            throw new UserError("lang:user.password_invalid");
        }

        AuthCode auth = redisTemplate.opsForValue().get("authCode:" + code);
        if(auth == null){
            throw new UserError("lang:user.auth_code_invalid");
        }

        if(userTable.existsUserByUsernameIgnoreCaseOrMojang(username, auth.getUuid()) || profileRepository.existsByName(username)){
            throw new UserError("lang:user.exist");
        }

        YggdrasilProfile profile = new YggdrasilProfile();
        User user = new User();
        profile.setName(username);
        profile.setUser(user.getId());

        user.setUsername(username);
        user.setPassword(OtherUtil.sha256(password));
        user.setMojang(auth.getUuid());
        user.setProfile(profile.getUuid());
        redisTemplate.delete("authCode:" + code);

        log.info("User Register: " + user.username);
        userTable.save(user);
        profileRepository.save(profile);
    }

    /**
     * 添加用户错误次数
     * @param username 用户名
     * @param request 请求
     */
    public void addUserBanTime(String username, HttpServletRequest request){
        BanUser banUser = banUserList.get(username);
        if(banUser.time >= LOGIN_FAIL_BAN){
            log.info("User Ban: " + username + " <- " + OtherUtil.getRealIp(request));
            banUser.setUnix(System.currentTimeMillis());
        }else{
            banUser.time++;
        }
    }

    /**
     * 用户封禁检测
     * @param username 用户名
     * @return 用户
     */
    public User banUserCheck(String username){
        User user = userTable.getUserByUsername(username);
        if(user == null){
            throw new UserError("lang:auth.failed");
        }

        BanUser banUser = banUserList.get(username);
        if(banUser == null){
            banUser = new BanUser(0, 0);
            banUserList.put(username, banUser);
        }

        if(banUser.unix != 0 && banUser.unix + LOGIN_FAIL_BAN_TIME >= System.currentTimeMillis()){
            throw new UserError("lang:auth.failed");
        }

        return user;
    }

    /**
     * 用户封禁数据
     */
    @Data
    @AllArgsConstructor
    public static class BanUser {
        /**
         * 错误次数
         */
        private int time;

        /**
         * 封禁时间
         */
        private long unix;
    }

    /**
     * 授权码
     */
    @Data
    public static class AuthCode {
        /**
         * Mojang UUID
         */
        private String uuid;
    }
}
