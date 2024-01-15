package io.xiaoyi311.unifiedpass.controller;

import com.alibaba.fastjson.JSONObject;
import io.xiaoyi311.unifiedpass.OtherUtil;
import io.xiaoyi311.unifiedpass.annotation.Permission;
import io.xiaoyi311.unifiedpass.entity.User;
import io.xiaoyi311.unifiedpass.entity.UserError;
import io.xiaoyi311.unifiedpass.entity.yggdrasil.YggdrasilProfile;
import io.xiaoyi311.unifiedpass.service.YggdrasilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

/**
 * 角色控制器
 * @author xiaoyi311
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    @Autowired
    YggdrasilService yggdrasilService;

    /**
     * 设置角色信息
     * @param data 信息
     * @return OK
     */
    @PostMapping("setInfo")
    @Permission
    public HttpStatus setInfo(@RequestBody JSONObject data, User user){
        YggdrasilProfile profile = yggdrasilService.getProfile(user.getProfile());
        if(profile != null){
            String model = (String) data.getOrDefault("model", profile.getModel());

            if(!Objects.equals(model, "default") && !Objects.equals(model, "slim")){
                throw new UserError("lang:profile.unknown_model");
            }

            if(!Objects.equals(data.getOrDefault("username", profile.getName()), profile.getName())){
                profile.setName((String) data.getOrDefault("username", profile.getName()));
                yggdrasilService.setTempInvalid(profile.getUuid());
            }

            profile.setModel((String) data.getOrDefault("model", profile.getModel()));
            yggdrasilService.saveProfile(profile);
        }else{
            throw new UserError("lang:server.unknown_profile");
        }
        return HttpStatus.NO_CONTENT;
    }

    @PostMapping("uploadSkin")
    @Permission
    public HttpStatus uploadSkin(MultipartFile file, User user){
        if(file == null || file.isEmpty()){
            throw new UserError("lang:upload.empty");
        }

        if(!Objects.equals(file.getContentType(), "image/png")){
            throw new UserError("lang:upload.unSupport_type");
        }

        try {
            BufferedImage bi = ImageIO.read(file.getInputStream());
            if(bi == null){
                throw new UserError("lang:upload.noImg");
            }

            if ((bi.getWidth() % 64 != 0 || bi.getHeight() % 64 != 0) || (bi.getWidth() % 32 != 0 || bi.getHeight() % 32 != 0)) {
                throw new UserError("lang:upload.unSafe");
            }

            File folder = new File(System.getProperty("user.dir"), "texture");
            folder.mkdir();

            YggdrasilProfile profile = yggdrasilService.getProfile(user.getProfile());
            if(profile.getSkin() != null){
                File old = new File(folder, profile.getSkin());
                if(old.exists()){
                    old.delete();
                }
            }

            String filename = OtherUtil.sha256(UUID.randomUUID().toString());
            File save = new File(folder, filename);
            file.transferTo(save);

            profile.setSkin(filename);
            yggdrasilService.saveProfile(profile);

            return HttpStatus.NO_CONTENT;
        } catch (IOException e) {
            throw new UserError("lang:upload.fail");
        }
    }
}
