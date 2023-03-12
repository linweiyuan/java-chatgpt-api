package com.linweiyuan.chatgptapi.model;

public record StartConversationRequest(
        String conversationId,
        String parentMessageId,
        String messageId,
        String content
) {
}
