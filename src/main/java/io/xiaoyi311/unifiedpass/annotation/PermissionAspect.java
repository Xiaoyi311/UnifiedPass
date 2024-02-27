package io.xiaoyi311.unifiedpass.annotation;

import io.xiaoyi311.unifiedpass.entity.User;
import io.xiaoyi311.unifiedpass.entity.UserError;
import io.xiaoyi311.unifiedpass.service.OAuthService;
import io.xiaoyi311.unifiedpass.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;

/**
 * 权限注解切入器
 * @author xiaoyi311
 */
@Aspect
@Component
@Slf4j
public class PermissionAspect {
    @Value("${devMode}")
    private Boolean isDev;

    final HttpServletRequest request;
    final UserService userService;
    final OAuthService oAuthService;

    public PermissionAspect(HttpServletRequest request, UserService userService, OAuthService oAuthService) {
        this.request = request;
        this.userService = userService;
        this.oAuthService = oAuthService;
    }

    @Around("@annotation(permission)")
    public Object aroundMethod(ProceedingJoinPoint joinPoint, Permission permission) throws Throwable {
        //权限检验
        User user = userService.getUserBySession(request);
        if(user == null){
            //不启用 OAuth
            String header = request.getHeader("Authorization");
            if(header == null){
                throw new PermissionDeniedDataAccessException("Permission Denied", new UserError("lang:no_permission"));
            }

            String[] data = header.split(" ");
            if(permission.oauth().length == 0 && !"bearer".equals(data[0].toLowerCase(Locale.ROOT))){
                throw new PermissionDeniedDataAccessException("Permission Denied", new UserError("lang:no_permission"));
            }else{
                //启用 OAuth
                boolean hasPermission = false;
                String[] datas = oAuthService.getTokenInfo(data[1]);
                for (String per : permission.oauth()) {
                    if(Arrays.asList(datas[1].split(" ")).contains(per)){
                        hasPermission = true;
                        break;
                    }
                }

                if(!hasPermission){
                    throw new PermissionDeniedDataAccessException("Permission Denied", new UserError("lang:no_permission"));
                }

                user = userService.getUserById(datas[0]);
                if(user == null){
                    throw new PermissionDeniedDataAccessException("Permission Denied", new UserError("lang:no_permission"));
                }
            }
        }

        if(isDev){
            log.warn("!!!!!!!!=Permission Verify is disabled in dev mode=!!!!!!!!");
        }else{
            if(permission.value() && !user.admin){
                throw new PermissionDeniedDataAccessException("Permission Denied", new UserError("lang:no_permission"));
            }
        }

        //尝试注入 User 类型的参数
        Object[] objects = joinPoint.getArgs();
        for (int i = 0; i < objects.length; i++) {
            Object object = objects[i];
            if(object instanceof User){
                objects[i] = user;
            }
        }

        //注入方法
        return joinPoint.proceed(objects);
    }
}
