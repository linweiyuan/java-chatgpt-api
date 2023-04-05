package com.linweiyuan.chatgptapi.scheduler;

import com.linweiyuan.chatgptapi.annotation.EnabledOnChatGPT;
import com.linweiyuan.chatgptapi.enums.ErrorEnum;
import com.linweiyuan.chatgptapi.exception.CaptchaException;
import com.microsoft.playwright.Page;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static com.linweiyuan.chatgptapi.misc.Constant.pageRefreshLock;
import static com.linweiyuan.chatgptapi.misc.LogUtil.error;
import static com.linweiyuan.chatgptapi.misc.LogUtil.warn;
import static com.linweiyuan.chatgptapi.misc.PlaywrightUtil.*;

@EnabledOnChatGPT
@Component
public class PageAutoReloadScheduler {
    private final Page page;

    public PageAutoReloadScheduler(Page page) {
        this.page = page;
    }

    // one minute
    @Scheduled(fixedRate = 1000 * 60, initialDelay = 1000 * 10)
    public void refresh() {
        if (pageRefreshLock.tryLock()) {
            page.reload();
            warn("Page reload: " + LocalDateTime.now());

            if (isAccessDenied(page)) {
                throw new CaptchaException(ErrorEnum.ACCESS_DENIED);
            }

            if (isAtCapacity(page)) {
                throw new CaptchaException(ErrorEnum.ACCESS_DENIED);
            }

            if (isWelcomed(page)) {
                warn("Reload done, no captcha.");
            } else {
                if (isCaptchaClicked(page)) {
                    warn("Reload done, captcha clicked.");
                } else {
                    error("Reload failed.");
                }
            }
        }
    }
}
