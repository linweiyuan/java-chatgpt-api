package com.linweiyuan.chatgptapi.model.chatgpt;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConversationResponseMessage(
        @JsonProperty("end_turn")
        boolean endTurn
) {
}
