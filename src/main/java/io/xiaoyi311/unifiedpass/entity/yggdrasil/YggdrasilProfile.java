package io.xiaoyi311.unifiedpass.entity.yggdrasil;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.xiaoyi311.unifiedpass.OtherUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.Serial;
import java.io.Serializable;
import java.util.Base64;
import java.util.UUID;

/**
 * Yggdrasil 角色
 * @author xiaoyi311
 */
@Data
@Entity
@Component
@Table(name = "up_profilelist")
public class YggdrasilProfile implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * UUID
     */
    @Id
    public String uuid = UUID.randomUUID().toString().replaceAll("-", "");

    /**
     * 角色名
     */
    @Column
    public String name;

    /**
     * 模型
     */
    @Column
    public String model = "default";

    /**
     * 皮肤
     */
    @Column
    public String skin;

    /**
     * 披风
     */
    @Column
    public String cape;

    /**
     * 用户
     */
    @Column
    public String user;

    /**
     * 获取 JSON 数据
     * @param privateKey 私钥(留空不签名)
     * @param website 域名
     * @return JSON 数据
     */
    public JSONObject getJsonData(String privateKey, String website){
        JSONObject root = new JSONObject();
        root.put("id", uuid);
        root.put("name", name);

        JSONArray properties = new JSONArray();

        JSONObject textures = new JSONObject();
        textures.put("name", "textures");
        String b64 = Base64.getEncoder().encodeToString(getTextureData(website).toJSONString().getBytes());
        textures.put("value", b64);
        if(!privateKey.isEmpty()){
            textures.put("signature", OtherUtil.rsaSign(privateKey, b64));
        }

        properties.add(textures);

        /* 不可上传材质
        JSONObject uploadableTextures = new JSONObject();
        uploadableTextures.put("name", "uploadableTextures");
        uploadableTextures.put("value", "skin");
        if(!privateKey.isEmpty()){
            uploadableTextures.put("signature", OtherUtil.rsaSign(privateKey, "skin"));
        }

        properties.add(uploadableTextures);
        */

        root.put("properties", properties);
        return root;
    }

    /**
     * 获取材质数据
     * @param website 域名
     * @return 材质数据
     */
    private JSONObject getTextureData(String website){
        JSONObject root = new JSONObject();
        root.put("timestamp", System.currentTimeMillis());
        root.put("profileId", uuid);
        root.put("profileName", name);

        JSONObject textures = new JSONObject();

        if(skin != null){
            JSONObject skin = new JSONObject();
            skin.put("url", website + "/textures/" + this.skin);

            JSONObject metadata = new JSONObject();
            metadata.put("model", model);

            skin.put("metadata", metadata);

            textures.put("SKIN", skin);
        }

        if(cape != null){
            JSONObject cape = new JSONObject();
            cape.put("url", this.cape);

            textures.put("CAPE", cape);
        }

        root.put("textures", textures);
        return root;
    }
}
