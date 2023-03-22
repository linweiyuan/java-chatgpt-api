package com.linweiyuan.chatgptapi.misc;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;

@Slf4j
public class CaptchaUtil {
    public static void handleCaptcha(WebDriver webDriver) {
        try {
            var captchaDetected = CaptchaUtil.haveCaptcha(webDriver);
            if (!captchaDetected) {
                log.info("no captcha.");
            } else {
                log.info("captcha is detected!!!");
                tryToClickCaptchaTextBox(webDriver);
            }
        } catch (Exception e) {
            log.error("Failed to handle captcha: {}", e.toString());
        }
    }

    private static boolean haveCaptcha(WebDriver webDriver) {
        var wait = newWait(webDriver);
        try {
            var welcomeElement = wait.until(driver -> driver.findElement(By.className("mb-2")));
            var welcomeText = welcomeElement.getText();
            log.info("welcome text: {}", welcomeText);
            return !"Welcome to ChatGPT".equals(welcomeText);
        } catch (Exception e) {
            return true;
        }
    }

    private static void tryToClickCaptchaTextBox(WebDriver webDriver) {
        log.info("try to click captcha");
        webDriver = webDriver.switchTo().frame(0);
        var wait = newWait(webDriver);
        WebElement checkbox = wait.until(driver -> driver.findElement(By.cssSelector("input[type=checkbox]")));
        checkbox.click();
        log.info("captcha is clicked.");
    }

    private static FluentWait<WebDriver> newWait(WebDriver webDriver) {
        return new FluentWait<>(webDriver)
                .withTimeout(Duration.ofSeconds(10))
                .pollingEvery(Duration.ofSeconds(2))
                .ignoring(NoSuchElementException.class)
                .ignoring(TimeoutException.class);
    }
}
