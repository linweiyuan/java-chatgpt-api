package com.linweiyuan.chatgptapi.misc;

import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import lombok.SneakyThrows;

import java.util.concurrent.TimeUnit;

import static com.linweiyuan.chatgptapi.misc.Constant.PAGE_RELOAD_LOCK;
import static com.linweiyuan.chatgptapi.misc.Constant.WELCOME_TEXT;
import static com.linweiyuan.chatgptapi.misc.LogUtil.*;

public class PlaywrightUtil {
    private static final int INTERVAL = 1;
    private static int totalClickCaptchaCount = 0;

    private static boolean firstTime = true;

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

    public static boolean isReady(Page page) {
        page.waitForLoadState(LoadState.NETWORKIDLE);
        return page.title().contains("ChatGPT");
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
                .ifPresentOrElse(PlaywrightUtil::clickCheckBox, () -> {
                    iframe.click();
                    totalClickCaptchaCount++;
                });

        try {
            page.waitForCondition(() -> page.context().cookies().stream().anyMatch(cookie -> cookie.name.equals("cf_clearance")), new Page.WaitForConditionOptions().setTimeout(5_000));
        } catch (TimeoutError error) {
            page.reload();
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.frames().forEach(frame -> frame.waitForLoadState(LoadState.NETWORKIDLE));
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
        totalClickCaptchaCount++;
    }

    public static Page handleCaptcha(Page page) {
        if (isCaptchaClicked(page) && isReady(page)) {
            if (totalClickCaptchaCount != 0) {
                warn("Total click " + totalClickCaptchaCount + " times to pass captcha");
                totalClickCaptchaCount = 0;
            }
            if (firstTime) {
                info(WELCOME_TEXT);
                firstTime = false;
            }
            return page;
        } else {
            return handleCaptcha(page);
        }
    }

    public static void tryToReload(Page page) {
        if (PAGE_RELOAD_LOCK.tryLock()) {
            try {
                page.reload();
                page.waitForLoadState(LoadState.NETWORKIDLE);

                if (!isReady(page)) {
                    handleCaptcha(page);
                }
            } catch (Exception e) {
                // when in 10th minute, reload will fail, but after reload again here, then it will works fine
                page.reload();
            } finally {
                PAGE_RELOAD_LOCK.unlock();
            }
        }
    }
}
