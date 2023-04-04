package com.linweiyuan.chatgptapi.controller;

import com.linweiyuan.chatgptapi.annotation.PreCheck;
import com.linweiyuan.chatgptapi.misc.Constant;
import com.linweiyuan.chatgptapi.model.api.ChatCompletionsRequest;
import com.linweiyuan.chatgptapi.service.ApiService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@SuppressWarnings("unused")
@PreCheck
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
}
