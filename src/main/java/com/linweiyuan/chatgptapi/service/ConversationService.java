  package com.linweiyuan.chatgptapi.service;

import com.linweiyuan.chatgptapi.model.GetConversationsResponse;
import org.springframework.http.ResponseEntity;

public interface ConversationService {
    ResponseEntity<GetConversationsResponse> getConversations(String accessToken, int offset, int limit);
}
