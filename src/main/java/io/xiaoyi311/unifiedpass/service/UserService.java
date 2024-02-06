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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
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

    final UserRepository userTable;
    final ProfileRepository profileRepository;
    final RedisTemplate<String, String> redisTemplate;

    Map<String, BanUser> banUserList = new HashMap<>();

    public UserService(UserRepository userTable, ProfileRepository profileRepository, RedisTemplate<String, String> redisTemplate) {
        this.userTable = userTable;
        this.profileRepository = profileRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 由角色 UUID 获取用户
     * @param uuid 角色 UUID
     * @return 用户
     */
    public User getUserByProfile(String uuid){
        YggdrasilProfile profile = profileRepository.getYggdrasilProfileByUuid(uuid);
        if(profile == null){
            throw new UserError("lang:user.not_exist");
        }

        return userTable.getUserById(profile.user);
    }

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

        userUpCheck(username, password);

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
     * 修改用户信息
     * @param username 用户名
     * @param passwordOld 旧密码
     * @param passwordNew 新密码
     * @param user 用户
     */
    public void setInfo(String username, String passwordOld, String passwordNew, User user){
        if(username.length() <= 5 || username.length() > 20 || OtherUtil.isSuccessStr(username)){
            throw new UserError("lang:user.username_invalid");
        }

        if(!Objects.equals(passwordOld, "")) {
            if(passwordNew.length() <= 5 || passwordNew.length() > 20){
                throw new UserError("lang:user.password_invalid");
            }

            if(!OtherUtil.sha256(passwordOld).equals(user.getPassword())){
                throw new UserError("lang:user.password_wrong");
            }
        }

        user.setUsername(username);
        if(!Objects.equals(passwordOld, "")){
            user.setPassword(OtherUtil.sha256(passwordNew));
        }
        userTable.save(user);
    }

    /**
     * 注册账户
     * @param username 用户名
     * @param password 密码
     * @param code 验证码
     */
    public void register(String username, String password, String code){
        userUpCheck(username, password);

        String uuid = redisTemplate.opsForValue().get("authCode:" + code);
        if(uuid == null){
            throw new UserError("lang:user.code_invalid");
        }

        if(userTable.existsUserByUsernameIgnoreCaseOrMojang(username, uuid) || profileRepository.existsByName(username)){
            throw new UserError("lang:user.exist");
        }

        YggdrasilProfile profile = new YggdrasilProfile();
        User user = new User();
        profile.setName(username);
        profile.setUser(user.getId());

        user.setUsername(username);
        user.setPassword(OtherUtil.sha256(password));
        user.setMojang(uuid);
        user.setProfile(profile.getUuid());

        log.info("User Register: " + user.username);
        userTable.save(user);
        profileRepository.save(profile);
        redisTemplate.delete("authCode:" + code);
    }

    /**
     * 创建 UVS 验证码
     * @param uuid 正版 UUID
     * @param user 用户
     * @return UVS 验证码
     */
     public Integer createUvs(String uuid, User user){
         if(uuid.length() != 32){
             throw new UserError("lang:user.uuid_invalid");
         }

         if(userTable.existsUserByMojang(uuid)){
            throw new UserError("lang:user.exist");
         }

         int code = (int) Math.round((Math.random() * 9 + 1 ) * 100000);
         redisTemplate.opsForValue().set("authCode:" + code, uuid, Duration.ofMinutes(5));
         log.info("Create UVS " + uuid + ": " + code + " <- " + user.getId());
         return code;
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
     * 用户用户名与密码检查
     */
    private void userUpCheck(String username, String password){
        if(username.length() <= 5 || username.length() > 20 || OtherUtil.isSuccessStr(username)){
            throw new UserError("lang:user.username_invalid");
        }

        if(password.length() <= 5 || password.length() > 20){
            throw new UserError("lang:user.password_invalid");
        }
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
}
