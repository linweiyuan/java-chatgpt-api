package com.linweiyuan.chatgptapi.config;

import com.linweiyuan.chatgptapi.aop.CheckHeaderInterceptor;
import com.linweiyuan.chatgptapi.aop.PreCheckInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final CheckHeaderInterceptor checkHeaderInterceptor;

    private final PreCheckInterceptor preCheckInterceptor;

    public WebMvcConfig(CheckHeaderInterceptor checkHeaderInterceptor, @Autowired(required = false) PreCheckInterceptor preCheckInterceptor) {
        this.checkHeaderInterceptor = checkHeaderInterceptor;
        this.preCheckInterceptor = preCheckInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(checkHeaderInterceptor).addPathPatterns("/**");
        if (preCheckInterceptor != null) {
            registry.addInterceptor(preCheckInterceptor).addPathPatterns("/**");
        }
    }
}
