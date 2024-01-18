package io.xiaoyi311.unifiedpass.service;

import io.xiaoyi311.unifiedpass.OtherUtil;
import io.xiaoyi311.unifiedpass.entity.User;
import io.xiaoyi311.unifiedpass.entity.UserError;
import io.xiaoyi311.unifiedpass.entity.yggdrasil.YggdrasilProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

/**
 * 角色服务
 * @author xiaoyi311
 */
@Service
@Slf4j
public class ProfileService {
    @Autowired
    YggdrasilService yggdrasilService;

    /**
     * 设置用户信息
     * @param user 用户
     * @param model_in 模型
     * @param username_in 用户名
     */
    public void setInfo(User user, String model_in, String username_in){
        YggdrasilProfile profile = yggdrasilService.getProfile(user.getProfile());
        if(profile != null){
            String model = Objects.equals(model_in, "") ? profile.getModel() : model_in;
            String username = Objects.equals(username_in, "") ? profile.getName() : username_in;

            if(!Objects.equals(model, "default") && !Objects.equals(model, "slim")){
                throw new UserError("lang:profile.unknown_model");
            }

            if(!Objects.equals(username, profile.getName())){
                profile.setName(username);
                yggdrasilService.setTempInvalid(profile.getUuid());
            }

            profile.setModel(model);
            yggdrasilService.saveProfile(profile);
            log.info("Change Info: " + user.getUsername(), " -> " + model + ", " + username);
        }else{
            throw new UserError("lang:server.unknown_profile");
        }
    }

    /**
     * 上传皮肤
     * @param file 文件
     * @param user 用户
     */
    public void uploadSkin(MultipartFile file, User user){
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
            folder.mkdirs();

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
            log.info("Upload Skin: " + user.getUsername() + " -> " + filename);
        } catch (IOException e) {
            throw new UserError("lang:upload.fail");
        }
    }
}
