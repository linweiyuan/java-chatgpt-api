package com.linweiyuan.chatgptapi.misc;

import com.linweiyuan.chatgptapi.enums.ErrorEnum;
import com.microsoft.playwright.*;
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
            if (title.isBlank()) {
                tryToClickCaptcha(page);
            } else if (title.equals("Just a moment...")) {
                saveScreenshot(page);
            }
            return true;
        } catch (PlaywrightException e) {
            return false;
        }
    }

    @SneakyThrows
    private static void tryToClickCaptcha(Page page) {
        Locator iframe = page.getByTitle("Widget containing a Cloudflare security challenge");

        warn("Waiting for captcha phase 1");
        while (!iframe.isVisible()) {
            TimeUnit.SECONDS.sleep(INTERVAL);
        }

        Frame frame = page.frames().get(2);
        ElementHandle content = frame.querySelector("#content");
        String style = content.getAttribute("style");
        if (style.equals("display: block;")) {
            warn("Waiting for captcha phase 2");
            while (!frame.getByText("Verify you are human").isVisible()) {
                TimeUnit.SECONDS.sleep(INTERVAL);
            }

            iframe.click();

            try {
                page.waitForCondition(() -> page.context().cookies().stream().anyMatch(cookie -> cookie.name.equals("cf_clearance")));
            } catch (TimeoutError error) {
                error(ErrorEnum.GET_CF_COOKIES_ERROR.message);
                saveScreenshot(page);
            }

            warn("Captcha should be clicked");
        } else if (style.equals("display: none;")) {
            page.reload();
        }
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
        warn("Failed to handle captcha, please copy this image from container to check what happens use below command");
        info("docker cp " + System.getenv("HOSTNAME") + ":" + fileName + " .");
        System.exit(1);
    }
}
