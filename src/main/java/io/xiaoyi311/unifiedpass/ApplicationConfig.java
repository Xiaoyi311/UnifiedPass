package io.xiaoyi311.unifiedpass;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.alibaba.fastjson.support.spring.FastJsonRedisSerializer;
import io.xiaoyi311.unifiedpass.service.YggdrasilService;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.MediaType;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.ArrayList;
import java.util.List;

/**
 * 程序配置
 * @author xiaoyi311
 */
@Configuration
public class ApplicationConfig implements WebMvcConfigurer {
    /**
     * JSON 序列化配置
     */
    @Bean
    public HttpMessageConverters fastJsonHttpMessageConverters(){
        FastJsonHttpMessageConverter fConverter = new FastJsonHttpMessageConverter();
        FastJsonConfig config = new FastJsonConfig();
        config.setSerializerFeatures(SerializerFeature.PrettyFormat);
        List<MediaType> supportMediaType = new ArrayList<>();
        supportMediaType.add(MediaType.APPLICATION_JSON);
        fConverter.setSupportedMediaTypes(supportMediaType);
        fConverter.setFastJsonConfig(config);
        return new HttpMessageConverters(fConverter);
    }

    /**
     * Redis 数据配置
     */
    @Bean
    public RedisTemplate<String, YggdrasilService.SessionCheck> redisTemplateSc(RedisConnectionFactory factory){
        RedisTemplate<String, YggdrasilService.SessionCheck> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new FastJsonRedisSerializer<>(YggdrasilService.SessionCheck.class));
        return redisTemplate;
    }

    /**
     * 新增跨域映射
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowCredentials(true)
                .allowedMethods("GET", "POST", "OPTIONS", "PUT", "DELETE")
                .maxAge(3600);
    }

    /**
     * 新增静态资源访问
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/static/**")
                .addResourceLocations("classpath/static/static/**")
                .resourceChain(true);
        registry.addResourceHandler("/textures/**")
                .addResourceLocations("file:" + System.getProperty("user.dir") + "/texture/");
    }

    /**
     * 新增页面展示控制器
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/")
                .setViewName("forward:/index.html");
    }

    @Bean
    public MultipartConfigElement multipartConfigElement(){
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.parse("5MB"));
        factory.setMaxRequestSize(DataSize.parse("2MB"));
        return factory.createMultipartConfig();
    }
}
