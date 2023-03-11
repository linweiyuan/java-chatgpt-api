package com.linweiyuan.chatgptapi;

import com.linweiyuan.loggerspringbootstarter.annotation.EnableExceptionLog;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SuppressWarnings("SpellCheckingInspection")
@EnableExceptionLog
@EnableScheduling
@SpringBootApplication
public class ChatgptapiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatgptapiApplication.class, args);
    }
}
