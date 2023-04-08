package com.linweiyuan.chatgptapi.model.chatgpt;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class Content {
    @JsonProperty("content_type")
    private String contentType;
    private List<String> parts;
}
