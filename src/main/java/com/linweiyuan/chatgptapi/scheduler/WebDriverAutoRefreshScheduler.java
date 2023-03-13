package com.linweiyuan.chatgptapi.scheduler;

import com.linweiyuan.chatgptapi.misc.Constant;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.FluentWait;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
public class WebDriverAutoRefreshScheduler {
    private final WebDriver webDriver;

    public WebDriverAutoRefreshScheduler(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    @Scheduled(fixedRate = 1000 * 60 * 3, initialDelay = 1000 * 10)
    public void activeRefresh() {
        webDriver.navigate().refresh();
        log.debug("active refresh: {}", LocalDateTime.now());
    }

    @SuppressWarnings("PointlessArithmeticExpression")
    @Scheduled(fixedRate = 1000 * 60 * 1, initialDelay = 1000 * 10)
    public void passiveRefresh() {
        try {
            var status = (Long) ((JavascriptExecutor) webDriver).executeAsyncScript("""
                    var callback = arguments[arguments.length - 1];
                    var xhr = new XMLHttpRequest();
                    xhr.open('GET', '%s', false);
                    xhr.onreadystatechange = function() {
                        callback(xhr.status);
                    }
                    xhr.send();
                    """.formatted(Constant.CHATGPT_URL));
            if (status == HttpStatus.FORBIDDEN.value()) {
                log.debug("passive refresh: {}", LocalDateTime.now());

                webDriver.navigate().refresh();

                var captchaDetected = haveCaptcha();
                if (!captchaDetected) {
                    log.debug("no captcha.");
                } else {
                    log.debug("captcha is detected!!!");
                    tryToClickCaptchaTextBox();
                }
            }
        } catch (ScriptTimeoutException e) {
            log.error("passiveRefresh failed: {}", e.toString());
        }
    }

    private boolean haveCaptcha() {
        var wait = new FluentWait<>(webDriver)
                .withTimeout(Duration.ofSeconds(30))
                .pollingEvery(Duration.ofSeconds(5))
                .ignoring(NoSuchElementException.class)
                .ignoring(TimeoutException.class);
        var welcomeElement = wait.until(driver -> driver.findElement(By.className("mb-2")));
        var welcomeText = welcomeElement.getText();
        return !"Welcome to ChatGPT".equals(welcomeText);
    }

    private void tryToClickCaptchaTextBox() {
        log.debug("try to click captcha");
        var wait = new FluentWait<>(webDriver)
                .withTimeout(Duration.ofSeconds(30))
                .pollingEvery(Duration.ofSeconds(5))
                .ignoring(NoSuchElementException.class);
        WebElement checkbox = wait.until(driver -> driver.findElement(By.cssSelector("input[type=checkbox]")));
        if (checkbox.isDisplayed()) {
            log.debug("captcha is displayed.");
            checkbox.click();
        } else {
            log.debug("captcha is not displayed.");
        }
    }
}