package com.linweiyuan.chatgptapi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linweiyuan.chatgptapi.misc.Constant;
import com.linweiyuan.chatgptapi.model.*;
import com.linweiyuan.chatgptapi.service.ConversationService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class ConversationServiceImpl implements ConversationService {
    private final JavascriptExecutor js;

    private final ObjectMapper objectMapper;

    public ConversationServiceImpl(WebDriver webDriver, ObjectMapper objectMapper) {
        this.js = (JavascriptExecutor) webDriver;
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    @Override
    public ResponseEntity<GetConversationsResponse> getConversations(String accessToken, int offset, int limit) {
        var responseText = (String) js.executeScript(
                getGetScript(
                        String.format(Constant.GET_CONVERSATIONS_URL, offset, limit),
                        accessToken
                )
        );
        return ResponseEntity.ok(objectMapper.readValue(responseText, GetConversationsResponse.class));
    }

    @SneakyThrows
    @Override
    public Flux<String> startConversation(String accessToken, ConversationRequest conversationRequest) {
        var requestBody = objectMapper.writeValueAsString(conversationRequest);
        js.executeScript(getPostScriptForStartConversation(Constant.START_CONVERSATIONS_URL, accessToken, requestBody));

        return Flux.create(fluxSink -> {
            var executorService = Executors.newSingleThreadExecutor();
            executorService.submit(() -> {
                while (true) {
                    try {
                        var eventData = (String) js.executeAsyncScript(getCallbackScriptForStartConversation());
                        if (eventData.equals("429") || eventData.equals("[DONE]")) {
                            fluxSink.next(eventData);
                            fluxSink.complete();
                            break;
                        }

                        fluxSink.next(eventData);
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
    public ResponseEntity<GenerateTitleResponse> genConversationTitle(
            String accessToken,
            String conversationId,
            GenerateTitleRequest generateTitleRequest
    ) {
        var jsonBody = objectMapper.writeValueAsString(generateTitleRequest);
        var responseText = (String) js.executeScript(
                getPostScript(
                        String.format(Constant.GENERATE_TITLE_URL, conversationId),
                        accessToken,
                        jsonBody
                )
        );
        return ResponseEntity.ok(objectMapper.readValue(responseText, GenerateTitleResponse.class));
    }

    @SneakyThrows
    @Override
    public ResponseEntity<String> getConversationContent(String accessToken, String conversationId) {
        var responseText = (String) js.executeScript(
                getGetScript(
                        String.format(Constant.GET_CONVERSATION_CONTENT_URL, conversationId),
                        accessToken
                )
        );
        return ResponseEntity.ok(responseText);
    }

    @SneakyThrows
    @Override
    public ResponseEntity<Boolean> updateConversation(
            String accessToken,
            String conversationId,
            UpdateConversationRequest updateConversationRequest
    ) {
        var jsonBody = objectMapper.writeValueAsString(updateConversationRequest);
        var responseText = (String) js.executeScript(
                getPatchScript(
                        String.format(Constant.UPDATE_CONVERSATION_URL, conversationId),
                        accessToken,
                        jsonBody
                )
        );
        return ResponseEntity.ok((Boolean) objectMapper.readValue(responseText, Map.class).get("success"));
    }

    @SneakyThrows
    @Override
    public ResponseEntity<Boolean> clearConversations(String accessToken, UpdateConversationRequest updateConversationRequest) {
        var jsonBody = objectMapper.writeValueAsString(updateConversationRequest);
        var responseText = (String) js.executeScript(
                getPatchScript(
                        Constant.CLEAR_CONVERSATIONS_URL,
                        accessToken,
                        jsonBody
                )
        );
        return ResponseEntity.ok((Boolean) objectMapper.readValue(responseText, Map.class).get("success"));
    }

    @SneakyThrows
    @Override
    public ResponseEntity<String> feedbackMessage(String accessToken, FeedbackRequest feedbackRequest) {
        var jsonBody = objectMapper.writeValueAsString(feedbackRequest);
        var responseText = (String) js.executeScript(
                getPostScript(
                        Constant.FEEDBACK_MESSAGE_URL,
                        accessToken,
                        jsonBody
                )
        );
        return ResponseEntity.ok((String) objectMapper.readValue(responseText, Map.class).get("rating"));
    }

    private String getGetScript(String url, String accessToken) {
        return """
                const xhr = new XMLHttpRequest();
                xhr.open('GET', '%s', false);
                xhr.setRequestHeader('Authorization', 'Bearer %s');
                xhr.send();
                return xhr.responseText;
                """.formatted(url, accessToken);
    }

    @SuppressWarnings("SameParameterValue")
    private String getPostScriptForStartConversation(String url, String accessToken, String jsonString) {
        return """
                const xhr = new XMLHttpRequest();
                xhr.open('POST', '%s', true);
                xhr.setRequestHeader('Accept', 'text/event-stream');
                xhr.setRequestHeader('Authorization', 'Bearer %s');
                xhr.setRequestHeader('Content-Type', 'application/json');
                xhr.onreadystatechange = function() {
                    if (xhr.readyState === 3 && xhr.status === 200) {
                        window.postMessage(xhr.responseText);
                    } else if (xhr.status === 429) {
                        window.postMessage("429");
                    } if (xhr.readyState === 4) {

                    }
                };
                xhr.send('%s');
                """.formatted(url, accessToken, jsonString);
    }

    private String getCallbackScriptForStartConversation() {
        return """
               const callback = arguments[arguments.length - 1];
               const handleFunction = function(event) {
                   const list = event.data.split('\\n\\n');
                   list.pop();
                   const eventData = list.pop();
                   if (eventData.startsWith('event')) {
                       callback(eventData.substring(55));
                   } else {
                       callback(eventData.substring(6));
                   }
               };
               window.removeEventListener('message', handleFunction);
               window.addEventListener('message', handleFunction);
                """;
    }

    private String getPostScript(String url, String accessToken, String jsonBody) {
        return """
                const xhr = new XMLHttpRequest();
                xhr.open('POST', '%s', false);
                xhr.setRequestHeader('Authorization', 'Bearer %s');
                xhr.setRequestHeader('Content-Type', 'application/json');
                xhr.send('%s');
                return xhr.responseText;
                """.formatted(url, accessToken, jsonBody);
    }

    private String getPatchScript(String url, String accessToken, String jsonBody) {
        return """
                const xhr = new XMLHttpRequest();
                xhr.open('PATCH', '%s', false);
                xhr.setRequestHeader('Authorization', 'Bearer %s');
                xhr.setRequestHeader('Content-Type', 'application/json');
                xhr.send('%s');
                return xhr.responseText;
                """.formatted(url, accessToken, jsonBody);
    }
}