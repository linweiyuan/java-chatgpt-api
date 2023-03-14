package com.linweiyuan.chatgptapi.aop;

import com.linweiyuan.chatgptapi.annotation.PreCheck;
import com.linweiyuan.chatgptapi.misc.Constant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.FluentWait;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
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

                var captchaDetected = haveCaptcha();
                if (!captchaDetected) {
                    log.info("no captcha.");
                } else {
                    log.info("captcha is detected!!!");
                    tryToClickCaptchaTextBox();
                }
            }
        } catch (ScriptTimeoutException e) {
            log.error("passiveRefresh failed: {}", e.toString());
        }

        return true;
    }

    private FluentWait<WebDriver> newWait() {
        return new FluentWait<>(webDriver)
                .withTimeout(Duration.ofSeconds(10))
                .pollingEvery(Duration.ofSeconds(2))
                .ignoring(NoSuchElementException.class)
                .ignoring(TimeoutException.class);
    }

    private boolean haveCaptcha() {
        var wait = newWait();
        var welcomeElement = wait.until(driver -> driver.findElement(By.className("mb-2")));
        var welcomeText = welcomeElement.getText();
        log.info("welcome text: {}", welcomeText);
        return !"Welcome to ChatGPT".equals(welcomeText);
    }

    private void tryToClickCaptchaTextBox() {
        log.info("try to click captcha");
        var wait = newWait();
        WebElement checkbox = wait.until(driver -> driver.findElement(By.cssSelector("input[type=checkbox]")));
        if (checkbox.isDisplayed()) {
            log.info("captcha is displayed.");
            checkbox.click();
        } else {
            log.info("captcha is not displayed.");
        }
    }
}