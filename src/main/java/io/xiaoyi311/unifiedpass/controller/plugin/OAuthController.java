package io.xiaoyi311.unifiedpass.controller.plugin;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.xiaoyi311.unifiedpass.OtherUtil;
import io.xiaoyi311.unifiedpass.annotation.CsrfProtect;
import io.xiaoyi311.unifiedpass.annotation.Permission;
import io.xiaoyi311.unifiedpass.entity.OAuthApp;
import io.xiaoyi311.unifiedpass.entity.ResponseData;
import io.xiaoyi311.unifiedpass.entity.User;
import io.xiaoyi311.unifiedpass.entity.UserError;
import io.xiaoyi311.unifiedpass.service.OAuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;


/**
 * OAuth 控制器
 * @author xiaoyi311
 */
@CrossOrigin
@RestController
@RequestMapping("/api/oauth")
public class OAuthController {
    OAuthService oAuthService;

    OAuthController(OAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }

    /**
     * 请求授权
     * @param args 请求信息
     * @param response 响应
     */
    @CsrfProtect(true)
    @GetMapping("authorize")
    public void authorize(@RequestParam Map<String, String> args, HttpServletResponse response) {
        if(!Objects.equals(args.get("response_type"), "code")){
            throw new UserError("Unsupported Authorize Type!");
        }

        try {
            response.sendRedirect(
                    "/#/oauthReq?redirect_uri="+args.getOrDefault("redirect_uri", "")
                            +"&client_id="+args.get("client_id")
                            +"&state="+args.get("state")
                            +"&scope="+args.getOrDefault("scope", "")
                            +"&response_type="+args.getOrDefault("response_type", ""));
        } catch (Exception e) {
            throw new UserError("Redirect To Authorize Failed!");
        }
    }

    /**
     * 创建一个 OAuth 应用程序
     * @param data 请求信息
     * @return 204
     */
    @Permission(true)
    @PostMapping("create")
    public HttpStatus create(
            @RequestBody JSONObject data
    ){
        oAuthService.create(
                (String) data.getOrDefault("callback", ""),
                (String) data.getOrDefault("name", "")
        );
        return HttpStatus.NO_CONTENT;
    }

    /**
     * 删除一个 OAuth 应用程序
     * @param data 请求信息
     * @return 204
     */
    @Permission(true)
    @PostMapping("delete")
    public HttpStatus delete(
            @RequestBody JSONObject data
    ){
        oAuthService.delete(
                (String) data.getOrDefault("clientId", "")
        );
        return HttpStatus.NO_CONTENT;
    }

    /**
     * 列出所有 OAuth 应用程序
     * @return 所有 OAuth 应用程序信息
     */
    @Permission(true)
    @GetMapping("list")
    public JSONArray list(){
        return oAuthService.list();
    }

    /**
     * 修改一个 OAuth 应用程序
     * @param data 请求信息
     * @return 204
     */
    @Permission(true)
    @PostMapping("set")
    public HttpStatus set(
            @RequestBody JSONObject data
    ){
        oAuthService.set(data);
        return HttpStatus.NO_CONTENT;
    }

    /**
     * 获取 OAuth 应用程序的信息
     * @param clientId Client ID
     * @return OAuth 应用程序的信息
     */
    @GetMapping("info/{clientId}")
    public JSONObject info(
            @PathVariable(required = false) String clientId
    ){
        return oAuthService.getInfo(clientId);
    }

    /**
     * 接受授权
     * @param args 请求数据
     * @param user 用户
     */
    @CsrfProtect
    @Permission
    @GetMapping("accept")
    public ResponseData accept(
            User user,
            @RequestParam Map<String, String> args
    ){
        if(!Objects.equals(args.get("response_type"), "code")){
            throw new UserError("Unsupported Authorize Type!");
        }

        return ResponseData.def(oAuthService.accept(
                user,
                args.getOrDefault("client_id", ""),
                args.getOrDefault("redirect_uri", ""),
                args.getOrDefault("state", ""),
                args.getOrDefault("scope", "")
        ));
    }

    /**
     * 获取 Token
     * @param args 请求数据
     * @param response 响应
     * @return Token
     */
    @GetMapping("token")
    public JSONObject token(
            @RequestParam Map<String, String> args,
            HttpServletResponse response
    ){
        if(Objects.equals(args.get("grant_type"), "authorization_code")){
            return oAuthService.token(
                    args.getOrDefault("code", ""),
                    args.getOrDefault("client_id", ""),
                    args.getOrDefault("client_secret", ""),
                    args.getOrDefault("redirect_uri", ""),
                    response
            );
        } else if (Objects.equals(args.get("grant_type"), "refresh_token")){
            return oAuthService.refreshToken(
                    args.getOrDefault("refresh_token", ""),
                    args.getOrDefault("client_id", ""),
                    response
            );

        } else {
            throw new UserError("Unsupported Grant Type!");
        }
    }
}
