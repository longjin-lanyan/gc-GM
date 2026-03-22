package com.genshin.gm.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * UA Interceptor:
 * - Mobile/Android browser → redirect to /download.html
 * - PC browser → pass through normally
 */
@Component
public class UserAgentInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && isMobile(userAgent)) {
            response.sendRedirect("/download.html");
            return false;
        }
        return true;
    }

    private boolean isMobile(String ua) {
        String lower = ua.toLowerCase();
        return lower.contains("android") || lower.contains("iphone")
                || lower.contains("ipad") || lower.contains("mobile");
    }
}
