package io.xiaoyi311.unifiedpass.annotation;

import com.alibaba.fastjson.JSONObject;
import io.xiaoyi311.unifiedpass.OtherUtil;
import io.xiaoyi311.unifiedpass.entity.UserError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

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

        Map<String, String> args = new HashMap<>();
        args.put("secret", "6Lc7slgpAAAAADqVA2Tg88RYkMHqqvSANZMK4EPs");
        args.put("response", token);

        JSONObject result = OtherUtil.postArgs("https://www.recaptcha.net/recaptcha/api/siteverify", args);
        if(result == null){
            throw new PermissionDeniedDataAccessException("Google Server Error", new UserError("lang:google_error"));
        }

        if(!(Boolean) result.getOrDefault("success", false) && (Double) result.getOrDefault("score", 0.0) < verify.value()){
            throw new PermissionDeniedDataAccessException("Recaptcha Verify Failed", new UserError("lang:fail_recaptcha"));
        }

        return joinPoint.proceed();
    }
}
