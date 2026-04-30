package com.genshin.gm.controller;

import com.genshin.gm.entity.IpAccountRecord;
import com.genshin.gm.repository.IpAccountRecordRepository;
import com.genshin.gm.service.RegisterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/register")
@CrossOrigin(originPatterns = "*")
public class RegisterController {

    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);

    @Autowired private RegisterService registerService;
    @Autowired private IpAccountRecordRepository ipAccountRecordRepository;

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip;
    }

    /** POST /api/register/account — 玩家自助注册游戏账号 */
    @PostMapping("/account")
    public ResponseEntity<Map<String, Object>> registerAccount(
            @RequestBody Map<String, String> body, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        String username  = body.get("username");
        logger.info("收到注册请求 - IP: {}, 账号: {}", clientIp, username);
        return ResponseEntity.ok(registerService.registerGameAccount(username, clientIp));
    }

    /** GET /api/register/quota — 查询当前IP配额 */
    @GetMapping("/quota")
    public ResponseEntity<Map<String, Object>> getQuota(HttpServletRequest request) {
        return ResponseEntity.ok(registerService.getIpQuota(getClientIp(request)));
    }

    /**
     * GET /api/register/my-accounts
     * 返回当前 IP 已注册的所有账号名称和注册日期，供注册页展示。
     */
    @GetMapping("/my-accounts")
    public ResponseEntity<Map<String, Object>> getMyAccounts(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        List<IpAccountRecord> records = ipAccountRecordRepository.findAll().stream()
                .filter(r -> clientIp.equals(r.getIpAddress()))
                .sorted(Comparator.comparing(IpAccountRecord::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        List<Map<String, Object>> accounts = records.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("username",  r.getUsername());
            m.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success",  true);
        result.put("ip",       clientIp);
        result.put("accounts", accounts);
        result.put("used",     accounts.size());
        result.put("max",      3);
        return ResponseEntity.ok(result);
    }
}
