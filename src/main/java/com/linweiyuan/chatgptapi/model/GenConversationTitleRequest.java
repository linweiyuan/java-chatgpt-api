package com.linweiyuan.chatgptapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GenConversationTitleRequest(
        @JsonProperty("message_id")
        String messageId
) {
}
