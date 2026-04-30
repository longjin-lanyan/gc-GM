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
                .excludePathPatterns(
                        "/api/**",          // 所有 API 接口
                        "/data/**",         // 静态数据文件
                        "/download.html",   // 下载页自身
                        "/register.html",   // 玩家注册页（安卓玩家需要访问）
                        "/html/register.html",
                        "/wiki.html"    //wiki页面（安卓玩家需要访问）
                );
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
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
