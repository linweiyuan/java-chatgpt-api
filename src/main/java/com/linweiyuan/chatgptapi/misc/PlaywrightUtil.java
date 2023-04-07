package com.linweiyuan.chatgptapi.misc;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import lombok.SneakyThrows;

import java.util.concurrent.TimeUnit;

import static com.linweiyuan.chatgptapi.misc.LogUtil.*;

public class PlaywrightUtil {
    private static boolean isFirstTimeRun = true;

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
            Locator byTitle = page.getByTitle("Widget containing a Cloudflare security challenge");

            warn("Waiting for captcha phase 1");
            while (!byTitle.isVisible()) {
                TimeUnit.SECONDS.sleep(1);
            }

            warn("Waiting for captcha phase 2");
            while (page.frames().stream().noneMatch(frame -> frame.getByText("Verify you are human").isVisible())) {
                TimeUnit.SECONDS.sleep(1);
            }

            byTitle.click();

            warn("Captcha should be clicked");
            return true;
        } catch (PlaywrightException e) {
            return false;
        }
    }

    public static Page handleCaptcha(Page page) {
        if (isCaptchaClicked(page) && isWelcomed(page)) {
            return page;
        } else {
            return handleCaptcha(page);
        }
    }
}
