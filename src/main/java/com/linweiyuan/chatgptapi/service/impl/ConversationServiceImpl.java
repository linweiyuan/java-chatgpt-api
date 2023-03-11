package com.linweiyuan.chatgptapi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linweiyuan.chatgptapi.misc.Constant;
import com.linweiyuan.chatgptapi.model.GetConversationsResponse;
import com.linweiyuan.chatgptapi.service.ConversationService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class ConversationServiceImpl implements ConversationService {
    private final WebDriver webDriver;

    private final ObjectMapper objectMapper;

    private final AtomicInteger retryCount = new AtomicInteger(0);

    public ConversationServiceImpl(WebDriver webDriver, ObjectMapper objectMapper) {
        this.webDriver = webDriver;
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    @Override
    public ResponseEntity<GetConversationsResponse> getConversations(String accessToken, int offset, int limit) {
        JavascriptExecutor executor = (JavascriptExecutor) webDriver;

        var json = (String) executor.executeScript("""
                var xhr = new XMLHttpRequest();
                xhr.open('GET', '%s', false);
                xhr.setRequestHeader('Authorization', '%s');
                xhr.send();
                return xhr.responseText;
                """.formatted(String.format(Constant.GET_CONVERSATIONS_URL, offset, limit), accessToken));

        if (json.startsWith("<html>")) {
            var count = retryCount.incrementAndGet();
            webDriver.navigate().refresh();
            log.info("passive refresh: {}, retry count: {}", LocalDateTime.now(), count);

            if (count > Constant.MAXIMUM_RETRY_COUNT) {
                retryCount.set(0);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            }
            return getConversations(accessToken, offset, limit);
        } else {
            return ResponseEntity.ok(objectMapper.readValue(json, GetConversationsResponse.class));
        }
    }
}
