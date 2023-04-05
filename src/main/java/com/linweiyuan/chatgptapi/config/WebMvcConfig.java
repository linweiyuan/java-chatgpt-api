package com.linweiyuan.chatgptapi.config;

import com.linweiyuan.chatgptapi.aop.CheckHeaderInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final CheckHeaderInterceptor checkHeaderInterceptor;

    public WebMvcConfig(CheckHeaderInterceptor checkHeaderInterceptor) {
        this.checkHeaderInterceptor = checkHeaderInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(checkHeaderInterceptor).addPathPatterns("/**");
    }
}
