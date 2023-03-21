package com.linweiyuan.chatgptapi.model;

public record Message(
        Author author,
        Content content,
        String id,
        String role
) {
}
