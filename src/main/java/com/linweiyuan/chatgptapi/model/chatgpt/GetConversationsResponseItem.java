package com.linweiyuan.chatgptapi.model.chatgpt;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public record GetConversationsResponseItem(
        String id,
        String title,
        @JsonProperty("create_time")
        Date createTime
) {
}
