package com.linweiyuan.chatgptapi.service;

import com.linweiyuan.chatgptapi.model.chatgpt.ConversationRequest;
import com.linweiyuan.chatgptapi.model.chatgpt.FeedbackRequest;
import com.linweiyuan.chatgptapi.model.chatgpt.GenerateTitleRequest;
import com.linweiyuan.chatgptapi.model.chatgpt.UpdateConversationRequest;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

public interface ChatGPTService {
    ResponseEntity<String> getConversations(String accessToken, int offset, int limit);

    Flux<String> startConversation(String accessToken, ConversationRequest conversationRequest);

    ResponseEntity<String> genConversationTitle(
            String accessToken,
            String conversationId,
            GenerateTitleRequest generateTitleRequest
    );

    ResponseEntity<String> getConversationContent(
            String accessToken,
            String conversationId
    );

    ResponseEntity<Boolean> updateConversation(
            String accessToken,
            String conversationId,
            UpdateConversationRequest updateConversationRequest
    );

    ResponseEntity<Boolean> clearConversations(
            String accessToken,
            UpdateConversationRequest updateConversationRequest
    );

    ResponseEntity<String> feedbackMessage(
            String accessToken,
            FeedbackRequest feedbackRequest
    );

    ResponseEntity<String> getModels(
            String accessToken
    );

    ResponseEntity<String> checkAccount(
            String accessToken
    );
}
