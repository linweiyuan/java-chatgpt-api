package com.linweiyuan.chatgptapi.scheduler;

import com.linweiyuan.chatgptapi.annotation.EnabledOnChatGPT;
import com.linweiyuan.chatgptapi.enums.ErrorEnum;
import com.linweiyuan.chatgptapi.exception.CaptchaException;
import com.microsoft.playwright.Page;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.linweiyuan.chatgptapi.misc.Constant.PAGE_RELOAD_LOCK;
import static com.linweiyuan.chatgptapi.misc.LogUtil.error;
import static com.linweiyuan.chatgptapi.misc.PlaywrightUtil.*;

@EnabledOnChatGPT
@Component
public class PageAutoReloadScheduler {
    private final Page page;

    public PageAutoReloadScheduler(Page page) {
        this.page = page;
    }

    // auto reload every 3 minutes if not in conversation
    @Scheduled(fixedRate = 1000 * 60 * 3, initialDelay = 1000 * 10)
    public void refresh() {
        if (PAGE_RELOAD_LOCK.tryLock()) {
            try {
                page.reload();

                if (isAccessDenied(page)) {
                    throw new CaptchaException(ErrorEnum.ACCESS_DENIED);
                }

                if (isAtCapacity(page)) {
                    throw new CaptchaException(ErrorEnum.ACCESS_DENIED);
                }

                if (!isWelcomed(page)) {
                    handleCaptcha(page);
                }
            } catch (Exception e) {
                error("Reload failed: " + e.getMessage());
            } finally {
                PAGE_RELOAD_LOCK.unlock();
            }
        }
    }
}
