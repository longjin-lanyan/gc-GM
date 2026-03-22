package com.genshin.gm.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * UA Interceptor:
 * - Android app (UA starts with GenshinGM-Android) → pass through
 * - PC browser accessing / or /index.html → redirect to /login.html
 * - /api/**, /data/**, /login.html, /download.html, /admin.html → always pass through
 */
@Component
public class UserAgentInterceptor implements HandlerInterceptor {

    private static final String APP_UA_PREFIX = "GenshinGM-Android";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI();
        String userAgent = request.getHeader("User-Agent");

        // Android app - always pass through
        if (userAgent != null && userAgent.startsWith(APP_UA_PREFIX)) {
            return true;
        }

        // PC browser: root or index.html → redirect to login page
        if ("/".equals(path) || "/index.html".equals(path)) {
            response.sendRedirect("/login.html");
            return false;
        }

        return true;
    }
}
