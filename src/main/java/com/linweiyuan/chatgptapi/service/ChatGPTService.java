package com.linweiyuan.chatgptapi.service;

import com.linweiyuan.chatgptapi.model.chatgpt.ConversationRequest;
import com.linweiyuan.chatgptapi.model.chatgpt.FeedbackRequest;
import com.linweiyuan.chatgptapi.model.chatgpt.GenerateTitleRequest;
import com.linweiyuan.chatgptapi.model.chatgpt.UpdateConversationRequest;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

public interface ChatGPTService {
    ResponseEntity<String> getConversations(String authorization, int offset, int limit);

    Flux<String> startConversation(String authorization, ConversationRequest conversationRequest);

    ResponseEntity<String> genConversationTitle(
            String authorization,
            String conversationId,
            GenerateTitleRequest generateTitleRequest
    );

    ResponseEntity<String> getConversationContent(
            String authorization,
            String conversationId
    );

    ResponseEntity<Boolean> updateConversation(
            String authorization,
            String conversationId,
            UpdateConversationRequest updateConversationRequest
    );

    ResponseEntity<Boolean> clearConversations(
            String authorization,
            UpdateConversationRequest updateConversationRequest
    );

    ResponseEntity<String> feedbackMessage(
            String authorization,
            FeedbackRequest feedbackRequest
    );
}
