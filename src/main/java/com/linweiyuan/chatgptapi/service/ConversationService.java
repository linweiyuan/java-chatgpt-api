package com.linweiyuan.chatgptapi.service;

import com.linweiyuan.chatgptapi.model.*;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

public interface ConversationService {
    ResponseEntity<GetConversationsResponse> getConversations(String accessToken, int offset, int limit);

    Flux<StartConversationResponse> startConversation(
            String accessToken,
            StartConversationRequest startConversationRequest
    );

    ResponseEntity<GenConversationTitleResponse> genConversationTitle(
            String accessToken,
            String conversationId,
            GenConversationTitleRequest genConversationTitleRequest
    );

    ResponseEntity<GetConversationContentResponse> getConversationContent(
            String accessToken,
            String conversationId
    );

    ResponseEntity<Boolean> renameConversation(
            String accessToken,
            String conversationId,
            RenameConversationTitleRequest renameConversationTitleRequest
    );
}
