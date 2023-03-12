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

    // just like healthCheck, to avoid cloudflare 403
    @Scheduled(fixedRate = 1000 * 60 * 3, initialDelay = 1000 * 10)
    public void test() {
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
            log.info("need to refresh: {}", LocalDateTime.now());

            webDriver.navigate().refresh();

            WebElement element = new FluentWait<>(webDriver)
                    .withTimeout(Duration.ofSeconds(30))
                    .pollingEvery(Duration.ofSeconds(5))
                    .ignoring(NoSuchElementException.class)
                    .until(driver -> driver.findElement(By.className("mb-2")));
            if (element.getText().equals("Welcome to ChatGPT")) {
                log.info("no captcha.");
            } else {
                log.info("captcha is detected!!!");

                WebElement checkbox = new FluentWait<>(webDriver)
                        .withTimeout(Duration.ofSeconds(30))
                        .pollingEvery(Duration.ofSeconds(5))
                        .ignoring(NoSuchElementException.class)
                        .until(driver -> driver.findElement(By.cssSelector("input[type=checkbox]")));
                checkbox.click();
            }
        }
    }
}
