package com.linweiyuan.chatgptapi.config;

import com.linweiyuan.chatgptapi.misc.Constant;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;

@Configuration
public class WebDriverConfig {
    @SuppressWarnings("SpellCheckingInspection")
    @Bean
    WebDriver webDriver() throws IOException {
        // https://github.com/ultrafunkamsterdam/undetected-chromedriver
        System.setProperty("webdriver.chrome.driver", new ClassPathResource("undetected_chromedriver").getFile().getAbsolutePath());

        var chromeOptions = new ChromeOptions();

        // fix "Caused by: org.openqa.selenium.remote.http.ConnectionFailedException: Unable to establish websocket connection"
        chromeOptions.addArguments("--remote-allow-origins=*");

        // fake real browser
        chromeOptions.addArguments("--disable-blink-features=AutomationControlled");
        chromeOptions.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));

        chromeOptions.addArguments("--headless=new");

        var webDriver = new ChromeDriver(chromeOptions);
        webDriver.get(Constant.CHATGPT_URL);

        // use this to auto login chatgpt
        webDriver.manage().addCookie(new Cookie("__Secure-next-auth.session-token", System.getenv("SESSION_TOKEN")));

        webDriver.manage().timeouts().scriptTimeout(Duration.ofSeconds(Constant.SCRIPT_EXECUTION_TIMEOUT));

        return webDriver;
    }
}
