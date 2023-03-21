package com.linweiyuan.chatgptapi.enums;

import com.linweiyuan.chatgptapi.misc.Constant;

public enum ErrorEnum {
    GET_CONVERSATIONS_ERROR(500, Constant.ERROR_MESSAGE_GET_CONVERSATIONS),
    GENERATE_TITLE_ERROR(500, Constant.ERROR_MESSAGE_GENERATE_TITLE),
    GET_CONTENT_ERROR(500, Constant.ERROR_MESSAGE_GET_CONTENT),
    UPDATE_CONVERSATION_ERROR(500, Constant.ERROR_MESSAGE_UPDATE_CONVERSATION),
    CLEAR_CONVERSATIONS_ERROR(500, Constant.ERROR_MESSAGE_CLEAR_CONVERSATIONS),
    FEEDBACK_MESSAGE_ERROR(500, Constant.ERROR_MESSAGE_FEEDBACK_MESSAGE),
    ;

    public final int code;
    public final String message;

    ErrorEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
