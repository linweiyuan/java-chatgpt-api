package com.linweiyuan.chatgptapi.model;

import java.util.Collection;

public record GetConversationsResponse(
        Collection<GetConversationsResponseItem> items,

        int total,
        int limit,
        int offset
) {
}
