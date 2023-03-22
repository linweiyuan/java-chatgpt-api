package com.linweiyuan.chatgptapi.aop;

import com.linweiyuan.chatgptapi.annotation.PreCheck;
import com.linweiyuan.chatgptapi.misc.CaptchaUtil;
import com.linweiyuan.chatgptapi.misc.Constant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.ScriptTimeoutException;
import org.openqa.selenium.WebDriver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

@Slf4j
@Component
public class PreCheckInterceptor implements HandlerInterceptor {
    private final WebDriver webDriver;

    public PreCheckInterceptor(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (((HandlerMethod) handler).getMethod().getAnnotation(PreCheck.class) == null) {
            return true;
        }

        if (request.getHeader(HttpHeaders.AUTHORIZATION) == null) {
            log.warn("no access token");
            return false;
        }

        try {
            var status = (Long) ((JavascriptExecutor) webDriver).executeScript("""
                    const xhr = new XMLHttpRequest();
                    xhr.open('GET', '%s', false);
                    xhr.send();
                    return xhr.status;
                    """.formatted(Constant.PRE_CHECK_URL));

            log.info("pre check status: {}", status);

            if (status == HttpStatus.FORBIDDEN.value()) {
                log.info("passive refresh: {}", LocalDateTime.now());

                webDriver.navigate().refresh();

                CaptchaUtil.handleCaptcha(webDriver);
            }
        } catch (ScriptTimeoutException e) {
            log.error("passiveRefresh failed: {}", e.toString());
        }

        return true;
    }
}
