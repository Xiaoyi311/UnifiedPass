package io.xiaoyi311.unifiedpass.controller;

import com.alibaba.fastjson.JSONObject;
import io.xiaoyi311.unifiedpass.annotation.GoogleVerify;
import io.xiaoyi311.unifiedpass.annotation.Permission;
import io.xiaoyi311.unifiedpass.entity.ResponseData;
import io.xiaoyi311.unifiedpass.entity.User;
import io.xiaoyi311.unifiedpass.service.MicrosoftService;
import io.xiaoyi311.unifiedpass.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 身份验证控制器
 * @author xiaoyi311
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private UserService userService;

    @Autowired
    private MicrosoftService microsoftService;

    /**
     * 注册
     * @param data 数据
     * @return 204
     */
    @PostMapping("register")
    @GoogleVerify
    public HttpStatus register(
            @RequestBody JSONObject data
    ){
        userService.register(
                (String) data.getOrDefault("username", ""),
                (String) data.getOrDefault("password", ""),
                (String) data.getOrDefault("mi_access", "")
        );
        return HttpStatus.NO_CONTENT;
    }

    /**
     * 修改用户信息
     * @param data 数据
     * @param user 用户
     * @return 204
     */
    @PostMapping("setInfo")
    @Permission
    public HttpStatus setInfo(
            @RequestBody JSONObject data,
            User user
    ){
        userService.setInfo(
                (String) data.getOrDefault("username", ""),
                (String) data.getOrDefault("password_old", ""),
                (String) data.getOrDefault("password_new", ""),
                user
        );
        return HttpStatus.NO_CONTENT;
    }

    /**
     * 登陆
     * @param data 数据
     * @param request 请求信息
     * @return OK
     */
    @PostMapping("login")
    //@GoogleVerify
    public ResponseData login(
            @RequestBody JSONObject data,
            HttpServletRequest request
    ){
        userService.login(
                (String) data.getOrDefault("username", ""),
                (String) data.getOrDefault("password", ""),
                (Boolean) data.getOrDefault("persistent", false),
                request
        );
        return ResponseData.deafult("OK");
    }

    /**
     * 登出
     * @param request 请求信息
     * @return 204
     */
    @GetMapping(value = "logout")
    public HttpStatus logout(
            HttpServletRequest request
    ){
        request.getSession().invalidate();
        return HttpStatus.NO_CONTENT;
    }

    /**
     * 账户信息<br>
     * 权限：普通
     * @param user 用户
     * @return 用户信息序列化
     */
    @Permission
    @GetMapping("info")
    public User info(
            User user
    ){
        return user;
    }

    /**
     * 微软登陆请求
     * @return 登录代码
     */
    @GoogleVerify
    //@GetMapping("miOauth")
    public ResponseData miOauth(){
        return ResponseData.deafult(microsoftService.spawnCode());
    }

    /**
     * 微软登陆请求状态
     * @param args 参数
     * @return 登录代码
     */
    //@GetMapping("miOauthStatus")
    public ResponseData miOauthStatus(@RequestParam Map<String, String> args){
        return ResponseData.deafult(microsoftService.checkCode(
                args.getOrDefault("code", "")
        ));
    }
}
