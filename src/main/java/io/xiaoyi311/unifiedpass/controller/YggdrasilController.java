package io.xiaoyi311.unifiedpass.controller;

import com.alibaba.fastjson.JSONObject;
import io.xiaoyi311.unifiedpass.entity.ServerSetting;
import io.xiaoyi311.unifiedpass.entity.yggdrasil.YggdrasilError;
import io.xiaoyi311.unifiedpass.entity.yggdrasil.YggdrasilProfile;
import io.xiaoyi311.unifiedpass.service.SettingsService;
import io.xiaoyi311.unifiedpass.service.YggdrasilService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Yggdrasil 统一控制器
 * @author xiaoyi311
 */
@RestController
@RequestMapping("/api/yggdrasil/")
public class YggdrasilController {
    @Autowired
    YggdrasilService yggdrasilService;

    @Autowired
    SettingsService settingsService;

    /**
     * 服务器信息<br>
     * @return 服务器信息
     */
    @GetMapping
    public JSONObject info(){
        return yggdrasilService.getInfo();
    }

    /**
     * 登录<br>
     * 使用密码进行身份验证，并分配一个新的令牌。
     * @param body 请求内容
     * @param request 请求
     * @return 响应内容/400
     */
    @PostMapping("authserver/authenticate")
    public Object login(@RequestBody JSONObject body, HttpServletRequest request){
        try{
            return yggdrasilService.login(
                    (String) body.getOrDefault("username", ""),
                    (String) body.getOrDefault("password", ""),
                    (String) body.getOrDefault("clientToken", UUID.randomUUID().toString().replaceAll("-", "")),
                    (Boolean) body.getOrDefault("requestUser", false),
                    request
            );
        }catch (Exception e){
            e.printStackTrace();
            return HttpStatus.BAD_REQUEST;
        }
    }

    /**
     * 刷新<br>
     * 吊销原令牌，并颁发一个新的令牌。
     * @param body 请求内容
     * @return 响应内容/400
     */
    @PostMapping("authserver/refresh")
    public Object refresh(@RequestBody JSONObject body){
        try{
            return yggdrasilService.refresh(
                    (String) body.getOrDefault("accessToken", ""),
                    (String) body.getOrDefault("clientToken", ""),
                    (Boolean) body.getOrDefault("requestUser", false),
                    (JSONObject) body.getOrDefault("selectedProfile", null)
            );
        }catch (Exception e){
            return HttpStatus.BAD_REQUEST;
        }
    }

    /**
     * 验证令牌<br>
     * 检验令牌是否有效。
     * @param body 请求内容
     * @return 204/TOKEN_INVALID
     */
    @PostMapping("authserver/validate")
    public HttpStatus validate(@RequestBody JSONObject body){
        if (yggdrasilService.validate(
                (String) body.getOrDefault("accessToken", ""),
                (String) body.getOrDefault("clientToken", "")
        )){
            return HttpStatus.NO_CONTENT;
        } else {
            throw YggdrasilError.Errors.TOKEN_INVALID.getMsg();
        }
    }

    /**
     * 吊销令牌<br>
     * 吊销给定令牌。
     * @param body 请求内容
     * @return 204
     */
    @PostMapping("authserver/invalidate")
    public HttpStatus invalidate(@RequestBody JSONObject body){
        try{
            yggdrasilService.invalidate(
                    (String) body.getOrDefault("accessToken", "")
            );
        } catch (Error ignored) {}
        return HttpStatus.NO_CONTENT;
    }

    /**
     * 登出<br>
     * 吊销用户的所有令牌。
     * @param body 请求内容
     * @param request 请求
     * @return 204
     */
    @PostMapping("authserver/signout")
    public HttpStatus signout(@RequestBody JSONObject body, HttpServletRequest request){
        yggdrasilService.signout(
                (String) body.getOrDefault("username", ""),
                (String) body.getOrDefault("password", ""),
                request
        );
        return HttpStatus.NO_CONTENT;
    }

    /**
     * 查询角色属性<br>
     * 查询指定角色的完整信息（包含角色属性）。
     * @param args 请求内容
     * @param uuid 角色的 UUID（无符号）
     * @return 角色信息/204
     */
    @GetMapping("sessionserver/session/minecraft/profile/{uuid}")
    public Object profile(@RequestParam Map<String, Object> args, @PathVariable(required = false) String uuid){
        try{
            YggdrasilProfile profile = yggdrasilService.getProfile(uuid);
            return profile == null ? HttpStatus.NO_CONTENT : profile.getJsonData(
                    Boolean.parseBoolean((String) args.getOrDefault("unsigned", "true")) ?
                            "" : settingsService.get(ServerSetting.Settings.PrivateKey).getValue(),
                    settingsService.get(ServerSetting.Settings.Website).getValue()
            );
        }catch (Exception e){
            return HttpStatus.BAD_REQUEST;
        }
    }

    /**
     * 客户端进入服务器<br>
     * 记录服务端发送给客户端的 serverId，以备服务端检查。
     * @param body 请求内容
     * @return 204
     */
    @PostMapping("sessionserver/session/minecraft/join")
    public HttpStatus join(@RequestBody JSONObject body, HttpServletRequest request){
        yggdrasilService.join(
                (String) body.getOrDefault("accessToken", ""),
                (String) body.getOrDefault("selectedProfile", ""),
                (String) body.getOrDefault("serverId", ""),
                request
        );
        return HttpStatus.NO_CONTENT;
    }

    /**
     * 服务端验证客户端<br>
     * 检查客户端会话的有效性，即数据库中是否存在该 serverId 的记录，且信息正确。
     * @param args 请求内容
     * @return 角色信息/204
     */
    @GetMapping("/sessionserver/session/minecraft/hasJoined")
    public Object hasJoined(@RequestParam Map<String, Object> args){
        YggdrasilProfile profile = yggdrasilService.hasJoined(
                (String) args.getOrDefault("username", ""),
                (String) args.getOrDefault("serverId", ""),
                (String) args.getOrDefault("ip", "")
        );

        return profile == null ?
                HttpStatus.NO_CONTENT :
                profile.getJsonData(
                        settingsService.get(ServerSetting.Settings.PrivateKey).getValue(),
                        settingsService.get(ServerSetting.Settings.Website).getValue()
                );
    }
}
