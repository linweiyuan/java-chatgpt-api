package com.linweiyuan.chatgptapi.service;

import com.linweiyuan.chatgptapi.model.GetConversationsResponse;
import com.linweiyuan.chatgptapi.model.StartConversationRequest;
import com.linweiyuan.chatgptapi.model.StartConversationResponse;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

public interface ConversationService {
    ResponseEntity<GetConversationsResponse> getConversations(String accessToken, int offset, int limit);

    Flux<StartConversationResponse> startConversation(
            String accessToken,
            StartConversationRequest startConversationRequest
    );
}
