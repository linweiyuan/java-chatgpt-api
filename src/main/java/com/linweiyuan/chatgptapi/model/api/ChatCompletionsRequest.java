package com.linweiyuan.chatgptapi.model.api;

import java.util.List;

public record ChatCompletionsRequest(
        String model,
        List<Message> messages,
        boolean stream
) {
}
