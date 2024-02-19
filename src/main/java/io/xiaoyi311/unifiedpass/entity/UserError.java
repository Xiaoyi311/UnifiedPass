package io.xiaoyi311.unifiedpass.entity;

/**
 * 反馈给用户的错误
 * @author xiaoyi311
 */
public class UserError extends Error {
    /**
     * 实例化
     * @param msg 信息
     */
    public UserError(String msg) {
        super(msg);
    }
}
