package com.linweiyuan.chatgptapi.service.impl;

import com.linweiyuan.chatgptapi.misc.Constant;
import com.linweiyuan.chatgptapi.model.api.ChatCompletionsRequest;
import com.linweiyuan.chatgptapi.service.ApiService;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.linweiyuan.chatgptapi.misc.HeaderUtil.getAuthorizationHeader;

@Service
public class ApiServiceImpl implements ApiService {
    private final WebClient webClient;

    public ApiServiceImpl() {
        this.webClient = WebClient.builder()
                .baseUrl(Constant.API_URL)
                .build();
    }

    @SneakyThrows
    @Override
    public Flux<String> chatCompletions(String authorization, ChatCompletionsRequest chatCompletionsRequest) {
        return webClient.post()
                .uri(Constant.API_CHAT_COMPLETIONS)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader(authorization))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chatCompletionsRequest)
                .retrieve()
                .bodyToFlux(String.class);
    }

    @Override
    public Mono<String> checkCreditGrants(String authorization) {
        return webClient.get()
                .uri(Constant.API_CHECK_CREDIT_GRANTS)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader(authorization))
                .retrieve()
                .bodyToMono(String.class);
    }
}
