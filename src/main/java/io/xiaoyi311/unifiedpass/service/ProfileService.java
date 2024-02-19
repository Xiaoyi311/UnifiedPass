package io.xiaoyi311.unifiedpass.service;

import io.xiaoyi311.unifiedpass.OtherUtil;
import io.xiaoyi311.unifiedpass.dao.CapeRepository;
import io.xiaoyi311.unifiedpass.dao.UserRepository;
import io.xiaoyi311.unifiedpass.entity.Cape;
import io.xiaoyi311.unifiedpass.entity.User;
import io.xiaoyi311.unifiedpass.entity.UserError;
import io.xiaoyi311.unifiedpass.entity.yggdrasil.YggdrasilProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 角色服务
 * @author xiaoyi311
 */
@Service
@Slf4j
public class ProfileService {
    final YggdrasilService yggdrasilService;
    final UserRepository userRepository;
    final CapeRepository capeRepository;

    /**
     * 实例化角色服务
     */
    public ProfileService(YggdrasilService yggdrasilService, UserRepository userRepository, CapeRepository capeRepository) {
        this.yggdrasilService = yggdrasilService;
        this.userRepository = userRepository;
        this.capeRepository = capeRepository;
    }

    /**
     * 设置角色信息
     * @param user 用户
     * @param modelIn 模型
     * @param usernameIn 用户名
     * @param cape 披风
     */
    public void setInfo(User user, String modelIn, String usernameIn, String cape){
        YggdrasilProfile profile = yggdrasilService.getProfile(user.getProfile());
        if(profile != null){
            String model = Objects.equals(modelIn, "") ? profile.getModel() : modelIn;
            String username = Objects.equals(usernameIn, "") ? profile.getName() : usernameIn;
            setInfo(profile, model, username, cape);
            log.info("Change Profile Info: {} -> [{}, {}, {}]", user.getId(), model, username, cape);
        }else{
            throw new UserError("lang:server.unknown_profile");
        }
    }

    /**
     * 设置指定角色信息
     * @param user 用户
     * @param uuid 角色 UUID
     * @param modelIn 模型
     * @param usernameIn 用户名
     * @param cape 披风
     */
    public void setInfo(User user, String uuid, String modelIn, String usernameIn, String cape){
        YggdrasilProfile profile = yggdrasilService.getProfile(uuid);
        if(profile != null){
            String model = Objects.equals(modelIn, "") ? profile.getModel() : modelIn;
            String username = Objects.equals(usernameIn, "") ? profile.getName() : usernameIn;
            setInfo(profile, model, username, cape);
            log.info("Change Profile Info: {}: {} -> [{}, {}, {}]", user.getId(), uuid, model, username, cape);
        }else{
            throw new UserError("lang:server.unknown_profile");
        }
    }

    /**
     * 设置角色信息
     * @param profile 角色
     * @param model 模型
     * @param username 用户名
     * @param cape 披风
     */
    private void setInfo(YggdrasilProfile profile, String model, String username, String cape){
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
            checkSkin(file);
            String filename = writeSkin(file, user.getProfile());
            log.info("Upload Skin: {} -> {}", user.getId(), filename);
        } catch (IOException e) {
            throw new UserError("lang:upload.fail");
        }
    }

    /**
     * 上传皮肤
     * @param file 文件
     * @param uuid 角色 UUID
     * @param user 用户
     */
    public void uploadSkin(MultipartFile file, String uuid, User user){
        if(file == null || file.isEmpty()){
            throw new UserError("lang:upload.empty");
        }

        if(!Objects.equals(file.getContentType(), "image/png")){
            throw new UserError("lang:upload.unSupport_type");
        }

        try {
            checkSkin(file);
            String filename = writeSkin(file, uuid);
            log.info("Upload Skin: {}: {} -> {}", user.getId(), uuid, filename);
        } catch (IOException e) {
            throw new UserError("lang:upload.fail");
        }
    }

    /**
     * 检查皮肤合法性
     * @param file 皮肤数据
     */
    private void checkSkin(MultipartFile file) throws IOException {
        BufferedImage bi = ImageIO.read(file.getInputStream());
        if(bi == null){
            throw new UserError("lang:upload.noImg");
        }

        if ((bi.getWidth() % 64 != 0 || bi.getHeight() % 64 != 0) || (bi.getWidth() % 32 != 0 || bi.getHeight() % 32 != 0)) {
            throw new UserError("lang:upload.unSafe");
        }
    }

    /**
     * 写皮肤入磁盘
     * @param file 皮肤数据
     * @param uuid 角色 UUID
     * @return 文件名
     */
    private String writeSkin(MultipartFile file, String uuid) throws IOException {
        File folder = new File(System.getProperty("user.dir"), "texture");
        folder.mkdirs();

        YggdrasilProfile profile = yggdrasilService.getProfile(uuid);
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
        return filename;
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
        String[] profiles = profile.split(",");
        boolean isBad = false;
        for (String p : profiles) {
            try{
                sbCape(cape, p, true);
                log.info("Send Cape: {} -> [{}: {}]", user.getId(), p, cape);
            }catch (Exception e){
                if(profiles.length == 1){
                    throw e;
                }
                isBad = true;
                log.error("Send Cape Error: {} -> [{}: {}]", user.getId(), p, cape);
            }
        }
        if(isBad){
            throw new UserError("lang:cape.sendError");
        }
    }

    /**
     * 收回披风
     * @param user 操作用户
     * @param cape 披风 UUID
     * @param profile 被收回角色
     */
    public void backCape(User user, String cape, String profile){
        String[] profiles = profile.split(",");
        boolean isBad = false;
        for (String p : profiles) {
            try{
                sbCape(cape, p, false);
                log.info("Back Cape: {} -> [{}: {}]", user.getId(), p, cape);
            }catch (Exception e){
                if(profiles.length == 1){
                    throw e;
                }
                isBad = true;
                log.error("Back Cape Error: {} -> [{}: {}]", user.getId(), p, cape);
            }
        }
        if(isBad){
            throw new UserError("lang:cape.backError");
        }
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
