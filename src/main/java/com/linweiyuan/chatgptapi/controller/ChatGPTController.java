package com.linweiyuan.chatgptapi.controller;

import com.linweiyuan.chatgptapi.annotation.PreCheck;
import com.linweiyuan.chatgptapi.misc.Constant;
import com.linweiyuan.chatgptapi.model.chatgpt.*;
import com.linweiyuan.chatgptapi.service.ChatGPTService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@SuppressWarnings("unused")
@PreCheck(onlyCheckAuthorization = false)
@RestController
public class ChatGPTController {
    private final ChatGPTService chatGPTService;

    public ChatGPTController(ChatGPTService chatGPTService) {
        this.chatGPTService = chatGPTService;
    }

    @GetMapping("/conversations")
    public ResponseEntity<GetConversationsResponse> getConversations(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken,
            @RequestParam(defaultValue = Constant.DEFAULT_OFFSET) int offset,
            @RequestParam(defaultValue = Constant.DEFAULT_LIMIT) int limit
    ) {
        return chatGPTService.getConversations(accessToken, offset, limit);
    }

    @PostMapping(value = "/conversation", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> startConversation(
            @RequestHeader String authorization,
            @RequestBody ConversationRequest conversationRequest
    ) {
        return chatGPTService.startConversation(authorization, conversationRequest);
    }

    @PostMapping("/conversation/gen_title/{conversationId}")
    public ResponseEntity<GenerateTitleResponse> genConversationTitle(
            @RequestHeader String authorization,
            @PathVariable String conversationId,
            @RequestBody GenerateTitleRequest generateTitleRequest
    ) {
        return chatGPTService.genConversationTitle(authorization, conversationId, generateTitleRequest);
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<String> getConversationContent(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken,
            @PathVariable String conversationId
    ) {
        return chatGPTService.getConversationContent(accessToken, conversationId);
    }

    @PostMapping("/conversation/{conversationId}")
    public ResponseEntity<Boolean> updateConversation(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken,
            @PathVariable String conversationId,
            @RequestBody UpdateConversationRequest updateConversationRequest
    ) {
        return chatGPTService.updateConversation(accessToken, conversationId, updateConversationRequest);
    }

    @PostMapping("/conversations")
    public ResponseEntity<Boolean> clearConversations(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken,
            @RequestBody UpdateConversationRequest updateConversationRequest
    ) {
        return chatGPTService.clearConversations(accessToken, updateConversationRequest);
    }

    @PostMapping("/conversation/message_feedback")
    public ResponseEntity<String> feedbackMessage(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken,
            @RequestBody FeedbackRequest feedbackRequest
    ) {
        return chatGPTService.feedbackMessage(accessToken, feedbackRequest);
    }
}
