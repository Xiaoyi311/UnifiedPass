package io.xiaoyi311.unifiedpass.controller;

import com.alibaba.fastjson.JSONObject;
import io.xiaoyi311.unifiedpass.annotation.Permission;
import io.xiaoyi311.unifiedpass.entity.ResponseData;
import io.xiaoyi311.unifiedpass.entity.User;
import io.xiaoyi311.unifiedpass.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 身份验证控制器
 * @author xiaoyi311
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private UserService userService;

    /**
     * 注册
     * @param data 数据
     * @return OK
     */
    @PostMapping("register")
    public HttpStatus register(
            @RequestBody JSONObject data
    ){
        userService.register(
                (String) data.getOrDefault("username", ""),
                (String) data.getOrDefault("password", ""),
                (Integer) data.getOrDefault("code", 0)
        );
        return HttpStatus.NO_CONTENT;
    }

    /**
     * 登陆
     * @param data 数据
     * @return OK
     */
    @PostMapping("login")
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
     * @return OK
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
     * @return 用户信息序列化
     */
    @Permission
    @GetMapping("info")
    public User info(
            User user
    ){
        return user;
    }
}
