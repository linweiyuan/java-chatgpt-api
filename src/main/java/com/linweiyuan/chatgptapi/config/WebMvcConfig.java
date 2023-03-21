package com.linweiyuan.chatgptapi.config;

import com.linweiyuan.chatgptapi.aop.PreCheckInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final PreCheckInterceptor preCheckInterceptor;

    public WebMvcConfig(PreCheckInterceptor preCheckInterceptor) {
        this.preCheckInterceptor = preCheckInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(preCheckInterceptor).addPathPatterns("/**");
    }
}
