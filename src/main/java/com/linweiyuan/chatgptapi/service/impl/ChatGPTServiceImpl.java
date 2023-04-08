package com.linweiyuan.chatgptapi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linweiyuan.chatgptapi.annotation.EnabledOnChatGPT;
import com.linweiyuan.chatgptapi.enums.ErrorEnum;
import com.linweiyuan.chatgptapi.exception.ConversationException;
import com.linweiyuan.chatgptapi.misc.Constant;
import com.linweiyuan.chatgptapi.model.chatgpt.*;
import com.linweiyuan.chatgptapi.service.ChatGPTService;
import com.microsoft.playwright.Page;
import lombok.SneakyThrows;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.linweiyuan.chatgptapi.misc.Constant.PAGE_RELOAD_LOCK;
import static com.linweiyuan.chatgptapi.misc.HeaderUtil.getAuthorizationHeader;

@EnabledOnChatGPT
@Service
public class ChatGPTServiceImpl implements ChatGPTService {
    private final Page page;

    private final ObjectMapper objectMapper;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public ChatGPTServiceImpl(Page page, ObjectMapper objectMapper) {
        this.page = page;
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    @Override
    public ResponseEntity<String> getConversations(String accessToken, int offset, int limit) {
        var responseText = (String) page.evaluate(
                getGetScript(
                        String.format(Constant.GET_CONVERSATIONS_URL, offset, limit),
                        accessToken,
                        Constant.ERROR_MESSAGE_GET_CONVERSATIONS
                )
        );
        if (Constant.ERROR_MESSAGE_GET_CONVERSATIONS.equals(responseText)) {
            throw new ConversationException(ErrorEnum.GET_CONVERSATIONS_ERROR);
        }

        return ResponseEntity.ok(responseText);
    }

    @SneakyThrows
    @Override
    public Flux<String> startConversation(String accessToken, ConversationRequest conversationRequest) {
        return Flux.create(fluxSink -> executorService.submit(() -> {
                    try {
                        // prevent page auto reload interrupting conversation
                        PAGE_RELOAD_LOCK.lock();

                        // add support for old api
                        var message = conversationRequest.messages().get(0);
                        var author = message.getAuthor();
                        if (author == null || author.getRole() == null) {
                            author = new Author();
                            author.setRole("user");
                            message.setAuthor(author);
                        }
                        String requestBody = objectMapper.writeValueAsString(conversationRequest);
                        page.evaluate("delete window.conversationResponseData;");
                        page.evaluate(getPostScriptForStartConversation(Constant.START_CONVERSATIONS_URL, getAuthorizationHeader(accessToken), requestBody));

                        // prevent handle multiple times
                        var temp = "";
                        while (true) {
                            var conversationResponseData = (String) page.evaluate("() => window.conversationResponseData;");
                            if (conversationResponseData == null) {
                                continue;
                            }

                            //noinspection OptionalGetWithoutIsPresent
                            conversationResponseData = Arrays.stream(conversationResponseData.split("\n\n"))
                                    .filter(s ->
                                            !s.isBlank() &&
                                            !s.startsWith("event") &&
                                            !s.startsWith("data: 2023") &&
                                            !s.equals("data: [DONE]")
                                    )
                                    .reduce((first, last) -> last)
                                    .get();
                            if (!temp.isBlank()) {
                                if (temp.equals(conversationResponseData)) {
                                    continue;
                                }
                            }
                            temp = conversationResponseData;

                            conversationResponseData = conversationResponseData.substring(6);
                            fluxSink.next(conversationResponseData);

                            var endTurn = objectMapper.readValue(conversationResponseData, ConversationResponse.class)
                                    .conversationResponseMessage()
                                    .endTurn();
                            if (endTurn) {
                                fluxSink.next("[DONE]");
                                fluxSink.complete();
                                break;
                            }
                        }
                    } catch (Exception ignored) {
                    } finally {
                        PAGE_RELOAD_LOCK.unlock();
                    }
                })
        );
    }

    @SneakyThrows
    @Override
    public ResponseEntity<String> genConversationTitle(
            String accessToken,
            String conversationId,
            GenerateTitleRequest generateTitleRequest
    ) {
        var jsonBody = objectMapper.writeValueAsString(generateTitleRequest);
        var responseText = (String) page.evaluate(
                getPostScript(
                        String.format(Constant.GENERATE_TITLE_URL, conversationId),
                        accessToken,
                        jsonBody,
                        Constant.ERROR_MESSAGE_GENERATE_TITLE
                )
        );
        return ResponseEntity.ok(responseText);
    }

    @SneakyThrows
    @Override
    public ResponseEntity<String> getConversationContent(String accessToken, String conversationId) {
        var responseText = (String) page.evaluate(
                getGetScript(
                        String.format(Constant.GET_CONVERSATION_CONTENT_URL, conversationId),
                        accessToken,
                        Constant.ERROR_MESSAGE_GET_CONTENT
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
        var responseText = (String) page.evaluate(
                getPatchScript(
                        String.format(Constant.UPDATE_CONVERSATION_URL, conversationId),
                        accessToken,
                        jsonBody,
                        Constant.ERROR_MESSAGE_UPDATE_CONVERSATION
                )
        );
        return ResponseEntity.ok((Boolean) objectMapper.readValue(responseText, Map.class).get("success"));
    }

    @SneakyThrows
    @Override
    public ResponseEntity<Boolean> clearConversations(String accessToken, UpdateConversationRequest updateConversationRequest) {
        var jsonBody = objectMapper.writeValueAsString(updateConversationRequest);
        var responseText = (String) page.evaluate(
                getPatchScript(
                        Constant.CLEAR_CONVERSATIONS_URL,
                        accessToken,
                        jsonBody,
                        Constant.ERROR_MESSAGE_CLEAR_CONVERSATIONS
                )
        );
        return ResponseEntity.ok((Boolean) objectMapper.readValue(responseText, Map.class).get("success"));
    }

    @SneakyThrows
    @Override
    public ResponseEntity<String> feedbackMessage(String accessToken, FeedbackRequest feedbackRequest) {
        var jsonBody = objectMapper.writeValueAsString(feedbackRequest);
        var responseText = (String) page.evaluate(
                getPostScript(
                        Constant.FEEDBACK_MESSAGE_URL,
                        accessToken,
                        jsonBody,
                        Constant.ERROR_MESSAGE_FEEDBACK_MESSAGE
                )
        );
        return ResponseEntity.ok((String) objectMapper.readValue(responseText, Map.class).get("rating"));
    }

    private String getGetScript(String url, String accessToken, String errorMessage) {
        return """
                fetch('%s', {
                    headers: {
                        'Authorization': '%s'
                    }
                })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('%s');
                    }
                    return response.text();
                })
                .catch(err => {
                    return err.message;
                });
                """.formatted(url, getAuthorizationHeader(accessToken), errorMessage);
    }

    @SuppressWarnings({"SameParameterValue", "SpellCheckingInspection"})
    private String getPostScriptForStartConversation(String url, String accessToken, String jsonString) {
        return """
                let conversationResponseData;

                const xhr = new XMLHttpRequest();
                xhr.open('POST', '%s', true);
                xhr.setRequestHeader('Accept', 'text/event-stream');
                xhr.setRequestHeader('Authorization', '%s');
                xhr.setRequestHeader('Content-Type', 'application/json');
                xhr.onreadystatechange = function() {
                    window.conversationResponseData = xhr.responseText;
                };
                xhr.send(JSON.stringify(%s));
                """.formatted(url, getAuthorizationHeader(accessToken), jsonString);
    }

    private String getPostScript(String url, String accessToken, String jsonBody, String errorMessage) {
        return """
                fetch('%s', {
                    method: 'POST',
                    headers: {
                        'Authorization': '%s',
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(%s)
                })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('%s');
                    }
                    return response.text();
                })
                .catch(err => {
                    return err.message;
                });
                """.formatted(url, getAuthorizationHeader(accessToken), jsonBody, errorMessage);
    }

    private String getPatchScript(String url, String accessToken, String jsonBody, String errorMessage) {
        return """
                fetch('%s', {
                    method: 'PATCH',
                    headers: {
                        'Authorization': '%s',
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(%s)
                })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('%s');
                    }
                    return response.text();
                })
                .catch(err => {
                    return err.message;
                });
                """.formatted(url, getAuthorizationHeader(accessToken), jsonBody, errorMessage);
    }
}
