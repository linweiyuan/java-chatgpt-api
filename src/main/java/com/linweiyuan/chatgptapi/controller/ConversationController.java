package com.linweiyuan.chatgptapi.controller;

import com.linweiyuan.chatgptapi.misc.Constant;
import com.linweiyuan.chatgptapi.model.GetConversationsResponse;
import com.linweiyuan.chatgptapi.model.StartConversationRequest;
import com.linweiyuan.chatgptapi.model.StartConversationResponse;
import com.linweiyuan.chatgptapi.service.ConversationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@SuppressWarnings("unused")
@RestController
public class ConversationController {
    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping("/conversations")
    public ResponseEntity<GetConversationsResponse> getConversations(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken,
            @RequestParam(defaultValue = Constant.DEFAULT_OFFSET) int offset,
            @RequestParam(defaultValue = Constant.DEFAULT_LIMIT) int limit
    ) {
        return conversationService.getConversations(accessToken, offset, limit);
    }

    @PostMapping(value = "/conversation", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<StartConversationResponse> startConversation(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken,
            @RequestBody StartConversationRequest startConversationRequest
    ) {
        return conversationService.startConversation(accessToken, startConversationRequest);
    }
}
