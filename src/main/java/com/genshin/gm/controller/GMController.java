package com.genshin.gm.controller;

import com.genshin.gm.model.GameData;
import com.genshin.gm.service.GMService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GM 指令生成 REST API 控制器
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(originPatterns = "*")
public class GMController {
    
    @Autowired
    private GMService gmService;
    
    /**
     * 获取物品列表
     */
    @GetMapping("/items")
    public List<GameData> getItems() {
        return gmService.getItems();
    }
    
    /**
     * 获取武器列表
     */
    @GetMapping("/weapons")
    public List<GameData> getWeapons() {
        return gmService.getWeapons();
    }
    
    /**
     * 获取角色列表
     */
    @GetMapping("/avatars")
    public List<GameData> getAvatars() {
        return gmService.getAvatars();
    }
    
    /**
     * 获取任务列表
     */
    @GetMapping("/quests")
    public List<GameData> getQuests() {
        return gmService.getQuests();
    }
    
    /**
     * 生成物品给予指令
     */
    @PostMapping("/command/give")
    public Map<String, Object> generateGiveCommand(@RequestBody Map<String, Object> params) {
        Integer itemId = (Integer) params.get("itemId");
        Integer quantity = (Integer) params.get("quantity");
        
        String command = gmService.generateGiveCommand(itemId, quantity);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("command", command);
        return response;
    }
    
    /**
     * 生成任务添加指令
     */
    @PostMapping("/command/quest/add")
    public Map<String, Object> generateQuestAddCommand(@RequestBody Map<String, Object> params) {
        Integer questId = (Integer) params.get("questId");
        
        String command = gmService.generateQuestAddCommand(questId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("command", command);
        return response;
    }
    
    /**
     * 生成任务完成指令
     */
    @PostMapping("/command/quest/finish")
    public Map<String, Object> generateQuestFinishCommand(@RequestBody Map<String, Object> params) {
        Integer questId = (Integer) params.get("questId");
        
        String command = gmService.generateQuestFinishCommand(questId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("command", command);
        return response;
    }
}