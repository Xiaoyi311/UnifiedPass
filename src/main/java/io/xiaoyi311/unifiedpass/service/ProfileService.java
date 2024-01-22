package io.xiaoyi311.unifiedpass.service;

import io.xiaoyi311.unifiedpass.OtherUtil;
import io.xiaoyi311.unifiedpass.dao.CapeRepository;
import io.xiaoyi311.unifiedpass.dao.UserRepository;
import io.xiaoyi311.unifiedpass.entity.Cape;
import io.xiaoyi311.unifiedpass.entity.User;
import io.xiaoyi311.unifiedpass.entity.UserError;
import io.xiaoyi311.unifiedpass.entity.yggdrasil.YggdrasilProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 角色服务
 * @author xiaoyi311
 */
@Service
@Slf4j
public class ProfileService {
    @Autowired
    YggdrasilService yggdrasilService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CapeRepository capeRepository;

    /**
     * 设置用户信息
     * @param user 用户
     * @param model_in 模型
     * @param username_in 用户名
     * @param cape 披风
     */
    public void setInfo(User user, String model_in, String username_in, String cape){
        YggdrasilProfile profile = yggdrasilService.getProfile(user.getProfile());
        if(profile != null){
            String model = Objects.equals(model_in, "") ? profile.getModel() : model_in;
            String username = Objects.equals(username_in, "") ? profile.getName() : username_in;

            if(!Objects.equals(model, "default") && !Objects.equals(model, "slim")){
                throw new UserError("lang:profile.unknown_model");
            }

            List<String> capes = new ArrayList<>(List.of(profile.getCapes().split(",")));
            List<String> result = new ArrayList<>();
            if(!capes.contains(cape)){
                throw new UserError("lang:profile.unknown_cape");
            }
            capes.remove(cape);
            result.add(cape);
            result.addAll(capes);

            if(!Objects.equals(username, profile.getName())){
                profile.setName(username);
                yggdrasilService.setTempInvalid(profile.getUuid());
            }

            profile.setCapes(String.join(",", result));
            profile.setModel(model);
            yggdrasilService.saveProfile(profile);
            log.info("Change Profile Info: {} -> [{}, {}, {}]", user.getId(), model, username, cape);
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
            log.info("Upload Skin: {} -> {}", user.getId(), filename);
        } catch (IOException e) {
            throw new UserError("lang:upload.fail");
        }
    }

    /**
     * 上传披风
     * @param file 文件
     * @param uuid 披风 UUID
     * @param user 用户
     */
    public void uploadCape(MultipartFile file, String uuid, User user){
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

            if (bi.getWidth() % 64 != 0 || bi.getHeight() % 32 != 0) {
                throw new UserError("lang:upload.unSafe");
            }

            File folder = new File(System.getProperty("user.dir"), "texture");
            folder.mkdirs();

            Cape cape = capeRepository.getCapeByUuid(uuid);
            if(cape == null){
                throw new UserError("lang:upload.noCapeFound");
            }

            File old = new File(folder, cape.getUuid());
            if(old.exists()){
                old.delete();
            }

            String filename = cape.getUuid();
            File save = new File(folder, filename);
            file.transferTo(save);

            capeRepository.save(cape);
            log.info("Upload Cape: {} -> {}", user.getId(), filename);
        } catch (IOException e) {
            throw new UserError("lang:upload.fail");
        }
    }

    /**
     * 获取披风总数
     * @return 披风总数
     */
    public long getCapeTotal(){
        return capeRepository.count();
    }

    /**
     * 分页获取披风列表
     * @param pageable 分页参数
     * @return 披风列表
     */
    public List<Cape> getCapes(Pageable pageable){
        return capeRepository.findAll(pageable).toList();
    }

    /**
     * 设置披风信息
     * @param user 用户
     * @param uuid UUID
     * @param name 披风名
     * @param type 披风类型
     */
    public Cape setCape(User user, String uuid, String name, Cape.CapeType type){
        Cape cape = capeRepository.getCapeByUuid(uuid);
        cape = cape == null ? new Cape() : cape;

        if(Objects.equals(name, "")){
            throw new UserError("lang:cape.req_name");
        }

        cape.setName(name);
        cape.setType(type);
        cape.setEditTime(System.currentTimeMillis());
        capeRepository.save(cape);
        log.info("Change Cape Info: {} -> {}: [{}, {}]", user.getId(), cape.getUuid(), cape.getName(), cape.getType().name());

        return cape;
    }

    /**
     * 删除披风
     * @param user 用户
     * @param uuid 披风 UUID
     */
    public void delCape(User user, String uuid){
        Cape cape = capeRepository.getCapeByUuid(uuid);
        if(cape != null){
            File folder = new File(System.getProperty("user.dir"), "texture");
            folder.mkdirs();

            File name = new File(folder, cape.getUuid());
            if(name.exists()){
                name.delete();
            }

            capeRepository.delete(cape);
            log.info("Delete Cape: {} -> {}", user.getId(), uuid);

            List<YggdrasilProfile> profiles = yggdrasilService.profileRepository.findYggdrasilProfilesByCapesContains(cape.getUuid());
            profiles.forEach((profile -> {
                List<String> result = new ArrayList<>(List.of(profile.getCapes().split(",")));
                result.remove(cape.getUuid());
                profile.setCapes(String.join(",", result));
                yggdrasilService.profileRepository.save(profile);

                log.info("Delete Profile Cape: {} -> {}", profile.getUuid(), cape.getUuid());
            }));
        }
    }

    /**
     * 获取指定角色披风列表
     * @param uuid 指定角色
     * @return 披风列表
     */
    public List<Cape> getProfileCapes(String uuid){
        YggdrasilProfile profile = yggdrasilService.getProfile(uuid);
        if(profile == null){
            throw new UserError("lang:profile.notFound");
        }

        List<Cape> result = new ArrayList<>();
        if(!Objects.equals(profile.getCapes(), "")) {
            for (String capeId : profile.getCapes().split(",")) {
                if(!Objects.equals(capeId, "NONE")) {
                    Cape cape = capeRepository.getCapeByUuid(capeId);
                    result.add(cape);
                }else{
                    Cape none = new Cape();
                    none.setType(Cape.CapeType.SPECIAL);
                    none.setName("无披风");
                    result.add(none);
                }
            }
        }

        return result;
    }

    /**
     * 发放披风
     * @param user 操作用户
     * @param cape 披风 UUID
     * @param profile 被发放角色
     */
    public void sendCape(User user, String cape, String profile){
        sbCape(cape, profile, true);
        log.info("Send Cape: {} -> [{}: {}]", user.getId(), profile, cape);
    }

    /**
     * 收回披风
     * @param user 操作用户
     * @param cape 披风 UUID
     * @param profile 被收回角色
     */
    public void backCape(User user, String cape, String profile){
        sbCape(cape, profile, false);
        log.info("Back Cape: {} -> [{}: {}]", user.getId(), profile, cape);
    }

    /**
     * 发放/收回披风
     * @param capeId 披风 ID
     * @param profileId 角色 ID
     * @param send 是否为发动，否则收回
     */
    private void sbCape(String capeId, String profileId, boolean send){
        Cape cape = capeRepository.getCapeByUuid(capeId);
        if(cape == null){
            throw new UserError("lang:cape.notFound");
        }

        YggdrasilProfile profile = yggdrasilService.getProfile(profileId);
        if(profile == null){
            throw new UserError("lang:profile.notFound");
        }

        List<String> capes = Objects.equals(profile.getCapes(), "") ?
                new ArrayList<>() :
                new ArrayList<>(List.of(profile.getCapes().split(",")));

        if(send) {
            if(capes.contains(cape.getUuid())){
                throw new UserError("lang:cape.playerHad");
            }
            capes.add(cape.getUuid());
        }else{
            if(!capes.contains(cape.getUuid())){
                throw new UserError("lang:cape.playerNotHave");
            }
            capes.remove(cape.getUuid());
        }

        profile.setCapes(capes.isEmpty() ? null : String.join(",", capes));
        yggdrasilService.profileRepository.save(profile);
    }
}
