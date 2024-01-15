package io.xiaoyi311.unifiedpass.annotation;

import io.xiaoyi311.unifiedpass.entity.User;
import io.xiaoyi311.unifiedpass.entity.UserError;
import io.xiaoyi311.unifiedpass.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.stereotype.Component;

/**
 * 权限注解切入器
 * @author xiaoyi311
 */
@Aspect
@Component
public class PermissionAspect {
    @Autowired
    HttpServletRequest request;

    @Autowired
    UserService userService;

    @Around("@annotation(permission)")
    public Object beforeMethod(ProceedingJoinPoint joinPoint, Permission permission) throws Throwable {
        //权限检验
        User user = userService.getUserBySession(request);
        if(user == null || permission.value() && !user.admin){
            throw new PermissionDeniedDataAccessException("Permission Denied", new UserError("lang:no_permission"));
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
