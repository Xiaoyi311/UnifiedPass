package io.xiaoyi311.unifiedpass.dao;

import io.xiaoyi311.unifiedpass.entity.Cape;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 披风数据库控制层
 * @author xiaoyi311
 */
public interface CapeRepository extends JpaRepository<Cape, String> {
    /**
     * 由 UUID 获取披风
     * @param uuid UUID
     * @return 披风
     */
    Cape getCapeByUuid(String uuid);
}
