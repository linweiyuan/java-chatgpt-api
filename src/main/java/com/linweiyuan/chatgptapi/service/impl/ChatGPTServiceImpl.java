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
import reactor.core.publisher.FluxSink;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.linweiyuan.chatgptapi.misc.Constant.PAGE_RELOAD_LOCK;
import static com.linweiyuan.chatgptapi.misc.HeaderUtil.getAuthorizationHeader;
import static com.linweiyuan.chatgptapi.misc.LogUtil.error;

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

                        // add support for old chatgpt api
                        var message = conversationRequest.messages().get(0);
                        var author = message.getAuthor();
                        if (author == null || author.getRole() == null) {
                            author = new Author();
                            author.setRole("user");
                            message.setAuthor(author);
                        }
                        var oldContentToResponse = "";
                        sendConversationRequest(accessToken, conversationRequest, oldContentToResponse, fluxSink);
                    } catch (Exception e) {
                        error(e.getLocalizedMessage());
                    } finally {
                        PAGE_RELOAD_LOCK.unlock();
                    }
                })
        );
    }

    @SneakyThrows
    private void sendConversationRequest(String accessToken, ConversationRequest conversationRequest, String oldContentToResponse, FluxSink<String> fluxSink) {
        String requestBody = objectMapper.writeValueAsString(conversationRequest);
        page.evaluate("delete window.conversationResponseData;");
        page.evaluate(getPostScriptForStartConversation(Constant.START_CONVERSATIONS_URL, getAuthorizationHeader(accessToken), requestBody));

        // prevent handle multiple times
        var temp = "";
        ConversationResponse conversationResponse;
        var maxTokens = false;
        while (true) {
            var conversationResponseData = (String) page.evaluate("() => window.conversationResponseData;");
            if (conversationResponseData == null || conversationResponseData.isBlank()) {
                TimeUnit.SECONDS.sleep(1);
                continue;
            }

            if (!temp.isBlank()) {
                if (temp.equals(conversationResponseData)) {
                    TimeUnit.MILLISECONDS.sleep(10);
                    continue;
                }
            }
            temp = conversationResponseData;

            conversationResponse = objectMapper.readValue(conversationResponseData, ConversationResponse.class);
            var message = conversationResponse.conversationResponseMessage();
            if (oldContentToResponse.isBlank()) {
                fluxSink.next(conversationResponseData);
            } else {
                List<String> parts = message.content().getParts();
                parts.set(0, oldContentToResponse + parts.get(0));

                fluxSink.next(objectMapper.writeValueAsString(conversationResponse));
            }

            var finishDetails = message.metadata().finishDetails();
            if (finishDetails != null && "max_tokens".equals(finishDetails.type())) {
                maxTokens = true;
                oldContentToResponse = message.content().getParts().get(0);
                break;
            }

            var endTurn = message.endTurn();
            if (endTurn) {
                fluxSink.next("[DONE]");
                fluxSink.complete();
                break;
            }
        }
        if (maxTokens) {
            var newRequest = newConversationRequest(conversationRequest, conversationResponse);
            sendConversationRequest(accessToken, newRequest, oldContentToResponse, fluxSink);
        }
    }

    private static ConversationRequest newConversationRequest(ConversationRequest conversationRequest, ConversationResponse conversationResponse) {
        var author = new Author();
        author.setRole("user");
        Content content = new Content();
        content.setContentType("text");
        content.setParts(List.of("continue"));
        var message = new Message();
        message.setAuthor(author);
        message.setContent(content);
        message.setId(UUID.randomUUID().toString());
        return new ConversationRequest(
                conversationRequest.action(),
                List.of(message),
                conversationRequest.model(),
                conversationResponse.conversationResponseMessage().id(),
                conversationResponse.conversationId()
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
        if (Constant.ERROR_MESSAGE_GENERATE_TITLE.equals(responseText)) {
            throw new ConversationException(ErrorEnum.GENERATE_TITLE_ERROR);
        }
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
        if (Constant.ERROR_MESSAGE_GET_CONTENT.equals(responseText)) {
            throw new ConversationException(ErrorEnum.GET_CONTENT_ERROR);
        }
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
        if (Constant.ERROR_MESSAGE_UPDATE_CONVERSATION.equals(responseText)) {
            throw new ConversationException(ErrorEnum.UPDATE_CONVERSATION_ERROR);
        }
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
        if (Constant.ERROR_MESSAGE_CLEAR_CONVERSATIONS.equals(responseText)) {
            throw new ConversationException(ErrorEnum.CLEAR_CONVERSATIONS_ERROR);
        }
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
        if (Constant.ERROR_MESSAGE_FEEDBACK_MESSAGE.equals(responseText)) {
            throw new ConversationException(ErrorEnum.FEEDBACK_MESSAGE_ERROR);
        }
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
                let temp;

                const xhr = new XMLHttpRequest();
                xhr.open('POST', '%s');
                xhr.setRequestHeader('Accept', 'text/event-stream');
                xhr.setRequestHeader('Authorization', '%s');
                xhr.setRequestHeader('Content-Type', 'application/json');
                xhr.onreadystatechange = function() {
                    if (xhr.readyState === xhr.LOADING || xhr.readyState === xhr.DONE) {
                        const dataArray = xhr.responseText.substr(xhr.seenBytes).split("\\n\\n");
                        dataArray.pop(); // empty string
                        if (dataArray.length) {
                            let data = dataArray.pop(); // target data
                            if (data === 'data: [DONE]') { // this DONE will break the ending handling
                                if (dataArray.length) {
                                    data = dataArray.pop();
                                } else {
                                    data = temp;
                                }
                            } else if (data.startsWith('event')) {
                                data = data.substring(49);
                                if (!data) {
                                    data = temp;
                                }
                            }
                            if (data) {
                                if (!temp || temp !== data) {
                                    temp = data;
                                    window.conversationResponseData = data.substring(6);
                                }
                            }
                        }
                    }
                    xhr.seenBytes = xhr.responseText.length;
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
