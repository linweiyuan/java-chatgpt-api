package com.linweiyuan.chatgptapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GenerateTitleRequest(
        @JsonProperty("message_id")
        String messageId,
        String model
) {
}