package com.linweiyuan.chatgptapi.misc;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;

import static com.linweiyuan.chatgptapi.misc.LogUtil.*;

public class PlaywrightUtil {
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
            info(text);
            return "Welcome to ChatGPT".equals(text);
        } catch (PlaywrightException e) {
            return false;
        }
    }

    public static boolean isCaptchaClicked(Page page) {
        try {
            page.waitForTimeout(15000);
            // have no idea how to get iframe's data back, not like selenium
            page.querySelector("#challenge-form input").click();
            warn("Captcha should be clicked");
            return true;
        } catch (PlaywrightException e) {
            return false;
        }
    }
}
