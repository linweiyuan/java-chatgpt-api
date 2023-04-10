package com.linweiyuan.chatgptapi.config;

import com.linweiyuan.chatgptapi.annotation.EnabledOnChatGPT;
import com.linweiyuan.chatgptapi.enums.ErrorEnum;
import com.linweiyuan.chatgptapi.exception.CaptchaException;
import com.linweiyuan.chatgptapi.misc.Constant;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.Proxy;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.linweiyuan.chatgptapi.misc.LogUtil.*;
import static com.linweiyuan.chatgptapi.misc.PlaywrightUtil.*;

@SuppressWarnings("resource")
@EnabledOnChatGPT
@Configuration
public class PlaywrightConfig {
    @SneakyThrows
    @Bean
    Page page() {
        var launchOptions = new BrowserType.LaunchOptions().setHeadless(true);
        var proxy = System.getenv("PROXY");
        if (proxy != null && !proxy.isBlank()) {
            launchOptions = launchOptions.setProxy(new Proxy(proxy));
        }

        // Chromium can not get cf_cookies in headless mode
        Browser browser = Playwright.create()
                .firefox()
                .launch(launchOptions);

        var context = browser.newContext();
        context.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined});");

        Page page = context.newPage();
        page.navigate(Constant.CHATGPT_URL);
        page.waitForLoadState(LoadState.NETWORKIDLE);

        warn("There's a lot of checking to do, please wait...");
        warn("Access denied?");
        if (isAccessDenied(page)) {
            throw new CaptchaException(ErrorEnum.ACCESS_DENIED);
        }
        info("No");

        warn("At capacity?");
        if (isAtCapacity(page)) {
            throw new CaptchaException(ErrorEnum.AT_CAPACITY);
        }
        info("No");

        warn("Needs handle captcha?");
        if (isWelcomed(page)) {
            return page;
        }
        error("Yes");

        warn("Try to handle captcha");
        return handleCaptcha(page);
    }
}
