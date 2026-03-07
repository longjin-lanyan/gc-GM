package com.genshin.gm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * 配置加载器
 */
public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    private static final String CONFIG_FILE = "config.json";
    private static AppConfig appConfig;

    /**
     * 加载配置文件
     */
    public static AppConfig loadConfig() {
        if (appConfig != null) {
            return appConfig;
        }

        try {
            File configFile = new File(CONFIG_FILE);
            if (!configFile.exists()) {
                logger.warn("配置文件 {} 不存在，使用默认配置", CONFIG_FILE);
                appConfig = new AppConfig();
                appConfig.setFrontend(new AppConfig.FrontendConfig());
                return appConfig;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            appConfig = objectMapper.readValue(configFile, AppConfig.class);

            // 如果配置文件中没有MySQL配置，使用默认配置
            if (appConfig.getMysql() == null) {
                logger.warn("配置文件中未找到MySQL配置，使用默认配置");
                appConfig.setMysql(new AppConfig.MySQLConfig());
            }

            logger.info("成功加载配置文件: {}", CONFIG_FILE);
            logger.info("前端地址: {}:{}", appConfig.getFrontend().getHost(),
                       appConfig.getFrontend().getPort());
            logger.info("Grasscutter API: {}", appConfig.getGrasscutter().getFullUrl());
            logger.info("Grasscutter 超时: {}ms", appConfig.getGrasscutter().getTimeout());
            logger.info("MySQL连接: {}:{}/{}",
                       appConfig.getMysql().getHost(),
                       appConfig.getMysql().getPort(),
                       appConfig.getMysql().getDatabase());

            return appConfig;
        } catch (IOException e) {
            logger.error("加载配置文件失败，使用默认配置", e);
            appConfig = new AppConfig();
            appConfig.setFrontend(new AppConfig.FrontendConfig());
            return appConfig;
        }
    }

    /**
     * 获取配置实例
     */
    public static AppConfig getConfig() {
        if (appConfig == null) {
            return loadConfig();
        }
        return appConfig;
    }
}
