package io.xiaoyi311.unifiedpass.annotation;

import io.xiaoyi311.unifiedpass.OtherUtil;
import io.xiaoyi311.unifiedpass.entity.UserError;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.stereotype.Component;

/**
 * CSRF 防御注解切入器
 * @author xiaoyi311
 */
@Aspect
@Slf4j
@Component
public class CsrfProtectAspect {
    @Value("${devMode}")
    private Boolean isDev;

    final HttpServletRequest request;
    final HttpServletResponse response;
    final String key = OtherUtil.randomString(16);
    final long TIMEOUT = 5 * 60 * 1000;

    public CsrfProtectAspect(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    @Around("@annotation(csrfProtect)")
    public Object aroundMethod(ProceedingJoinPoint joinPoint, CsrfProtect csrfProtect) throws Throwable {
        if(!csrfProtect.value()){
            if(isDev){
                log.warn("!!!!!!!!=Csrf Protect is disabled in dev mode=!!!!!!!!");
                return joinPoint.proceed();
            }

            //CSRF 检验
            String data = request.getHeader("X-Csrf-Token");
            if(data == null){
                throw new PermissionDeniedDataAccessException("Not Safe Request", new UserError("lang:no_safe"));
            }

            try{
                long time = Long.parseLong(OtherUtil.aesDecrypt(data, key));
                if(System.currentTimeMillis() - time > TIMEOUT){
                    throw new PermissionDeniedDataAccessException("Not Safe Request", new UserError("lang:no_safe"));
                }
            }catch (Exception e){
                throw new PermissionDeniedDataAccessException("Not Safe Request", new UserError("lang:no_safe"));
            }
        }else{
            Cookie cookie = new Cookie("Csrf-Token", OtherUtil.aesEncrypt(String.valueOf(System.currentTimeMillis()), key));
            cookie.setPath("/");
            response.addCookie(cookie);
        }

        //注入方法
        return joinPoint.proceed();
    }
}
