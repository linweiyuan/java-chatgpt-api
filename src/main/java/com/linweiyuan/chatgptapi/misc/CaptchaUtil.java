package com.linweiyuan.chatgptapi.misc;

import lombok.SneakyThrows;
import lombok.val;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.linweiyuan.chatgptapi.misc.LogUtil.*;

public class CaptchaUtil {
    public static boolean checkAccess(WebDriver webDriver) {
        try {
            var wait = new FluentWait<>(webDriver)
                    .withTimeout(Duration.ofSeconds(Constant.CHECK_ACCESS_DENIED_TIMEOUT))
                    .pollingEvery(Duration.ofSeconds(Constant.CHECK_CAPTCHA_INTERVAL))
                    .ignoring(NoSuchElementException.class)
                    .ignoring(TimeoutException.class);
            var errorDetails = wait.until(driver -> driver.findElement(By.className("cf-error-details")));
            LogUtil.error(errorDetails.getText());
            return false;
        } catch (Exception e) {
            return true;
        }
    }

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
            webDriver.navigate().refresh();
            handleCaptcha(webDriver);
        }
    }

    private static boolean haveCaptcha(WebDriver webDriver) {
        var wait = new FluentWait<>(webDriver)
                .withTimeout(Duration.ofSeconds(Constant.CHECK_WELCOME_TEXT_TIMEOUT))
                .pollingEvery(Duration.ofSeconds(Constant.CHECK_CAPTCHA_INTERVAL))
                .ignoring(NoSuchElementException.class)
                .ignoring(TimeoutException.class);
        try {
            var welcomeElement = wait.until(driver -> driver.findElement(By.className("mb-2")));
            var welcomeText = welcomeElement.getText();
            info(welcomeText);
            return !"Welcome to ChatGPT".equals(welcomeText);
        } catch (Exception e) {
            return true;
        }
    }

    @SneakyThrows
    private static void tryToClickCaptchaTextBox(WebDriver webDriver) {
        info("Try to click captcha");
        webDriver = webDriver.switchTo().frame(0);
        var wait = new FluentWait<>(webDriver)
                .withTimeout(Duration.ofSeconds(Constant.CHECK_CAPTCHA_TIMEOUT))
                .pollingEvery(Duration.ofSeconds(Constant.CHECK_CAPTCHA_INTERVAL))
                .ignoring(NoSuchElementException.class)
                .ignoring(TimeoutException.class);
        var checkbox = wait.until(driver -> driver.findElement(By.cssSelector("input[type=checkbox]")));
        checkbox.click();
        info("Captcha is clicked!");

        TimeUnit.SECONDS.sleep(Constant.CHECK_NEXT_INTERVAL);

        val title = webDriver.getTitle();
        info(title);
        if (title.equals("Just a moment...")) {
            info("Still get a captcha");

            tryToClickCaptchaTextBox(webDriver);
        }
    }
}
