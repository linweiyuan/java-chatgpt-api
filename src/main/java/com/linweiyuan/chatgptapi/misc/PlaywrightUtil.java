package com.linweiyuan.chatgptapi.misc;

import com.linweiyuan.chatgptapi.enums.ErrorEnum;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.AriaRole;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import static com.linweiyuan.chatgptapi.misc.LogUtil.*;

public class PlaywrightUtil {
    private static boolean isFirstTimeRun = true;
    private static final int INTERVAL = 1;

    public static boolean isAccessDenied(Page page) {
        try {
            var element = page.waitForSelector(".cf-error-details", new Page.WaitForSelectorOptions().setTimeout(2000));
            error(element.textContent());
            return true;
        } catch (PlaywrightException e) {
            page.waitForTimeout(1000);
        }
        return false;
    }

    public static boolean isAtCapacity(Page page) {
        try {
            var element = page.waitForSelector(".text-3xl", new Page.WaitForSelectorOptions().setTimeout(3000));
            error(element.textContent());
            return true;
        } catch (PlaywrightException e) {
            return false;
        }
    }

    public static boolean isWelcomed(Page page) {
        try {
            var element = page.waitForSelector(".mb-2", new Page.WaitForSelectorOptions().setTimeout(10000));
            var text = element.textContent();

            var isWelcomed = "Welcome to ChatGPT".equals(text);
            if (isFirstTimeRun && isWelcomed) {
                info("No");
                info(text);
                isFirstTimeRun = false;
            }
            return isWelcomed;
        } catch (PlaywrightException e) {
            return false;
        }
    }

    @SneakyThrows
    public static boolean isCaptchaClicked(Page page) {
        try {
            var title = page.title();
            if (title.isBlank() || title.equals("Just a moment...")) {
                tryToClickCaptcha(page);
            }
            return true;
        } catch (PlaywrightException e) {
            return false;
        }
    }

    @SneakyThrows
    private static void tryToClickCaptcha(Page page) {
        var iframe = page.getByTitle("Widget containing a Cloudflare security challenge");
        while (!iframe.isVisible()) {
            TimeUnit.SECONDS.sleep(INTERVAL);
        }
        page.frames().stream()
                .filter(frame -> frame.url().startsWith("https://challenges.cloudflare.com"))
                .findFirst()
                .ifPresentOrElse(PlaywrightUtil::clickCheckBox, iframe::click);
        try {
            page.waitForCondition(() -> page.context().cookies().stream().anyMatch(cookie -> cookie.name.equals("cf_clearance")), new Page.WaitForConditionOptions().setTimeout(5_000));
        } catch (TimeoutError error) {
            error(ErrorEnum.GET_CF_COOKIES_ERROR.message);

            saveScreenshot(page);

            page.reload();
            handleCaptcha(page);
        }
    }

    @SneakyThrows
    private static void clickCheckBox(Frame frame) {
        var checkbox = frame.getByRole(AriaRole.CHECKBOX);
        while (!checkbox.isVisible()) {
            TimeUnit.SECONDS.sleep(INTERVAL);
        }
        checkbox.check();
    }

    public static Page handleCaptcha(Page page) {
        if (isCaptchaClicked(page) && isWelcomed(page)) {
            return page;
        } else {
            return handleCaptcha(page);
        }
    }

    public static void saveScreenshot(Page page) throws IOException {
        var fileName = "/tmp/captcha-" + DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss").format(LocalDateTime.now(ZoneId.of("Asia/Shanghai"))) + ".png";
        Files.write(Path.of(fileName), page.screenshot());
        warn("Failed to handle captcha, please use below command to copy the screenshot from container to check what happens");
        info("docker cp " + System.getenv("HOSTNAME") + ":" + fileName + " .");
    }
}
