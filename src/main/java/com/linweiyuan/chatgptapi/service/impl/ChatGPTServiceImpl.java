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
import com.microsoft.playwright.PlaywrightException;
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
import static com.linweiyuan.chatgptapi.misc.HeaderUtil.getAuthorizationHeader;
import static com.linweiyuan.chatgptapi.misc.JsUtil.*;
import static com.linweiyuan.chatgptapi.misc.LogUtil.error;

@EnabledOnChatGPT
@Service
public class ChatGPTServiceImpl implements ChatGPTService {
    private final Page apiPage;
    private final Page refreshPage;

    private final ObjectMapper objectMapper;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public ChatGPTServiceImpl(Page apiPage, Page refreshPage, ObjectMapper objectMapper) {
        this.apiPage = apiPage;
        this.refreshPage = refreshPage;
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    @Override
    public ResponseEntity<String> getConversations(String accessToken, int offset, int limit) {
        var responseText = (String) apiPage.evaluate(
                getGetScript(
                        String.format(Constant.GET_CONVERSATIONS_URL, offset, limit),
                        accessToken,
                        Constant.ERROR_MESSAGE_GET_CONVERSATIONS
                )
        );
        if (Constant.ERROR_MESSAGE_GET_CONVERSATIONS.equals(responseText)) {
            PlaywrightUtil.tryToReload(refreshPage);
            return getConversations(accessToken, offset, limit);
        }

        return ResponseEntity.ok(responseText);
    }

    @Override
    public Flux<String> startConversation(String accessToken, ConversationRequest conversationRequest) {
        return Flux.create(fluxSink -> executorService.submit(() -> {
                    try {
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
                        error("Start conversation exception: " + e.getLocalizedMessage());
                    }
                })
        );
    }

    @SneakyThrows
    private void sendConversationRequest(String accessToken, ConversationRequest conversationRequest, String oldContentToResponse, FluxSink<String> fluxSink) {
        String requestBody = objectMapper.writeValueAsString(conversationRequest);
        var messageId = conversationRequest.messages().get(0).getId();
        apiPage.evaluate(getPostScriptForStartConversation(Constant.START_CONVERSATIONS_URL, getAuthorizationHeader(accessToken), requestBody, messageId));

        // prevent handle multiple times
        var temp = "";
        ConversationResponse conversationResponse = null;
        var maxTokens = false;
        while (true) {
            String conversationResponseData;
            try {
                conversationResponseData = (String) apiPage.evaluate(String.format("() => conversationMap.get('%s')", messageId));
                if (conversationResponseData == null || conversationResponseData.isBlank()) {
                    continue;
                }
            } catch (PlaywrightException ignored) {
                continue;
            }

            if (conversationResponseData.charAt(0) == '4' || conversationResponseData.charAt(0) == '5') {
                var statusCode = Integer.parseInt(conversationResponseData.substring(0, 3));
                if (statusCode == HttpStatus.FORBIDDEN.value()) {
                    refreshPage.reload();
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
        apiPage.evaluate(String.format("conversationMap.delete('%s');", messageId));
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
        var responseText = (String) apiPage.evaluate(
                getPostScript(
                        String.format(Constant.GENERATE_TITLE_URL, conversationId),
                        accessToken,
                        jsonBody,
                        Constant.ERROR_MESSAGE_GENERATE_TITLE
                )
        );
        if (Constant.ERROR_MESSAGE_GENERATE_TITLE.equals(responseText)) {
            PlaywrightUtil.tryToReload(refreshPage);
            throw new ConversationException(ErrorEnum.GENERATE_TITLE_ERROR);
        }
        return ResponseEntity.ok(responseText);
    }

    @SneakyThrows
    @Override
    public ResponseEntity<String> getConversationContent(String accessToken, String conversationId) {
        var responseText = (String) apiPage.evaluate(
                getGetScript(
                        String.format(Constant.GET_CONVERSATION_CONTENT_URL, conversationId),
                        accessToken,
                        Constant.ERROR_MESSAGE_GET_CONTENT
                )
        );
        if (Constant.ERROR_MESSAGE_GET_CONTENT.equals(responseText)) {
            PlaywrightUtil.tryToReload(refreshPage);
            throw new ConversationException(ErrorEnum.GET_CONTENT_ERROR);
        }
        return ResponseEntity.ok(responseText);
    }

    @SneakyThrows
    @Override
    public ResponseEntity<Boolean> updateConversation(String accessToken, String conversationId, UpdateConversationRequest updateConversationRequest) {
        var jsonBody = objectMapper.writeValueAsString(updateConversationRequest);
        var responseText = (String) apiPage.evaluate(
                getPatchScript(
                        String.format(Constant.UPDATE_CONVERSATION_URL, conversationId),
                        accessToken,
                        jsonBody,
                        Constant.ERROR_MESSAGE_UPDATE_CONVERSATION
                )
        );
        if (Constant.ERROR_MESSAGE_UPDATE_CONVERSATION.equals(responseText)) {
            PlaywrightUtil.tryToReload(refreshPage);
            return updateConversation(accessToken, conversationId, updateConversationRequest);
        }

        return ResponseEntity.ok((Boolean) objectMapper.readValue(responseText, Map.class).get("success"));
    }

    @SneakyThrows
    @Override
    public ResponseEntity<Boolean> clearConversations(String accessToken, UpdateConversationRequest updateConversationRequest) {
        var jsonBody = objectMapper.writeValueAsString(updateConversationRequest);
        var responseText = (String) apiPage.evaluate(
                getPatchScript(
                        Constant.CLEAR_CONVERSATIONS_URL,
                        accessToken,
                        jsonBody,
                        Constant.ERROR_MESSAGE_CLEAR_CONVERSATIONS
                )
        );
        if (Constant.ERROR_MESSAGE_CLEAR_CONVERSATIONS.equals(responseText)) {
            PlaywrightUtil.tryToReload(refreshPage);
            return clearConversations(accessToken, updateConversationRequest);
        }

        return ResponseEntity.ok((Boolean) objectMapper.readValue(responseText, Map.class).get("success"));
    }

    @SneakyThrows
    @Override
    public ResponseEntity<String> feedbackMessage(String accessToken, FeedbackRequest feedbackRequest) {
        var jsonBody = objectMapper.writeValueAsString(feedbackRequest);
        var responseText = (String) apiPage.evaluate(
                getPostScript(
                        Constant.FEEDBACK_MESSAGE_URL,
                        accessToken,
                        jsonBody,
                        Constant.ERROR_MESSAGE_FEEDBACK_MESSAGE
                )
        );
        if (Constant.ERROR_MESSAGE_FEEDBACK_MESSAGE.equals(responseText)) {
            PlaywrightUtil.tryToReload(refreshPage);
            return feedbackMessage(accessToken, feedbackRequest);
        }

        return ResponseEntity.ok((String) objectMapper.readValue(responseText, Map.class).get("rating"));
    }

    @Override
    public ResponseEntity<String> getModels(String accessToken) {
        var responseText = (String) apiPage.evaluate(
                getGetScript(
                        String.format(Constant.GET_MODELS_URL),
                        accessToken,
                        Constant.ERROR_MESSAGE_GET_MODELS
                )
        );
        if (Constant.ERROR_MESSAGE_GET_MODELS.equals(responseText)) {
            PlaywrightUtil.tryToReload(refreshPage);
            return getModels(accessToken);
        }

        return ResponseEntity.ok(responseText);
    }

    @Override
    public ResponseEntity<String> checkAccount(String accessToken) {
        var responseText = (String) apiPage.evaluate(
                getGetScript(
                        String.format(Constant.CHECK_ACCOUNT_URL),
                        accessToken,
                        Constant.ERROR_MESSAGE_CHECK_ACCOUNT
                )
        );
        if (Constant.ERROR_MESSAGE_CHECK_ACCOUNT.equals(responseText)) {
            PlaywrightUtil.tryToReload(refreshPage);
            return checkAccount(accessToken);
        }

        return ResponseEntity.ok(responseText);
    }
}
