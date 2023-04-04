package com.linweiyuan.chatgptapi.model.chatgpt;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GenerateTitleRequest(
        @JsonProperty("message_id")
        String messageId,
        String model
) {
}
