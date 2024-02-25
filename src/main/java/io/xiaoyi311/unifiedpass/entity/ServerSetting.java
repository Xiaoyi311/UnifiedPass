package io.xiaoyi311.unifiedpass.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

/**
 * 服务器设置
 * @author xiaoyi311
 */
@Data
@Entity
@Table(name = "up_settings")
public class ServerSetting implements Serializable {
    /**
     * id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    /**
     * 键
     */
    @Column
    public Settings key;

    /**
     * 值
     */
    @Column
    public String value;

    /**
     * 服务器配置项
     */
    public enum Settings{
        /**
         * 服务器名称
         */
        ServerName,

        /**
         * 网站名
         */
        Website,

        /**
         * 公钥
         */
        PublicKey,

        /**
         * 私钥
         */
        PrivateKey,

        /**
         * 白名单
         */
        WhiteList
    }
}
