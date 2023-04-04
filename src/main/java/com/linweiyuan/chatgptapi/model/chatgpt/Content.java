package com.linweiyuan.chatgptapi.model.chatgpt;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Content(
        @JsonProperty("content_type")
        String contentType,
        List<String> parts
) {
}
