package io.xiaoyi311.unifiedpass.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * CSRF 保护注解
 * @author xiaoyi311
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CsrfProtect {
    /**
     * 是否为构建模式
     */
    boolean value() default false;
}
