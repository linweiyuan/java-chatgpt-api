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

@Configuration
public class WebDriverConfig {
    @SneakyThrows
    @Bean
    WebDriver webDriver() {
        var chromeOptions = new ChromeOptions();

        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--disable-dev-shm-usage");
        chromeOptions.addArguments("--disable-blink-features=AutomationControlled");
        chromeOptions.addArguments("--headless=new");

        var networkProxyServer = System.getenv("NETWORK_PROXY_SERVER");
        if (networkProxyServer != null) {
            chromeOptions.addArguments("--proxy-server=" + networkProxyServer);
        }

        var webDriver = new RemoteWebDriver(new URL(System.getenv("CHATGPT_PROXY_SERVER")), chromeOptions);
        webDriver.get(Constant.CHATGPT_URL);
        CaptchaUtil.handleCaptcha(webDriver);
        webDriver.manage().timeouts().scriptTimeout(Duration.ofSeconds(Constant.SCRIPT_EXECUTION_TIMEOUT));

        return webDriver;
    }
}
