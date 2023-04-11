package com.linweiyuan.chatgptapi.scheduler;

import com.linweiyuan.chatgptapi.annotation.EnabledOnChatGPT;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.linweiyuan.chatgptapi.misc.Constant.PAGE_RELOAD_LOCK;
import static com.linweiyuan.chatgptapi.misc.PlaywrightUtil.handleCaptcha;
import static com.linweiyuan.chatgptapi.misc.PlaywrightUtil.isReady;

@EnabledOnChatGPT
@Component
public class PageAutoReloadScheduler {
    private final Page page;

    public PageAutoReloadScheduler(Page page) {
        this.page = page;
    }

    // auto reload every 1 minutes
    @Scheduled(fixedRate = 60_000, initialDelay = 60_000)
    public void reload() {
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
