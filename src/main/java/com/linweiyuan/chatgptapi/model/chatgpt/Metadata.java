 package com.linweiyuan.chatgptapi.model.chatgpt;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Metadata(
        @JsonProperty("finish_details")
        FinishDetails finishDetails
) {
}
