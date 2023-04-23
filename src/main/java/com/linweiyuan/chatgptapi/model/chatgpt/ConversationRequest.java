package com.linweiyuan.chatgptapi.model.chatgpt;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class ConversationRequest {
    String action;
    List<Message> messages;
    String model;
    @JsonProperty("parent_message_id")
    String parentMessageId;
    @JsonProperty("conversation_id")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    String conversationId;
    @JsonProperty("timezone_offset_min")
    Integer timezoneOffsetMin;
    @JsonProperty("variant_purpose")
    String variantPurpose;

    @JsonProperty("continue_text")
    String continueText;
}
