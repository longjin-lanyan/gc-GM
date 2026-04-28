package com.genshin.gm.util;

import com.genshin.gm.model.GameData;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据加载工具类
 * 负责从 txt 文件中读取游戏数据
 */
@Component
public class DataLoader {
    
    private static final String DATA_DIR = "data/txt/";
    
    /**
     * 从指定文件加载数据
     * @param filename 文件名（如：Item.txt）
     * @return 数据映射 Map<ID, 名称>
     */
    public Map<Integer, String> loadDataAsMap(String filename) {
        Map<Integer, String> dataMap = new HashMap<>();
        File file = new File(DATA_DIR + filename);
        
        if (!file.exists()) {
            System.err.println("文件不存在: " + file.getAbsolutePath());
            return dataMap;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                // 移除UTF-8 BOM字符（如果存在于第一行）
                if (isFirstLine) {
                    if (line.length() > 0 && line.charAt(0) == '\uFEFF') {
                        line = line.substring(1);
                    }
                    isFirstLine = false;
                }

                line = line.trim();
                // 跳过空行和注释行
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // 解析格式: ID:名称
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    try {
                        Integer id = Integer.parseInt(parts[0].trim());
                        String name = parts[1].trim();
                        dataMap.put(id, name);
                    } catch (NumberFormatException e) {
                        System.err.println("无效的数据格式: " + line);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("读取文件失败: " + e.getMessage());
        }
        
        return dataMap;
    }
    
    /**
     * 从指定文件加载数据为列表
     * @param filename 文件名
     * @return 数据列表
     */
    public List<GameData> loadDataAsList(String filename) {
        List<GameData> dataList = new ArrayList<>();
        Map<Integer, String> dataMap = loadDataAsMap(filename);
        
        dataMap.forEach((id, name) -> {
            dataList.add(new GameData(id, name));
        });
        
        // 按 ID 排序
        dataList.sort((a, b) -> a.getId().compareTo(b.getId()));
        
        return dataList;
    }
    
    /**
     * 验证数据目录是否存在
     */
    public boolean validateDataDirectory() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            return dir.mkdirs();
        }
        return true;
    }
}