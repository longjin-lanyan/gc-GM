package com.genshin.gm.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

/**
 * Web配置类 - 配置静态资源映射、CORS和UA拦截
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private UserAgentInterceptor userAgentInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userAgentInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/api/**", "/data/**", "/login.html", "/download.html", "/admin.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /data/** 映射到 data 目录（用于背景图片等静态资源）
        registry.addResourceHandler("/data/**")
                .addResourceLocations("file:data/");

        // 将根路径映射到 html 目录
        registry.addResourceHandler("/**")
                .addResourceLocations("file:html/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 将根路径重定向到 index.html
        registry.addViewController("/").setViewName("forward:/index.html");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 配置CORS，允许所有来源访问API（包括proto端点）
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
