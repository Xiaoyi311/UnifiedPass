package io.xiaoyi311.unifiedpass.entity.yggdrasil;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Yggdrasil 错误信息
 * @author xiaoyi311
 */
@AllArgsConstructor
public class YggdrasilError extends Error{
    /**
     * 状态码
     */
    public int status;

    /**
     * 错误的简要描述（机器可读）
     */
    public String error;

    /**
     * 错误的详细信息（人类可读）
     */
    public String errorMessage;

    /**
     * 该错误的原因（可选）
     */
    public String cause;

    /**
     * 获取 JSON 数据
     * @return JSON 数据
     */
    public JSONObject getJsonData(){
        JSONObject root = new JSONObject();
        root.put("error", error);
        root.put("errorMessage", errorMessage);
        root.put("cause", cause);
        return root;
    }

    /**
     * 错误标准
     */
    @Getter
    public enum Errors{
        /**
         * 令牌无效
         */
        TOKEN_INVALID(new YggdrasilError(
                403,
                "ForbiddenOperationException",
                "Invalid token.",
                null
        )),

        /**
         * 密码错误，或短时间内多次登录失败而被暂时禁止登录
         */
        AUTH_FAILED(new YggdrasilError(
                403,
                "ForbiddenOperationException",
                "Invalid credentials. Invalid username or password.",
                null
        )),

        /**
         * 试图向一个已经绑定了角色的令牌指定其要绑定的角色
         */
        PROFILE_BIND(new YggdrasilError(
                403,
                "IllegalArgumentException",
                "Access token already has a profile assigned.",
                null
        )),

        /**
         * 试图向一个令牌绑定不属于其对应用户的角色 （非标准）
         */
        PROFILE_NOT_FOUND(new YggdrasilError(
                403,
                "ForbiddenOperationException",
                "Profile does not belong to this token",
                null
        )),

        /**
         * 试图使用一个错误的角色加入服务器
         */
        PLAYER_INVALID(new YggdrasilError(
                403,
                "ForbiddenOperationException",
                "Invalid token.",
                null
        ));

        /**
         * 错误信息
         */
        final YggdrasilError msg;

        /**
         * 实例化
         * @param msg 错误信息
         */
        Errors(YggdrasilError msg){
            this.msg = msg;
        }
    }
}
