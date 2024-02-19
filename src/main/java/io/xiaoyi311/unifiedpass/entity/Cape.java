package io.xiaoyi311.unifiedpass.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * 披风实体
 * @author xiaoyi311
 */
@Data
@Entity
@Table(name = "up_capelist")
public class Cape implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * UUID
     */
    @Id
    public String uuid = UUID.randomUUID().toString().replaceAll("-", "");

    /**
     * 名称
     */
    @Column
    public String name;

    /**
     * 类型
     */
    @Column
    public CapeType type = CapeType.ACTIVITY;

    /**
     * 创建时间
     */
    @Column
    public long createTime = System.currentTimeMillis();

    /**
     * 创建时间
     */
    @Column
    public long editTime = System.currentTimeMillis();

    /**
     * 披风类型
     */
    public enum CapeType{
        /**
         * 活动
         */
        ACTIVITY,

        /**
         * 权限
         */
        PERMISSION,

        /**
         * 回馈
         */
        REWARD,

        /**
         * 奖励
         */
        PRIZE,

        /**
         * 特殊
         */
        SPECIAL
    }
}
