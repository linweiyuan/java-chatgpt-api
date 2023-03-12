package com.linweiyuan.chatgptapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record GetConversationContentResponse(
        @JsonProperty("current_node")
        String currentNode,
        Map<String, MappingDetail> mapping,
        String title
) {
}
