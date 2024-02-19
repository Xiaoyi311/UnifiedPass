package io.xiaoyi311.unifiedpass.dao;

import io.xiaoyi311.unifiedpass.entity.ServerSetting;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 服务器设置表控制层
 * @author xiaoyi311
 */
public interface SettingRepository extends JpaRepository<ServerSetting, Integer> {
    /**
     * 由 Key 获取设置
     * @param key Key
     * @return 设置
     */
    ServerSetting getServerSettingByKey(ServerSetting.Settings key);
}
