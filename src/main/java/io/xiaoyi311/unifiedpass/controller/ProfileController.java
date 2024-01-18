package io.xiaoyi311.unifiedpass.controller;

import com.alibaba.fastjson.JSONObject;
import io.xiaoyi311.unifiedpass.annotation.Permission;
import io.xiaoyi311.unifiedpass.entity.User;
import io.xiaoyi311.unifiedpass.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 角色控制器
 * @author xiaoyi311
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
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
                (String) data.getOrDefault("username", "")
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

    @PostMapping("addCape")
    @Permission(true)
    public HttpStatus addCape(){
        return HttpStatus.NO_CONTENT;
    }
}
