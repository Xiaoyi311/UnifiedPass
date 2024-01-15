package io.xiaoyi311.unifiedpass.dao;

import io.xiaoyi311.unifiedpass.entity.yggdrasil.YggdrasilProfile;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 玩家角色数据库控制层
 * @author xiaoyi311
 */
public interface ProfileRepository extends JpaRepository<YggdrasilProfile, Integer> {
    /**
     * 由 UUID 获取角色
     * @param uuid UUID
     * @return 角色
     */
    YggdrasilProfile getYggdrasilProfileByUuid(String uuid);

    /**
     * 判断玩家名是否存在
     */
    boolean existsByName(String name);
}
