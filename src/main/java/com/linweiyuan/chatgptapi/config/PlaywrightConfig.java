package com.linweiyuan.chatgptapi.config;

import com.linweiyuan.chatgptapi.annotation.EnabledOnChatGPT;
import com.linweiyuan.chatgptapi.exception.CaptchaException;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.Proxy;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import static com.linweiyuan.chatgptapi.enums.ErrorEnum.ACCESS_DENIED;
import static com.linweiyuan.chatgptapi.misc.Constant.CHATGPT_URL;
import static com.linweiyuan.chatgptapi.misc.Constant.WELCOME_TEXT;
import static com.linweiyuan.chatgptapi.misc.LogUtil.info;
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
        if (StringUtils.hasText(proxy)) {
            launchOptions = launchOptions.setProxy(new Proxy(proxy));
        }

        // Chromium can not get cf_cookies in headless mode
        Browser browser = Playwright.create()
                .firefox()
                .launch(launchOptions);

        var context = browser.newContext();
        context.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined});");

        Page page = context.newPage();
        page.navigate(CHATGPT_URL);
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.frames().forEach(frame -> frame.waitForLoadState(LoadState.NETWORKIDLE));

        if (isReady(page)) {
            info(WELCOME_TEXT);
            return page;
        }

        if (isAccessDenied(page)) {
            throw new CaptchaException(ACCESS_DENIED);
        }

        return handleCaptcha(page);
    }
}
