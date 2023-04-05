package com.linweiyuan.chatgptapi.aop;

import com.linweiyuan.chatgptapi.annotation.EnabledOnChatGPT;
import com.linweiyuan.chatgptapi.annotation.PreCheck;
import com.linweiyuan.chatgptapi.misc.CaptchaUtil;
import com.linweiyuan.chatgptapi.misc.Constant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.ScriptTimeoutException;
import org.openqa.selenium.WebDriver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.linweiyuan.chatgptapi.misc.LogUtil.error;
import static com.linweiyuan.chatgptapi.misc.LogUtil.warn;

@EnabledOnChatGPT
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
        var precheck = method.getDeclaringClass().getAnnotation(PreCheck.class);
        if (precheck == null) {
            precheck = method.getAnnotation(PreCheck.class);
            if (precheck == null) {
                return true;
            }
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
        } catch (InvalidSelectorException e) {
            error("Failed to handle captcha: " + e);
        }

        return true;
    }
}
