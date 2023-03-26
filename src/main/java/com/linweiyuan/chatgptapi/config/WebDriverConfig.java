package com.linweiyuan.chatgptapi.config;

import com.linweiyuan.chatgptapi.misc.CaptchaUtil;
import com.linweiyuan.chatgptapi.misc.Constant;
import lombok.SneakyThrows;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URL;
import java.time.Duration;
import java.util.Collections;

@Configuration
public class WebDriverConfig {
    @SneakyThrows
    @Bean
    WebDriver webDriver() {
        var chromeOptions = new ChromeOptions();

        // fake real browser
        chromeOptions.addArguments("--disable-blink-features=AutomationControlled");
        chromeOptions.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));

        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--disable-dev-shm-usage");
        chromeOptions.addArguments("--disable-blink-features=AutomationControlled");
        chromeOptions.addArguments("--headless=new");

        var webDriver = new RemoteWebDriver(new URL(System.getenv("CHATGPT_PROXY_SERVER")), chromeOptions);
        webDriver.get(Constant.CHATGPT_URL);
        CaptchaUtil.handleCaptcha(webDriver);
        webDriver.manage().timeouts().scriptTimeout(Duration.ofSeconds(Constant.SCRIPT_EXECUTION_TIMEOUT));

        return webDriver;
    }
}
