package io.xiaoyi311.unifiedpass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * BackroomsMC 统一通行证
 * @author xiaoyi311
 */
@SpringBootApplication
public class UnifiedPassApplication{
    public static final String VERSION = "1.0.0";

    /**
     * 启动
     */
    public static void main(String[] args) {
        SpringApplication.run(UnifiedPassApplication.class, args);
    }
}
