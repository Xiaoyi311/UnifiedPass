package io.xiaoyi311.unifiedpass.entity;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import io.xiaoyi311.unifiedpass.OtherUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * OAuth 应用实体
 * @author xiaoyi311
 */
@Data
@Entity
@Table(name = "up_oauthapp")
public class OAuthApp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Client ID
     */
    @Id
    String clientId = OtherUtil.randomString(32);

    /**
     * Client Secret
     */
    @Column
    String clientSecret = OtherUtil.randomString(32);

    /**
     * 回调地址
     */
    @Column
    String callback;

    /**
     * 允许授权模式
     */
    @Column
    String authMode = "code";

    /**
     * 允许申请权限
     */
    @Column
    String permission = "user.info";

    /**
     * 应用名称
     */
    @Column
    String name;

    /**
     * 应用描述
     */
    @Column
    String des = "";

    /**
     * 应用主页
     */
    @Column
    String website = "";

    /**
     * 获取 OAuth 信息
     * @return 信息
     */
    public JSONObject getInfo(){
        JSONObject json = new JSONObject();
        json.put("name", getName());
        json.put("des", getDes());
        json.put("website", getWebsite());
        json.put("callback", getCallback());
        return json;
    }
}
