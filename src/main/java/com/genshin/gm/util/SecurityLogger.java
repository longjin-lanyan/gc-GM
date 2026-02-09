package com.genshin.gm.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPOutputStream;

/**
 * 安全日志工具
 * - errohuman.txt: 仅记录违规操作（UID伪造/越权等）
 * - all.txt: 全局操作日志，每日自动压缩归档并重建
 */
public class SecurityLogger {

    private static final Logger logger = LoggerFactory.getLogger(SecurityLogger.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Path LOG_DIR = Paths.get("log");
    private static final Path ERROR_HUMAN_FILE = LOG_DIR.resolve("errohuman.txt");
    private static final Path ALL_LOG_FILE = LOG_DIR.resolve("all.txt");

    // 记录当前日志文件对应的日期，用于判断是否需要轮转
    private static volatile String currentLogDate;

    static {
        try {
            Files.createDirectories(LOG_DIR);
            currentLogDate = LocalDate.now().format(DATE_FORMATTER);
        } catch (IOException e) {
            logger.error("无法创建日志目录: {}", LOG_DIR, e);
        }
    }

    /**
     * 检查是否需要进行日志轮转（跨天）
     * 如果当前日期与日志文件日期不同，压缩旧的 all.txt 并创建新的
     */
    private static synchronized void checkAndRotateDaily() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        if (today.equals(currentLogDate)) {
            return;
        }

        // 需要轮转：压缩昨天的 all.txt → all-yyyy-MM-dd.txt.gz
        try {
            if (Files.exists(ALL_LOG_FILE) && Files.size(ALL_LOG_FILE) > 0) {
                Path archiveFile = LOG_DIR.resolve("all-" + currentLogDate + ".txt.gz");
                compressFile(ALL_LOG_FILE, archiveFile);
                Files.delete(ALL_LOG_FILE);
                logger.info("日志轮转完成: all.txt → {}", archiveFile.getFileName());
            }
        } catch (IOException e) {
            logger.error("日志轮转失败", e);
        }

        currentLogDate = today;
    }

    /**
     * 将文件压缩为 GZIP 格式
     */
    private static void compressFile(Path source, Path target) throws IOException {
        try (InputStream in = Files.newInputStream(source);
             OutputStream out = new GZIPOutputStream(Files.newOutputStream(target))) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        }
    }

    /**
     * 记录UID伪造/越权尝试到 errohuman.txt（仅违规记录）
     *
     * @param ip             客户端IP
     * @param username       当前登录的用户名（可能为null）
     * @param boundUids      该账户绑定的UID列表
     * @param attemptedUid   尝试使用的UID
     * @param command        尝试执行的指令
     * @param detail         额外描述
     */
    public static void logUnauthorizedUidAttempt(String ip, String username,
                                                  String boundUids, String attemptedUid,
                                                  String command, String detail) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        StringBuilder sb = new StringBuilder();
        sb.append("========== 异常操作记录 ==========\n");
        sb.append("时间: ").append(timestamp).append("\n");
        sb.append("IP: ").append(ip).append("\n");
        sb.append("账号: ").append(username != null ? username : "未登录").append("\n");
        sb.append("账号绑定的UID: ").append(boundUids != null ? boundUids : "无").append("\n");
        sb.append("尝试操作的UID: ").append(attemptedUid).append("\n");
        sb.append("尝试执行的指令: ").append(command != null ? command : "无").append("\n");
        sb.append("描述: ").append(detail).append("\n");
        sb.append("==================================\n\n");

        String content = sb.toString();
        appendToFile(ERROR_HUMAN_FILE, content);

        // 同时写入全局日志
        logAction(ip, username, attemptedUid, "SECURITY_VIOLATION", detail);

        logger.warn("[SECURITY] UID伪造尝试 - IP: {}, 账号: {}, 绑定UID: {}, 尝试UID: {}, 描述: {}",
                ip, username, boundUids, attemptedUid, detail);
    }

    /**
     * 记录一般操作日志到 all.txt（全局日志，每日轮转）
     */
    public static void logAction(String ip, String username, String uid, String action, String detail) {
        checkAndRotateDaily();

        String timestamp = LocalDateTime.now().format(FORMATTER);
        String line = String.format("[%s] IP: %s | 账号: %s | UID: %s | 操作: %s | 详情: %s\n",
                timestamp, ip, username != null ? username : "-", uid != null ? uid : "-", action, detail);
        appendToFile(ALL_LOG_FILE, line);
    }

    private static synchronized void appendToFile(Path file, String content) {
        try {
            Files.writeString(file, content, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            logger.error("写入日志文件失败: {}", file, e);
        }
    }
}
