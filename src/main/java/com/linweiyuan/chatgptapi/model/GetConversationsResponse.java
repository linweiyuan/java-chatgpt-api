package com.linweiyuan.chatgptapi.model;

import java.util.List;

public record GetConversationsResponse(
        List<GetConversationsResponseItem> items,

        int total,
        int limit,
        int offset
) {
}
