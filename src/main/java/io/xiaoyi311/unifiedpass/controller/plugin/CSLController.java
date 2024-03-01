package io.xiaoyi311.unifiedpass.controller.plugin;

import com.alibaba.fastjson.JSONObject;
import io.xiaoyi311.unifiedpass.entity.yggdrasil.YggdrasilProfile;
import io.xiaoyi311.unifiedpass.service.YggdrasilService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Custom Skin Loader API 控制器
 * @author xiaoyi311
 */
@RestController
@RequestMapping("/api/csl")
public class CSLController {
    YggdrasilService yggdrasilService;

    public CSLController(YggdrasilService yggdrasilService) {
        this.yggdrasilService = yggdrasilService;
    }

    /**
     * 获取玩家皮肤
     * @param player 玩家名
     * @return 皮肤
     */
    @GetMapping("{player}.json")
    public Object getPlayer(@PathVariable(required = false) String player) {
        YggdrasilProfile profile = yggdrasilService.getProfileFromName(player);
        if(profile == null) {
            return HttpStatus.NOT_FOUND;
        }

        JSONObject root = new JSONObject();
        root.put("username", profile.getName());

        JSONObject texture = new JSONObject();
        texture.put(profile.getModel(), profile.getSkin());

        String cape = profile.getUsingCape();
        if(!Objects.equals(cape, "NONE")){
            texture.put("cape", cape);
        }

        root.put("textures", texture);
        return root;
    }
}
