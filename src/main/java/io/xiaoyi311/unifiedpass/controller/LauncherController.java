package io.xiaoyi311.unifiedpass.controller;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/launcher")
public class LauncherController {
    @GetMapping
    public JSONObject modsInfo(){
        File file = new File("mods.json");
        if(file.exists()) {
            ObjectMapper objectReader = new ObjectMapper();
            try {
                return new JSONObject(objectReader.readValue(file, Map.class));
            } catch (IOException e) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", e.getMessage());
                return jsonObject;
            }
        }
        return new JSONObject();
    }
}
