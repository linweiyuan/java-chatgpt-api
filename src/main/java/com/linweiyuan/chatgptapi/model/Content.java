package com.linweiyuan.chatgptapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;

public record Content(
        @JsonProperty("content_type")
        String contentType,
        Collection<String> parts
) {
}
