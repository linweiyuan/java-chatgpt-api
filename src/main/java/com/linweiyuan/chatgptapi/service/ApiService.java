package com.linweiyuan.chatgptapi.service;

import com.linweiyuan.chatgptapi.model.api.ChatCompletionsRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ApiService {
    Flux<String> chatCompletions(String authorization, ChatCompletionsRequest chatCompletionsRequest);

    Mono<String> checkCreditGrants(String authorization);
}
