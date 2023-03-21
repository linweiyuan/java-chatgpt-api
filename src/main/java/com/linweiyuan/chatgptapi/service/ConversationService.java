package com.linweiyuan.chatgptapi.service;

import com.linweiyuan.chatgptapi.model.*;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

public interface ConversationService {
    ResponseEntity<GetConversationsResponse> getConversations(String authorization, int offset, int limit);

    Flux<String> startConversation(String authorization, ConversationRequest conversationRequest);

    ResponseEntity<GenerateTitleResponse> genConversationTitle(
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
