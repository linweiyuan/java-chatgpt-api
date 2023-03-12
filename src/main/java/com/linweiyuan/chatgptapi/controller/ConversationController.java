package com.linweiyuan.chatgptapi.controller;

import com.linweiyuan.chatgptapi.misc.Constant;
import com.linweiyuan.chatgptapi.model.*;
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

    @PostMapping("/conversation/gen_title/{conversationId}")
    public ResponseEntity<GenConversationTitleResponse> genConversationTitle(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken,
            @PathVariable String conversationId,
            @RequestBody GenConversationTitleRequest genConversationTitleRequest
    ) {
        return conversationService.genConversationTitle(accessToken, conversationId, genConversationTitleRequest);
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<GetConversationContentResponse> getConversationContent(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken,
            @PathVariable String conversationId
    ) {
        return conversationService.getConversationContent(accessToken, conversationId);
    }

    @PostMapping("/conversation/{conversationId}")
    public ResponseEntity<Boolean> renameConversation(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken,
            @PathVariable String conversationId,
            @RequestBody UpdateConversationRequest updateConversationRequest
    ) {
        return conversationService.updateConversation(accessToken, conversationId, updateConversationRequest);
    }

    @PostMapping("/conversations")
    public ResponseEntity<Boolean> clearConversations(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken,
            @RequestBody UpdateConversationRequest updateConversationRequest
    ) {
        return conversationService.clearConversations(accessToken, updateConversationRequest);
    }

    @PostMapping("/conversation/message_feedback")
    public ResponseEntity<String> feedbackMessage(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken,
            @RequestBody FeedbackRequest feedbackRequest
    ) {
        return conversationService.feedbackMessage(accessToken, feedbackRequest);
    }
}
