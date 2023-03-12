package com.linweiyuan.chatgptapi.model;

import java.util.Collection;

public record MappingDetail(
        String id,
        Message message,
        String parent,
        Collection<String> children
) {
}
