package io.xiaoyi311.unifiedpass.controller;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.xiaoyi311.unifiedpass.OtherUtil;
import io.xiaoyi311.unifiedpass.entity.ResponseData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 主控制器
 * @author xiaoyi311
 */
@RestController
@Slf4j
@RequestMapping("/api")
public class RootController {
    @GetMapping
    public ResponseData apiInfo(){
        return ResponseData.def("The BackroomsMC Unified Pass Powered By Xiaoyi311 & BackroomsMC IT Group.");
    }

    @GetMapping("launcher")
    public JSONObject modsInfo(){
        File file = new File("mods.json");
        if(true) {
            ObjectMapper objectReader = new ObjectMapper();
            try {
                Map<String, Object> map = objectReader.readValue(file, Map.class);
                return new JSONObject(map);
            } catch (Exception e) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", e.getMessage());
                return jsonObject;
            }
        }
        return new JSONObject();
    }
}
