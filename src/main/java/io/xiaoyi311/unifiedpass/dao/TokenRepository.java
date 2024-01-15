package io.xiaoyi311.unifiedpass.dao;

import io.xiaoyi311.unifiedpass.entity.yggdrasil.YggdrasilToken;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Yggdrasil 令牌表控制层
 * @author xiaoyi311
 */
public interface TokenRepository extends JpaRepository<YggdrasilToken, Integer> {
    /**
     * 由 User UUID 获取 Token 数量
     * @param user User UUID
     * @return 数量
     */
    int countByUser(String user);

    /**
     * 获取指定角色的所有 Token
     * @param profile 角色 UUID
     * @return 该角色的所有 Token
     */
    List<YggdrasilToken> getYggdrasilTokensByProfile(String profile);

    /**
     * 由 Access Token 获取 Token
     * @param accessToken Access Token
     * @return Token
     */
    YggdrasilToken getYggdrasilTokenByAccessToken(String accessToken);

    /**
     * 删除指定用户的所有 Token
     * @param user UUID
     */
    @Transactional
    void deleteAllByUser(String user);

    /**
     * 删除指定用户最早的一个 Token
     * @param user UUID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM YggdrasilToken e WHERE e.user = :user AND e.time = (SELECT MIN(e2.minTime) FROM (SELECT MIN(time) as minTime FROM YggdrasilToken WHERE user = :user) e2)")
    void removeByUserAndTimeMin(@Param("user") String user);

    /**
     * 获取所有 Tokens
     * @return Tokens
     */
    List<YggdrasilToken> getYggdrasilTokensByAccessTokenNotNull();
}
