package io.xiaoyi311.unifiedpass.entity;

import lombok.Data;

/**
 * 统一返回数据
 * @author xiaoyi311
 */
@Data
public class ResponseData {
    /**
     * 状态码
     */
    public int status;

    /**
     * 时间戳
     */
    public long time = System.currentTimeMillis();

    /**
     * 数据
     */
    public Object data;

    /**
     * 生成返回数据
     * @param data 返回数据
     * @return 统一返回
     */
    public static ResponseData def(Object data){
        ResponseData rep = new ResponseData();
        rep.setData(data);
        rep.setStatus(200);
        return rep;
    }
}
