package com.linweiyuan.chatgptapi.service.impl;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linweiyuan.chatgptapi.annotation.EnabledOnChatGPT;
import com.linweiyuan.chatgptapi.enums.ErrorEnum;
import com.linweiyuan.chatgptapi.exception.ConversationException;
import com.linweiyuan.chatgptapi.misc.Constant;
import com.linweiyuan.chatgptapi.misc.PlaywrightUtil;
import com.linweiyuan.chatgptapi.model.chatgpt.*;
import com.linweiyuan.chatgptapi.service.ChatGPTService;
import com.microsoft.playwright.Page;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.linweiyuan.chatgptapi.misc.Constant.DONE_FLAG;
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
            PlaywrightUtil.tryToReload(page);
            throw new ConversationException(ErrorEnum.GET_CONVERSATIONS_ERROR);
        }
        return ResponseEntity.ok(responseText);
    }

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
        ConversationResponse conversationResponse = null;
        var maxTokens = false;
        while (true) {
            var conversationResponseData = (String) page.evaluate("() => window.conversationResponseData;");
            if (conversationResponseData == null || conversationResponseData.isBlank()) {
                continue;
            }

            if (conversationResponseData.charAt(0) == '4' || conversationResponseData.charAt(0) == '5') {
                var statusCode = Integer.parseInt(conversationResponseData.substring(0, 3));
                if (statusCode == HttpStatus.FORBIDDEN.value()) {
                    page.reload();
                }
                fluxSink.error(new ConversationException(statusCode, conversationResponseData.substring(3)));
                fluxSink.complete();
                break;
            }
            if (conversationResponseData.charAt(0) == '!') {
                fluxSink.next(conversationResponseData.substring(1));
                fluxSink.next(DONE_FLAG);
                fluxSink.complete();
                break;
            }

            if (!temp.isBlank()) {
                if (temp.equals(conversationResponseData)) {
                    continue;
                }
            }
            temp = conversationResponseData;

            try {
                conversationResponse = objectMapper.readValue(conversationResponseData, ConversationResponse.class);
            } catch (JsonParseException e) {
                continue;
            }

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
                var continueText = conversationRequest.continueText();
                if (StringUtils.hasText(continueText)) {
                    maxTokens = true;
                    oldContentToResponse = message.content().getParts().get(0);
                } else {
                    fluxSink.next(DONE_FLAG);
                    fluxSink.complete();
                }
                break;
            }

            var endTurn = message.endTurn();
            if (endTurn) {
                fluxSink.next(DONE_FLAG);
                fluxSink.complete();
                break;
            }
        }
        if (maxTokens && StringUtils.hasText(conversationRequest.continueText())) {
            TimeUnit.SECONDS.sleep(1);

            var newRequest = newConversationRequest(conversationRequest, conversationResponse);
            sendConversationRequest(accessToken, newRequest, oldContentToResponse, fluxSink);
        }
    }

    private static ConversationRequest newConversationRequest(ConversationRequest conversationRequest, ConversationResponse conversationResponse) {
        var author = new Author();
        author.setRole("user");
        Content content = new Content();
        content.setContentType("text");
        content.setParts(List.of(conversationRequest.continueText()));
        var message = new Message();
        message.setAuthor(author);
        message.setContent(content);
        message.setId(UUID.randomUUID().toString());
        return new ConversationRequest(
                conversationRequest.action(),
                List.of(message),
                conversationRequest.model(),
                conversationResponse.conversationResponseMessage().id(),
                conversationResponse.conversationId(),
                conversationRequest.continueText()
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
            PlaywrightUtil.tryToReload(page);
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
            PlaywrightUtil.tryToReload(page);
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
            PlaywrightUtil.tryToReload(page);
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
            PlaywrightUtil.tryToReload(page);
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
            PlaywrightUtil.tryToReload(page);
            throw new ConversationException(ErrorEnum.FEEDBACK_MESSAGE_ERROR);
        }
        return ResponseEntity.ok((String) objectMapper.readValue(responseText, Map.class).get("rating"));
    }

    @Override
    public ResponseEntity<String> getModels(String accessToken) {
        var responseText = (String) page.evaluate(
                getGetScript(
                        String.format(Constant.GET_MODELS_URL),
                        accessToken,
                        Constant.ERROR_MESSAGE_GET_MODELS
                )
        );
        if (Constant.ERROR_MESSAGE_GET_MODELS.equals(responseText)) {
            PlaywrightUtil.tryToReload(page);
            throw new ConversationException(ErrorEnum.GET_MODELS_ERROR);
        }
        return ResponseEntity.ok(responseText);
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
                // get the whole data again to make sure get the endTurn message back
                const getEndTurnMessage = (dataArray) => {
                    dataArray.pop(); // empty
                    dataArray.pop(); // data: [DONE]
                    return '!' + dataArray.pop().substring(6); // endTurn message
                };

                let conversationResponseData;

                const xhr = new XMLHttpRequest();
                xhr.open('POST', '%s');
                xhr.setRequestHeader('Accept', 'text/event-stream');
                xhr.setRequestHeader('Authorization', '%s');
                xhr.setRequestHeader('Content-Type', 'application/json');
                xhr.onreadystatechange = function() {
                    switch (xhr.readyState) {
                        case xhr.LOADING: {
                            switch (xhr.status) {
                                case 200: {
                                    const dataArray = xhr.responseText.substr(xhr.seenBytes).split("\\n\\n");
                                    dataArray.pop(); // empty string
                                    if (dataArray.length) {
                                        let data = dataArray.pop(); // target data
                                        if (data === 'data: [DONE]') { // this DONE will break the ending handling
                                            data = getEndTurnMessage(xhr.responseText.split("\\n\\n"));
                                        } else if (data.startsWith('event')) {
                                            data = data.substring(49);
                                        }
                                        if (data) {
                                            if (data.startsWith('!')) {
                                                window.conversationResponseData = data;
                                            } else {
                                                window.conversationResponseData = data.substring(6);
                                            }
                                        }
                                    }
                                    break;
                                }
                                case 401: {
                                    window.conversationResponseData = xhr.status + 'Access token has expired.';
                                    break;
                                }
                                case 403: {
                                    window.conversationResponseData = xhr.status + 'Something went wrong. If this issue persists please contact us through our help center at help.openai.com.';
                                    break;
                                }
                                case 404: {
                                    window.conversationResponseData = xhr.status + JSON.parse(xhr.responseText).detail;
                                    break;
                                }
                                case 413: {
                                    window.conversationResponseData = xhr.status + JSON.parse(xhr.responseText).detail.message;
                                    break;
                                }
                                case 422: {
                                    const detail = JSON.parse(xhr.responseText).detail[0];
                                    window.conversationResponseData = xhr.status + detail.loc + ' -> ' + detail.msg;
                                    break;
                                }
                                case 429: {
                                    window.conversationResponseData = xhr.status + JSON.parse(xhr.responseText).detail;
                                    break;
                                }
                                case 500: {
                                    window.conversationResponseData = xhr.status + 'Unknown error.';
                                    break;
                                }
                            }
                            xhr.seenBytes = xhr.responseText.length;
                            break;
                        }
                        case xhr.DONE:
                            // keep exception handling
                            if (!window.conversationResponseData.startsWith('4') && !window.conversationResponseData.startsWith('5')) {
                                window.conversationResponseData = getEndTurnMessage(xhr.responseText.split("\\n\\n"));
                            }
                            break;
                    }
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
