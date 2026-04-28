package com.genshin.gm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.genshin.gm.config.AppConfig;
import com.genshin.gm.config.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Grasscutter OpenCommand API 调用服务
 */
@Service
public class GrasscutterApiService {

    private static final Logger logger = LoggerFactory.getLogger(GrasscutterApiService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AppConfig.GrasscutterConfig gcConfig;

    public GrasscutterApiService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.gcConfig = ConfigLoader.getConfig().getGrasscutter();
    }

    /**
     * 获取完整 API URL
     */
    private String getApiUrl() {
        String serverUrl = gcConfig.getServerUrl();
        String apiPath = gcConfig.getApiPath();
        if (serverUrl.endsWith("/")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
        }
        if (!apiPath.startsWith("/")) {
            apiPath = "/" + apiPath;
        }
        return serverUrl + apiPath;
    }

    /**
     * 执行控制台命令
     * @param command 要执行的命令
     * @param serverUuid 多服务器时的目标UUID，为null则发送到主Dispatch
     * @return API 响应
     */
    public Map<String, Object> executeCommand(String command, String serverUuid) {
        Map<String, Object> result = new HashMap<>();

        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("token", gcConfig.getConsoleToken());
            payload.put("action", "command");

            if (serverUuid != null && !serverUuid.isBlank()) {
                payload.put("server", serverUuid);
            }

            payload.put("data", command);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(payload.toString(), headers);
            String apiUrl = getApiUrl();

            logger.info("调用 Grasscutter API: {} | 命令: {}", apiUrl, command);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JsonNode responseNode = objectMapper.readTree(response.getBody());
            int retcode = responseNode.get("retcode").asInt();
            String message = responseNode.has("message") ? responseNode.get("message").asText() : "";
            String data = responseNode.has("data") && !responseNode.get("data").isNull()
                    ? responseNode.get("data").asText()
                    : "";

            result.put("retcode", retcode);
            result.put("message", message);
            result.put("data", data);
            result.put("success", retcode == 200);

            if (retcode == 200) {
                logger.info("命令执行成功: {} → {}", command, data);
            } else {
                logger.warn("命令执行失败: {} → {} (retcode: {})", command, message, retcode);
            }

        } catch (Exception e) {
            logger.error("API 调用异常: {}", e.getMessage(), e);
            result.put("retcode", 500);
            result.put("message", "连接 Grasscutter 服务器失败: " + e.getMessage());
            result.put("data", "");
            result.put("success", false);
        }

        return result;
    }

    /**
     * 测试连接
     */
    public Map<String, Object> ping() {
        return executeCommand("ping", null);
    }

    /**
     * 获取多服务器列表
     */
    public Map<String, Object> getServerList() {
        Map<String, Object> result = new HashMap<>();
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("token", gcConfig.getConsoleToken());
            payload.put("action", "server");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(payload.toString(), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    getApiUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JsonNode responseNode = objectMapper.readTree(response.getBody());
            result.put("retcode", responseNode.get("retcode").asInt());
            result.put("data", responseNode.get("data"));

        } catch (Exception e) {
            logger.error("获取服务器列表失败: {}", e.getMessage());
            result.put("retcode", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}