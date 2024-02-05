package io.xiaoyi311.unifiedpass;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.xiaoyi311.unifiedpass.entity.ResponseData;
import io.xiaoyi311.unifiedpass.entity.UserError;
import io.xiaoyi311.unifiedpass.entity.yggdrasil.YggdrasilError;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 工具类
 * @author xiaoyi311
 */
@Slf4j
public class OtherUtil {
    /**
     * 获取真实 Ip
     * @param request 请求
     * @return Ip
     */
    public static String getRealIp(HttpServletRequest request){
        String ip = request.getHeader("X-Real-IP");
        if("".equals(ip) || "empty".equals(ip) || ip == null){
            ip = request.getHeader("X-Forwarded-For");
        }
        if("".equals(ip) || "empty".equals(ip) || ip == null){
            ip = request.getRemoteAddr();
        }
        if("".equals(ip) || "empty".equals(ip) || ip == null){
            log.warn("Couldn't get real ip!");
            return null;
        }
        return ip;
    }

    /**
     * 是否为合法字符串
     * @param str 字符串
     * @return 是否有效
     */
    public static boolean isSuccessStr(String str) {
        String pattern = "^[a-zA-Z0-9_]+$";

        Pattern regexPattern = Pattern.compile(pattern);
        Matcher matcher = regexPattern.matcher(str);

        return !matcher.matches();
    }

    /**
     * 统一解析返回
     * @param body 消息
     * @param debug 调试
     * @return 统一返回
     */
    public static ResponseData getResponse(Object body, boolean debug){
        ResponseData data = new ResponseData();

        if(body instanceof ProblemDetail p){
            data.setStatus(p.getStatus());
            data.setData(debug ? p : p.getTitle());
        } else if (body instanceof ClassCastException) {
            data.setStatus(400);
            data.setData("Wrong Args");
        } else if (body instanceof IllegalArgumentException) {
            data.setStatus(400);
            data.setData("Wrong Args");
        } else if (body instanceof PermissionDeniedDataAccessException e) {
            data.setStatus(403);
            data.setData(e.getCause().getMessage());
        } else if (body instanceof ServletException e) {
            if(e.getCause() instanceof UserError error){
                data.setStatus(200);
                data.setData(error.getMessage());
            } else if (e.getCause() instanceof YggdrasilError error) {
                data.setStatus(error.status);
                data.setData(error.getJsonData());
            } else {
                data.setStatus(500);
                if(debug){
                    log.trace("Servlet Exception", e);
                }
                data.setData(e.getMessage());
            }
        } else if (body instanceof Exception e) {
            data.setStatus(500);
            if(debug){
                log.trace("Servlet Exception", e);
            }
            data.setData(e.getMessage());
        } else if (body instanceof HttpStatus s) {
            data.setStatus(s.value());
            data.setData(null);
        } else if (body instanceof ResponseData r) {
            return r;
        } else {
            data.setStatus(200);
            data.setData(body);
        }

        return data;
    }

    /**
     * RSA 签名
     * @param privateKeyStr 私钥
     * @param data 数据
     * @return 签名
     */
    public static String rsaSign(String privateKeyStr, String data){
        try{
            privateKeyStr = privateKeyStr.replaceAll("\n", "");
            privateKeyStr = privateKeyStr.replace("-----BEGIN PRIVATE KEY-----", "");
            privateKeyStr = privateKeyStr.replace("-----END PRIVATE KEY-----", "");
            byte[] decodedBytes = Base64.getDecoder().decode(privateKeyStr);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decodedBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(spec);
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initSign(privateKey);
            signature.update(data.getBytes());
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e){
            return "=== ERROR SIGN SYSTEM ===";
        }
    }

    /**
     * 生成 SHA256
     * @param data 数据
     * @return SHA256
     */
    public static String sha256(String data){
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder stringBuilder = new StringBuilder();
            String temp;
            for (byte aByte : digest.digest()){
                temp = Integer.toHexString(aByte & 0xFF);
                if (temp.length() == 1){
                    stringBuilder.append("0");
                }
                stringBuilder.append(temp);
            }
            return stringBuilder.toString();
        }catch (Exception e){
            return "=== ERROR ENCODE SHA256 ===";
        }
    }

    /**
     * POST 带参请求
     * @author xiaoyi311
     */
    public static JSONObject postArgs(String url, Map<String, String> args){
        String form = args.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        HttpClient cli = HttpClient.newHttpClient();
        HttpResponse<String> response;
        try {
            response = cli.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(form))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );
        } catch (IOException | InterruptedException e) {
            return null;
        }

        return JSON.parseObject(response.body());
    }

    /**
     * POST 带 JSON 请求
     * @author xiaoyi311
     */
    public static JSONObject postJson(String url, String json){
        HttpClient cli = HttpClient.newHttpClient();
        HttpResponse<String> response;
        try {
            response = cli.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Content-Type", "application/json")
                            .header("Accept", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(json))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );
        } catch (IOException | InterruptedException e) {
            return null;
        }

        return JSON.parseObject(response.body());
    }
}
