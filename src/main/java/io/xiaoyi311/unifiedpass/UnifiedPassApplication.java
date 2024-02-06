package io.xiaoyi311.unifiedpass;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
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
        new SpringApplicationBuilder(UnifiedPassApplication.class)
                .beanNameGenerator(new CustomBeanNameGenerator())
                .run(args);
    }

    /**
     * 自定义 Bean 生成器
     */
    private static class CustomBeanNameGenerator implements BeanNameGenerator {
        @Override
        public String generateBeanName(BeanDefinition d, BeanDefinitionRegistry r) {
            return d.getBeanClassName();
        }
    }
}
