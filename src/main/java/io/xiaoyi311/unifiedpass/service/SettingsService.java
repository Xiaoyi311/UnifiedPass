package io.xiaoyi311.unifiedpass.service;

import io.xiaoyi311.unifiedpass.dao.SettingRepository;
import io.xiaoyi311.unifiedpass.entity.ServerSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 服务器设置服务
 * @author xiaoyi311
 */
@Service
public class SettingsService {
    @Autowired
    SettingRepository settingRepository;

    /**
     * 获取设置
     * @param key Key
     * @return 设置
     */
    public ServerSetting get(ServerSetting.Settings key){
        return settingRepository.getServerSettingByKey(key);
    }
}
