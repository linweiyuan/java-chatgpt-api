package com.linweiyuan.chatgptapi.model.chatgpt;

import java.util.List;

public record GetConversationsResponse(
        List<GetConversationsResponseItem> items,

        int total,
        int limit,
        int offset
) {
}
