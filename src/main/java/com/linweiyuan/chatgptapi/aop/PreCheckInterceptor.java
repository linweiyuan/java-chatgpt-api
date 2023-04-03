package com.linweiyuan.chatgptapi.aop;

import com.linweiyuan.chatgptapi.annotation.PreCheck;
import com.linweiyuan.chatgptapi.misc.CaptchaUtil;
import com.linweiyuan.chatgptapi.misc.Constant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.ScriptTimeoutException;
import org.openqa.selenium.WebDriver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.linweiyuan.chatgptapi.misc.LogUtil.*;

@Component
public class PreCheckInterceptor implements HandlerInterceptor {
    private final WebDriver webDriver;

    public PreCheckInterceptor(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        var method = ((HandlerMethod) handler).getMethod();
        if (method.getDeclaringClass().getAnnotation(PreCheck.class) == null) {
            if (method.getAnnotation(PreCheck.class) == null) {
                return true;
            }
        }

        info(request.getRequestURI());

        if (request.getHeader(HttpHeaders.AUTHORIZATION) == null) {
            warn("No access token");
            return false;
        }

        try {
            var status = (Long) ((JavascriptExecutor) webDriver).executeScript("""
                    const xhr = new XMLHttpRequest();
                    xhr.open('GET', '%s', false);
                    xhr.send();
                    return xhr.status;
                    """.formatted(Constant.CHATGPT_URL));

            if (status == HttpStatus.FORBIDDEN.value()) {
                warn("Session timeout, need to refresh");

                webDriver.navigate().refresh();

                CaptchaUtil.handleCaptcha(webDriver);
            }
        } catch (ScriptTimeoutException e) {
            error("Refresh failed: " + e);
        }

        return true;
    }
}
