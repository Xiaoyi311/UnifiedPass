package io.xiaoyi311.unifiedpass.entity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * 用户
 * @author xiaoyi311
 */
@Data
@Entity
@Table(name = "up_userlist")
public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id
    public String id = UUID.randomUUID().toString().replaceAll("-", "");

    /**
     * 用户名
     */
    @Column
    public String username;

    /**
     * Mojang UUID
     */
    @Column
    public String mojang;

    /**
     * 密码
     */
    @Column
    @JSONField(serialize = false)
    public String password;

    /**
     * 注册时间
     */
    @Column
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    public long registerTime = System.currentTimeMillis();

    /**
     * 管理员
     */
    @Column
    public boolean admin = false;

    /**
     * 绑定的角色
     */
    @Column
    public String profile;

    /**
     * 获取 JSON 数据
     * @return JSON 数据
     */
    public JSONObject getJsonData(){
        JSONObject root = new JSONObject();
        root.put("id", UUID.nameUUIDFromBytes(("TBMCUser: " + id).getBytes()).toString().replaceAll("-", ""));
        root.put("properties", new JSONArray());
        return root;
    }
}
