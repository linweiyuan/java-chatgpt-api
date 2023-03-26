package com.linweiyuan.chatgptapi.misc;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;

import static com.linweiyuan.chatgptapi.misc.LogUtil.*;

public class CaptchaUtil {
    public static void handleCaptcha(WebDriver webDriver) {
        try {
            var captchaDetected = CaptchaUtil.haveCaptcha(webDriver);
            if (!captchaDetected) {
                info("No captcha");
            } else {
                warn("Captcha is detected");
                tryToClickCaptchaTextBox(webDriver);
            }
        } catch (Exception e) {
            error("Failed to handle captcha: " + e);
            System.exit(1);
        }
    }

    private static boolean haveCaptcha(WebDriver webDriver) {
        var wait = newWait(webDriver);
        try {
            var welcomeElement = wait.until(driver -> driver.findElement(By.className("mb-2")));
            var welcomeText = welcomeElement.getText();
            info(welcomeText);
            return !"Welcome to ChatGPT".equals(welcomeText);
        } catch (Exception e) {
            return true;
        }
    }

    private static void tryToClickCaptchaTextBox(WebDriver webDriver) {
        info("Try to click captcha");
        webDriver = webDriver.switchTo().frame(0);
        var wait = newWait(webDriver);
        var checkbox = wait.until(driver -> driver.findElement(By.cssSelector("input[type=checkbox]")));
        checkbox.click();
        info("Captcha is clicked!");
    }

    private static FluentWait<WebDriver> newWait(WebDriver webDriver) {
        return new FluentWait<>(webDriver)
                .withTimeout(Duration.ofSeconds(10))
                .pollingEvery(Duration.ofSeconds(2))
                .ignoring(NoSuchElementException.class)
                .ignoring(TimeoutException.class);
    }
}
