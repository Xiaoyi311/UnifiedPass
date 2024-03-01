package io.xiaoyi311.unifiedpass.dao;

import io.xiaoyi311.unifiedpass.entity.yggdrasil.YggdrasilProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 玩家角色数据库控制层
 * @author xiaoyi311
 */
public interface ProfileRepository extends JpaRepository<YggdrasilProfile, String> {
    /**
     * 由 UUID 获取角色
     * @param uuid UUID
     * @return 角色
     */
    YggdrasilProfile getYggdrasilProfileByUuid(String uuid);

    /**
     * 判断角色名是否存在
     * @param name 角色名
     * @return 是否存在
     */
    boolean existsByName(String name);

    /**
     * 获取拥有指定披风的角色
     * @param cape 披风 UUID
     * @return 角色列表
     */
    List<YggdrasilProfile> findYggdrasilProfilesByCapesContains(String cape);

    /**
     * 由角色名获取角色
     * @param name 角色名
     * @return 角色
     */
    YggdrasilProfile getYggdrasilProfileByName(String name);
}
