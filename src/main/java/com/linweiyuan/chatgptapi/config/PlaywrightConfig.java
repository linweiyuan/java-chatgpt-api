package com.linweiyuan.chatgptapi.config;

import com.linweiyuan.chatgptapi.annotation.EnabledOnChatGPT;
import com.linweiyuan.chatgptapi.enums.ErrorEnum;
import com.linweiyuan.chatgptapi.exception.CaptchaException;
import com.linweiyuan.chatgptapi.misc.Constant;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
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
        // Chromium not work
        Browser browser = Playwright.create()
                .firefox()
                .launch(
                        new BrowserType.LaunchOptions()
                                .setHeadless(true)
                );
        Page page = browser.newPage();
        page.navigate(Constant.CHATGPT_URL);

        warn("There's a lot of checking to do, please wait...");
        warn("Access denied?");
        if (isAccessDenied(page)) {
            throw new CaptchaException(ErrorEnum.ACCESS_DENIED);
        }
        info("No");

        warn("At capacity?");
        if (isAtCapacity(page)) {
            throw new CaptchaException(ErrorEnum.ACCESS_DENIED);
        }
        info("No");

        warn("Needs handle captcha?");
        if (isWelcomed(page)) {
            return page;
        }
        error("Yes");

        warn("Try to handle captcha");
        if (isCaptchaClicked(page)) {
            return page;
        }

        throw new CaptchaException(ErrorEnum.HANDLE_CAPTCHA_ERROR);
    }
}
