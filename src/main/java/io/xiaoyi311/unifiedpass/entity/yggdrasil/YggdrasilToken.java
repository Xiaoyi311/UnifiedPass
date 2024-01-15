package io.xiaoyi311.unifiedpass.entity.yggdrasil;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * Yggdrasil 令牌
 * @author xiaoyi311
 */
@Data
@Entity
@Table(name = "up_tokenlist")
public class YggdrasilToken implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 服务端令牌
     */
    @Id
    public String accessToken = UUID.randomUUID().toString().replaceAll("-", "");

    /**
     * 客户端令牌
     */
    @Column
    public String clientToken;

    /**
     * 角色
     */
    @Column
    public String profile;

    /**
     * 用户
     */
    @Column
    public String user;

    /**
     * 颁发时间
     */
    @Column
    public long time = System.currentTimeMillis();

    /**
     * 临时失效
     */
    @Column
    public boolean tempInvalid = false;
}
