package com.genshin.gm.util;

import com.genshin.gm.model.GameData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 数据加载工具类
 * 从 data/txt/ 下的文本文件读取游戏数据。
 *
 * 文件格式：每行 "ID:名称"，支持以下注释/跳过规则：
 *   - 空行                → 跳过
 *   - 以 # 开头           → 跳过（单行注释）
 *   - 以 // 开头          → 跳过（单行注释）
 *   - ID 非纯数字         → 跳过（如 GachaBanner 中的字母前缀行）
 *   - UTF-8 BOM           → 自动去除
 */
@Component
public class DataLoader {

    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);
    private static final String DATA_DIR = "data/txt/";

    /**
     * 从指定文件加载数据为 Map<ID, 名称>
     */
    public Map<Integer, String> loadDataAsMap(String filename) {
        Map<Integer, String> dataMap = new LinkedHashMap<>();
        File file = new File(DATA_DIR + filename);

        if (!file.exists()) {
            logger.error("数据文件不存在: {}", file.getAbsolutePath());
            return dataMap;
        }

        int lineNo = 0, parsed = 0, skipped = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                lineNo++;

                // 去除 UTF-8 BOM（仅首行可能出现）
                if (lineNo == 1 && !line.isEmpty() && line.charAt(0) == '\uFEFF') {
                    line = line.substring(1);
                }

                line = line.trim();

                // 跳过空行、# 注释、// 注释
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                    skipped++;
                    continue;
                }

                // 解析 ID:名称
                int colonIdx = line.indexOf(':');
                if (colonIdx <= 0) {
                    skipped++;
                    continue;
                }

                String idStr   = line.substring(0, colonIdx).trim();
                String name    = line.substring(colonIdx + 1).trim();

                // ID 必须是纯数字（跳过字母前缀的行，如 GachaBanner 的 A007）
                if (!idStr.matches("\\d+")) {
                    skipped++;
                    continue;
                }

                try {
                    int id = Integer.parseInt(idStr);
                    // 去重：同一 ID 保留首次出现的值
                    dataMap.putIfAbsent(id, name);
                    parsed++;
                } catch (NumberFormatException e) {
                    // ID 超出 int 范围（Quest 中存在超长 ID）—— 跳过，不报错
                    skipped++;
                }
            }

        } catch (IOException e) {
            logger.error("读取文件失败: {}", filename, e);
        }

        logger.info("加载 {} 完成: {} 条有效, {} 行跳过 (共 {} 行)", filename, parsed, skipped, lineNo);
        return dataMap;
    }

    /**
     * 从指定文件加载数据为列表，按 ID 升序排列
     */
    public List<GameData> loadDataAsList(String filename) {
        List<GameData> list = new ArrayList<>();
        loadDataAsMap(filename).forEach((id, name) -> list.add(new GameData(id, name)));
        list.sort(Comparator.comparingInt(GameData::getId));
        return list;
    }

    /**
     * 验证并创建数据目录
     */
    public boolean validateDataDirectory() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            boolean ok = dir.mkdirs();
            if (!ok) logger.error("无法创建数据目录: {}", dir.getAbsolutePath());
            return ok;
        }
        return true;
    }
}
