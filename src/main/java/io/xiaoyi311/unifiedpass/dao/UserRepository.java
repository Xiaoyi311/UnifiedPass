package io.xiaoyi311.unifiedpass.dao;

import io.xiaoyi311.unifiedpass.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 用户信息数据库控制层
 * @author xiaoyi311
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    /**
     * 是否存在指定用户
     * @param username 用户名
     * @param mojang 正版 UUID
     * @return 是否存在
     */
    boolean existsUserByUsernameIgnoreCaseOrMojang(String username, String mojang);

    /**
     * 由用户名获取用户
     * @param username 用户名
     * @return 用户
     */
    User getUserByUsername(String username);

    /**
     * 由用户 ID 获取用户
     * @param id 用户 ID
     * @return 用户
     */
    User getUserById(String id);
}
