package com.linweiyuan.chatgptapi.model.chatgpt;

public record Message(
        Author author,
        Content content,
        String id
) {
}
