package com.genshin.gm.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * UA Interceptor: browser access to root → download page, app access → normal API
 * Admin page (/admin.html) is always accessible from browser
 */
@Component
public class UserAgentInterceptor implements HandlerInterceptor {

    private static final String APP_UA_PREFIX = "GenshinGM-Android";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI();
        String userAgent = request.getHeader("User-Agent");

        // API endpoints and admin page - always pass through
        if (path.startsWith("/api/") || path.startsWith("/admin")
                || path.startsWith("/data/") || path.startsWith("/download")) {
            return true;
        }

        return true;
    }
}
