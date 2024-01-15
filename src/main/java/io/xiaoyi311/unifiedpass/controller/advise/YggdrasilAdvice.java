package io.xiaoyi311.unifiedpass.controller.advise;

import io.xiaoyi311.unifiedpass.OtherUtil;
import io.xiaoyi311.unifiedpass.entity.ResponseData;
import io.xiaoyi311.unifiedpass.entity.yggdrasil.YggdrasilError;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Yggdrasil 返回统一处理
 * @author xiaoyi311
 */
@Order(0)
@RestControllerAdvice
public class YggdrasilAdvice implements ResponseBodyAdvice<Object> {
    @Value("${debug}")
    private boolean debug;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.getMember().getDeclaringClass().getName().contains("Yggdrasil");
    }

    @ExceptionHandler(YggdrasilError.class)
    public Throwable exceptionCatch(Exception e){
        return e;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        ResponseData data = OtherUtil.getResponse(body, debug);
        response.setStatusCode(HttpStatusCode.valueOf(data.getStatus()));
        return data.getData();
    }
}
