package com.linweiyuan.chatgptapi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linweiyuan.chatgptapi.misc.Constant;
import com.linweiyuan.chatgptapi.model.*;
import com.linweiyuan.chatgptapi.service.ConversationService;
import io.netty.util.internal.StringUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
        var executor = (JavascriptExecutor) webDriver;

        var responseText = (String) executor.executeScript("""
                var xhr = new XMLHttpRequest();
                xhr.open('GET', '%s', false);
                xhr.setRequestHeader('Authorization', '%s');
                xhr.send();
                return xhr.responseText;
                """.formatted(String.format(Constant.GET_CONVERSATIONS_URL, offset, limit), accessToken));

        if (responseText.startsWith("<html>")) {
            var count = retryCount.incrementAndGet();
            if (count <= Constant.MAXIMUM_RETRY_COUNT) {
                webDriver.navigate().refresh();
                TimeUnit.SECONDS.sleep(1);
                log.info("passive refresh: {}, retry count: {}", LocalDateTime.now(), count);
                return getConversations(accessToken, offset, limit);
            }
            retryCount.set(0);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } else {
            return ResponseEntity.ok(objectMapper.readValue(responseText, GetConversationsResponse.class));
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    @SneakyThrows
    @Override
    public Flux<StartConversationResponse> startConversation(String accessToken, StartConversationRequest startConversationRequest) {
        var executor = (JavascriptExecutor) webDriver;

        var conversationId = startConversationRequest.conversationId();
        var requestMap = new HashMap<>(Map.of(
                "action", "next",
                "messages", Collections.singletonList(
                        new Message(
                                new Author("user"),
                                new Content("text", Collections.singletonList(startConversationRequest.content())),
                                UUID.randomUUID().toString(),
                                "user"
                        )),
                "model", Constant.MODEL,
                "parent_message_id", startConversationRequest.parentMessageId()
        ));
        if (!StringUtil.isNullOrEmpty(conversationId)) {
            requestMap.put("conversation_id", conversationId);
        }

        var jsonBody = objectMapper.writeValueAsString(requestMap);

        executor.executeScript("""
                var xhr = new XMLHttpRequest();
                xhr.open('POST', '%s', true);
                xhr.setRequestHeader('Accept', 'text/event-stream');
                xhr.setRequestHeader('Authorization', '%s');
                xhr.setRequestHeader('Content-Type', 'application/json');
                xhr.onreadystatechange = function() {
                    if (xhr.readyState === 3 && xhr.status === 200) {
                        window.postMessage(xhr.responseText);
                    } else if (xhr.status === 429) {
                        window.postMessage("429");
                    }
                }
                xhr.send('%s');
                """.formatted(Constant.START_CONVERSATIONS_URL, accessToken, jsonBody));

        return Flux.create(fluxSink -> {
            var executorService = Executors.newSingleThreadExecutor();
            executorService.submit(() -> {
                while (true) {
                    try {
                        var eventData = (String) executor.executeAsyncScript("""
                                var callback = arguments[arguments.length - 1];
                                var handleFunction = function(event) {
                                    callback(event.data);
                                };
                                window.removeEventListener('message', handleFunction);
                                window.addEventListener('message', handleFunction);
                                """);
                        // how to return a customized message to response
                        if (eventData.equals("429")) {
                            fluxSink.complete();
                            break;
                        }

                        var last = Arrays.stream(eventData.split("\n\n"))
                                .map(event -> event.replaceAll("data: ", ""))
                                .reduce((first, second) -> second)
                                .orElse("")
                                .trim();
                        if (last.equals("[DONE]") || last.isBlank()) {
                            fluxSink.complete();
                            break;
                        }
                        fluxSink.next(objectMapper.readValue(last, StartConversationResponse.class));
                    } catch (Exception e) {
                        fluxSink.complete();
                        break;
                    }
                }
            });
            executorService.shutdown();
        });
    }

    @SneakyThrows
    @Override
    public ResponseEntity<GenConversationTitleResponse> genConversationTitle(
            String accessToken,
            String conversationId,
            GenConversationTitleRequest genConversationTitleRequest
    ) {
        var executor = (JavascriptExecutor) webDriver;

        var jsonBody = objectMapper.writeValueAsString(Map.of(
                "message_id", genConversationTitleRequest.messageId(),
                "model", Constant.MODEL
        ));

        var responseText = (String) executor.executeScript("""
                var xhr = new XMLHttpRequest();
                xhr.open('POST', '%s', false);
                xhr.setRequestHeader('Authorization', '%s');
                xhr.setRequestHeader('Content-Type', 'application/json');
                xhr.send('%s');
                return xhr.responseText;
                """.formatted(String.format(Constant.GEN_CONVERSATION_TITLE_URL, conversationId), accessToken, jsonBody));

        return ResponseEntity.ok(objectMapper.readValue(responseText, GenConversationTitleResponse.class));
    }

    @SneakyThrows
    @Override
    public ResponseEntity<GetConversationContentResponse> getConversationContent(String accessToken, String conversationId) {
        var executor = (JavascriptExecutor) webDriver;

        var responseText = (String) executor.executeScript("""
                var xhr = new XMLHttpRequest();
                xhr.open('GET', '%s', false);
                xhr.setRequestHeader('Authorization', '%s');
                xhr.send();
                return xhr.responseText;
                """.formatted(String.format(Constant.GET_CONVERSATION_CONTENT_URL, conversationId), accessToken));

        return ResponseEntity.ok(objectMapper.readValue(responseText, GetConversationContentResponse.class));
    }

    @SneakyThrows
    @Override
    public ResponseEntity<Boolean> renameConversation(
            String accessToken,
            String conversationId,
            RenameConversationTitleRequest renameConversationTitleRequest
    ) {
        var executor = (JavascriptExecutor) webDriver;

        var jsonBody = objectMapper.writeValueAsString(Map.of(
                "title", renameConversationTitleRequest.title()
        ));

        var responseText = (String) executor.executeScript("""
                var xhr = new XMLHttpRequest();
                xhr.open('PATCH', '%s', false);
                xhr.setRequestHeader('Authorization', '%s');
                xhr.setRequestHeader('Content-Type', 'application/json');
                xhr.send('%s');
                return xhr.responseText;
                """.formatted(String.format(Constant.RENAME_CONVERSATION_TITLE_URL, conversationId), accessToken, jsonBody));

        return ResponseEntity.ok((Boolean) objectMapper.readValue(responseText, Map.class).get("success"));
    }
}
