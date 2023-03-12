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
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.*;
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
    public Flux<StartConversationResponse> startConversation(String accessToken, StartConversationRequest startConversationRequest) {
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
        if (StringUtils.hasText(conversationId)) {
            requestMap.put("conversation_id", conversationId);
        }
        var jsonBody = objectMapper.writeValueAsString(requestMap);
        js.executeScript(
                getPostScriptForStartConversation(
                        Constant.START_CONVERSATIONS_URL,
                        accessToken,
                        jsonBody
                )
        );

        return Flux.create(fluxSink -> {
            var executorService = Executors.newSingleThreadExecutor();
            executorService.submit(() -> {
                while (true) {
                    try {
                        // TODO: is there a good way to handle this?
                        var eventData = (String) js.executeAsyncScript(getCallbackScriptForStartConversation());
                        // TODO: how to return a customized message to response?
                        if (eventData.equals("429")) {
                            fluxSink.complete();
                            break;
                        }

                        var last = Arrays.stream(eventData.split("\n\n"))
                                .map(event -> event.replaceAll("data: ", ""))
                                .reduce((first, second) -> second)
                                .orElse("")
                                .trim();
                        if (last.startsWith("event") || last.startsWith("20")) {
                            continue;
                        }

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
        var jsonBody = objectMapper.writeValueAsString(Map.of(
                "message_id", genConversationTitleRequest.messageId(),
                "model", Constant.MODEL
        ));
        var responseText = (String) js.executeScript(
                getPostScript(
                        String.format(Constant.GEN_CONVERSATION_TITLE_URL, conversationId),
                        accessToken,
                        jsonBody
                )
        );
        return ResponseEntity.ok(objectMapper.readValue(responseText, GenConversationTitleResponse.class));
    }

    @SneakyThrows
    @Override
    public ResponseEntity<GetConversationContentResponse> getConversationContent(String accessToken, String conversationId) {
        var responseText = (String) js.executeScript(
                getGetScript(
                        String.format(Constant.GET_CONVERSATION_CONTENT_URL, conversationId),
                        accessToken
                )
        );
        return ResponseEntity.ok(objectMapper.readValue(responseText, GetConversationContentResponse.class));
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
                var xhr = new XMLHttpRequest();
                xhr.open('GET', '%s', false);
                xhr.setRequestHeader('Authorization', '%s');
                xhr.send();
                return xhr.responseText;
                """.formatted(url, accessToken);
    }

    @SuppressWarnings("SameParameterValue")
    private String getPostScriptForStartConversation(String url, String accessToken, String jsonString) {
        return """
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
                """.formatted(url, accessToken, jsonString);
    }

    private String getCallbackScriptForStartConversation() {
        return """
                var callback = arguments[arguments.length - 1];
                var handleFunction = function(event) {
                    callback(event.data);
                };
                window.removeEventListener('message', handleFunction);
                window.addEventListener('message', handleFunction);
                """;
    }

    private String getPostScript(String url, String accessToken, String jsonBody) {
        return """
                var xhr = new XMLHttpRequest();
                xhr.open('POST', '%s', false);
                xhr.setRequestHeader('Authorization', '%s');
                xhr.setRequestHeader('Content-Type', 'application/json');
                xhr.send('%s');
                return xhr.responseText;
                """.formatted(url, accessToken, jsonBody);
    }

    private String getPatchScript(String url, String accessToken, String jsonBody) {
        return """
                var xhr = new XMLHttpRequest();
                xhr.open('PATCH', '%s', false);
                xhr.setRequestHeader('Authorization', '%s');
                xhr.setRequestHeader('Content-Type', 'application/json');
                xhr.send('%s');
                return xhr.responseText;
                """.formatted(url, accessToken, jsonBody);
    }
}
