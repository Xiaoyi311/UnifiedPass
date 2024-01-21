package io.xiaoyi311.unifiedpass.controller;

import com.alibaba.fastjson.JSONObject;
import io.xiaoyi311.unifiedpass.annotation.Permission;
import io.xiaoyi311.unifiedpass.entity.Cape;
import io.xiaoyi311.unifiedpass.entity.User;
import io.xiaoyi311.unifiedpass.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 角色控制器
 * @author xiaoyi311
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    final BigDecimal PAGE_SIZE = BigDecimal.valueOf(4);

    @Autowired
    ProfileService profileService;

    /**
     * 设置角色信息
     * @param data 信息
     * @return OK
     */
    @PostMapping("setInfo")
    @Permission
    public HttpStatus setInfo(@RequestBody JSONObject data, User user){
        profileService.setInfo(
                user,
                (String) data.getOrDefault("model", ""),
                (String) data.getOrDefault("username", ""),
                (String) data.getOrDefault("cape", "")
        );
        return HttpStatus.NO_CONTENT;
    }

    /**
     * 上传皮肤
     * @param file 文件
     * @param user 用户
     * @return 204
     */
    @PostMapping("uploadSkin")
    @Permission
    public HttpStatus uploadSkin(MultipartFile file, User user){
        profileService.uploadSkin(file, user);
        return HttpStatus.NO_CONTENT;
    }

    /**
     * 获取披风列表
     * @param args 参数
     * @return 披风列表
     */
    @GetMapping("getCapes")
    @Permission(true)
    public JSONObject getCapes(@RequestParam Map<String, String> args){
        JSONObject root = new JSONObject();
        BigDecimal total = BigDecimal.valueOf(profileService.getCapeTotal());
        root.put("total", total.divide(PAGE_SIZE, RoundingMode.UP).longValue());
        root.put("capes", profileService.getCapes(
                PageRequest.of(
                        Integer.parseInt(args.getOrDefault("page", "0")),
                        PAGE_SIZE.intValue()
                )
        ));
        return root;
    }

    /**
     * 获取用户披风列表
     * @param user 用户
     * @return 披风列表
     */
    @GetMapping("getUserCapes")
    @Permission
    public List<Cape> getUserCapes(User user){
        return profileService.getProfileCapes(user.getProfile());
    }

    /**
     * 获取指定角色披风列表
     * @param uuid 指定角色
     * @return 披风列表
     */
    @GetMapping("getUserCapes/{uuid}")
    @Permission(true)
    public List<Cape> getUserCapes(@PathVariable(required = false) String uuid){
        return profileService.getProfileCapes(uuid);
    }

    /**
     * 新建/编辑披风
     * @param data 信息
     * @param user 用户
     * @return 披风信息
     */
    @PostMapping("setCape")
    @Permission(true)
    public Cape setCape(@RequestBody JSONObject data, User user){
        return profileService.setCape(
                user,
                (String) data.getOrDefault("uuid", ""),
                (String) data.getOrDefault("name", ""),
                Cape.CapeType.valueOf(((String) data.getOrDefault("type", "activity")).toUpperCase(Locale.ROOT))
        );
    }

    /**
     * 删除披风
     * @param data 信息
     * @param user 用户
     * @return 披风信息
     */
    @DeleteMapping("delCape")
    @Permission(true)
    public HttpStatus delCape(@RequestBody JSONObject data, User user){
        profileService.delCape(
                user,
                (String) data.getOrDefault("uuid", "")
        );
        return HttpStatus.NO_CONTENT;
    }

    /**
     * 上传披风
     * @param file 文件
     * @param user 用户
     * @param uuid 披风 UUID
     * @return 204
     */
    @PostMapping("uploadCape/{uuid}")
    @Permission(true)
    public HttpStatus uploadCape(MultipartFile file, User user, @PathVariable(required = false) String uuid){
        profileService.uploadCape(file, uuid, user);
        return HttpStatus.NO_CONTENT;
    }

    /**
     * 发放披风
     * @param data 信息
     * @param user 用户
     * @return 204
     */
    @PostMapping("sendCape")
    @Permission(true)
    public HttpStatus sendCape(@RequestBody JSONObject data, User user){
        profileService.sendCape(
                user,
                (String) data.getOrDefault("cape", ""),
                (String) data.getOrDefault("user", "")
        );
        return HttpStatus.NO_CONTENT;
    }

    /**
     * 收回披风
     * @param data 信息
     * @param user 用户
     * @return 204
     */
    @PostMapping("backCape")
    @Permission(true)
    public HttpStatus backCape(@RequestBody JSONObject data, User user){
        profileService.backCape(
                user,
                (String) data.getOrDefault("cape", ""),
                (String) data.getOrDefault("user", "")
        );
        return HttpStatus.NO_CONTENT;
    }
}
