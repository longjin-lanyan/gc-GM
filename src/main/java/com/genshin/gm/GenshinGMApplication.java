package com.genshin.gm;

import com.genshin.gm.config.AppConfig;
import com.genshin.gm.config.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.awt.Desktop;
import java.net.URI;

/**
 * 原神 GM 指令生成系统 - 主启动类
 */
@SpringBootApplication
@EnableScheduling
public class GenshinGMApplication {
    private static final Logger logger = LoggerFactory.getLogger(GenshinGMApplication.class);

    public static void main(String[] args) {
        // 加载配置
        AppConfig config = ConfigLoader.loadConfig();

        // 设置服务器端口
        System.setProperty("server.port", String.valueOf(config.getFrontend().getPort()));

        SpringApplication.run(GenshinGMApplication.class, args);
    }

    /**
     * 应用启动完成后自动打开浏览器
     */
    @EventListener(ApplicationReadyEvent.class)
    public void openBrowser() {
        AppConfig config = ConfigLoader.getConfig();
        AppConfig.FrontendConfig frontend = config.getFrontend();

        if (!frontend.isAutoOpen()) {
            logger.info("自动打开浏览器已禁用");
            return;
        }

        String url = frontend.getUrl();
        logger.info("正在打开浏览器访问: {}", url);

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(new URI(url));
                    logger.info("浏览器已打开");
                } else {
                    logger.warn("当前系统不支持浏览器操作");
                    printManualUrl(url);
                }
            } else {
                logger.warn("当前系统不支持 Desktop 功能");
                printManualUrl(url);
            }
        } catch (Exception e) {
            logger.error("打开浏览器失败", e);
            printManualUrl(url);
        }
    }

    private void printManualUrl(String url) {
        logger.info("====================================");
        logger.info("请手动在浏览器中打开以下地址:");
        logger.info(url);
        logger.info("====================================");
    }
}