package com.linweiyuan.chatgptapi.aop;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.linweiyuan.chatgptapi.misc.LogUtil.info;
import static com.linweiyuan.chatgptapi.misc.LogUtil.warn;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class CheckHeaderInterceptor implements HandlerInterceptor {

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        info(request.getRequestURI());

        if (request.getHeader(HttpHeaders.AUTHORIZATION) == null) {
            warn("No access token");
            return false;
        }

        return true;
    }
}
