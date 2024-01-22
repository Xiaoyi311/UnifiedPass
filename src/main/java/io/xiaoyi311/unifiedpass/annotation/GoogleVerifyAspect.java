package io.xiaoyi311.unifiedpass.annotation;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.xiaoyi311.unifiedpass.entity.UserError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 权限注解切入器
 * @author xiaoyi311
 */
@Aspect
@Slf4j
@Component
public class GoogleVerifyAspect {
    @Autowired
    HttpServletRequest request;

    @Around("@annotation(verify)")
    public Object beforeMethod(ProceedingJoinPoint joinPoint, GoogleVerify verify) throws Throwable{
        String token = request.getHeader("Google_token");
        if(token == null){
            throw new PermissionDeniedDataAccessException("Request Recaptcha Verify", new UserError("lang:req_recaptcha"));
        }

        Map<String, String> parameters = new HashMap<>();
        parameters.put("secret", "6Lc7slgpAAAAADqVA2Tg88RYkMHqqvSANZMK4EPs");
        parameters.put("response", token);

        String form = parameters.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        HttpClient cli = HttpClient.newHttpClient();
        HttpResponse<String> response = cli.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("https://www.recaptcha.net/recaptcha/api/siteverify"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        JSONObject result = JSON.parseObject(response.body());
        if(!(Boolean) result.getOrDefault("success", false) && (Double) result.getOrDefault("score", 0.0) < verify.value()){
            throw new PermissionDeniedDataAccessException("Recaptcha Verify Failed", new UserError("lang:fail_recaptcha"));
        }

        return joinPoint.proceed();
    }
}
