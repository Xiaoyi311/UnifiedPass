package io.xiaoyi311.unifiedpass;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * BackroomsMC 统一通行证
 * @author xiaoyi311
 */
@SpringBootApplication
@ServletComponentScan
@Slf4j
public class UnifiedPassApplication {
    public static final String VERSION = "1.0.0";

    /**
     * 启动
     */
    public static void main(String[] args) {
        SpringApplication.run(UnifiedPassApplication.class, args);
    }
}
