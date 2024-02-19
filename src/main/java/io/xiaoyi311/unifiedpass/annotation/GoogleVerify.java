package io.xiaoyi311.unifiedpass.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Google Recaptcha 验证
 * @author xiaoyi311
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GoogleVerify {
    /**
     * 所需分数级别
     */
    double value() default 0.7;
}
