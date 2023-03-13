package com.linweiyuan.chatgptapi.config;

import com.linweiyuan.chatgptapi.misc.Constant;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;

@Configuration
public class WebDriverConfig {
    @Bean
    WebDriver webDriver() throws IOException {
        var chromeOptions = new ChromeOptions();

        // fake real browser
        chromeOptions.addArguments("--disable-blink-features=AutomationControlled");
        chromeOptions.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));

        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-gpu");
//        chromeOptions.addArguments("--headless=new");

        chromeOptions.addArguments("--proxy-server=127.0.0.1:20171");

        var webDriver = new RemoteWebDriver(new URL(System.getenv("WEB_DRIVER_URL")), chromeOptions);
        webDriver.get(Constant.CHATGPT_URL);

        webDriver.manage().timeouts().scriptTimeout(Duration.ofSeconds(Constant.SCRIPT_EXECUTION_TIMEOUT));

        return webDriver;
    }
}
