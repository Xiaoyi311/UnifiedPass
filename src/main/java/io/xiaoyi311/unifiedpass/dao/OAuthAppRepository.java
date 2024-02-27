package io.xiaoyi311.unifiedpass.dao;

import io.xiaoyi311.unifiedpass.entity.OAuthApp;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * OAuth APP 数据库控制层
 * @author xiaoyi311
 */
public interface OAuthAppRepository extends JpaRepository<OAuthApp, String> {
}
