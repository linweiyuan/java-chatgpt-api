package com.linweiyuan.chatgptapi.controller;

import com.linweiyuan.chatgptapi.annotation.PreCheck;
import com.linweiyuan.chatgptapi.misc.Constant;
import com.linweiyuan.chatgptapi.model.*;
import com.linweiyuan.chatgptapi.service.ConversationService;
import com.linweiyuan.loggerspringbootstarter.annotation.ApiLog;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@SuppressWarnings("unused")
@ApiLog
@RestController
public class ConversationController {
    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PreCheck
    @GetMapping("/conversations")
    public ResponseEntity<GetConversationsResponse> getConversations(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken,
            @RequestParam(defaultValue = Constant.DEFAULT_OFFSET) int offset,
            @RequestParam(defaultValue = Constant.DEFAULT_LIMIT) int limit
    ) {
        return conversationService.getConversations(accessToken, offset, limit);
    }

    @PreCheck
    @PostMapping(value = "/conversation", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> startConversation(
            @RequestHeader String authorization,
            @RequestBody ConversationRequest conversationRequest
    ) {
        return conversationService.startConversation(authorization, conversationRequest);
    }

    @PreCheck
    @PostMapping("/conversation/gen_title/{conversationId}")
    public ResponseEntity<GenerateTitleResponse> genConversationTitle(
            @RequestHeader String authorization,
            @PathVariable String conversationId,
            @RequestBody GenerateTitleRequest generateTitleRequest
    ) {
        return conversationService.genConversationTitle(authorization, conversationId, generateTitleRequest);
    }

    @PreCheck
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<String> getConversationContent(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken,
            @PathVariable String conversationId
    ) {
        return conversationService.getConversationContent(accessToken, conversationId);
    }

    @PreCheck
    @PostMapping("/conversation/{conversationId}")
    public ResponseEntity<Boolean> renameConversation(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken,
            @PathVariable String conversationId,
            @RequestBody UpdateConversationRequest updateConversationRequest
    ) {
        return conversationService.updateConversation(accessToken, conversationId, updateConversationRequest);
    }

    @PreCheck
    @PostMapping("/conversations")
    public ResponseEntity<Boolean> clearConversations(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken,
            @RequestBody UpdateConversationRequest updateConversationRequest
    ) {
        return conversationService.clearConversations(accessToken, updateConversationRequest);
    }

    @PreCheck
    @PostMapping("/conversation/message_feedback")
    public ResponseEntity<String> feedbackMessage(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken,
            @RequestBody FeedbackRequest feedbackRequest
    ) {
        return conversationService.feedbackMessage(accessToken, feedbackRequest);
    }
}
