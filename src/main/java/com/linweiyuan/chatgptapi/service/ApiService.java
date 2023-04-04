package com.linweiyuan.chatgptapi.service;

import com.linweiyuan.chatgptapi.model.api.ChatCompletionsRequest;
import reactor.core.publisher.Flux;

public interface ApiService {
    Flux<String> chatCompletions(String authorization, ChatCompletionsRequest chatCompletionsRequest);
}
