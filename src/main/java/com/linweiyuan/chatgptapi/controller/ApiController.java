package com.linweiyuan.chatgptapi.controller;

import com.linweiyuan.chatgptapi.misc.Constant;
import com.linweiyuan.chatgptapi.model.api.ChatCompletionsRequest;
import com.linweiyuan.chatgptapi.service.ApiService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SuppressWarnings("unused")
@RestController
public class ApiController {
    private final ApiService apiService;

    public ApiController(ApiService apiService) {
        this.apiService = apiService;
    }

    @PostMapping(value = Constant.API_CHAT_COMPLETIONS, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatCompletions(
            @RequestHeader String authorization,
            @RequestBody ChatCompletionsRequest chatCompletionsRequest
    ) {
        return apiService.chatCompletions(authorization, chatCompletionsRequest);
    }

    @GetMapping(Constant.API_CHECK_CREDIT_GRANTS)
    public Mono<String> checkCreditGrants(@RequestHeader String authorization) {
        return apiService.checkCreditGrants(authorization);
    }
}
