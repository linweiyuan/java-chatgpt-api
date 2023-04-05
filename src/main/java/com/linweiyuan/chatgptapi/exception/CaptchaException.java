 package com.linweiyuan.chatgptapi.exception;

import com.linweiyuan.chatgptapi.enums.ErrorEnum;
import lombok.Getter;

public class CaptchaException extends RuntimeException {
    @Getter
    private final int code;

    public CaptchaException(ErrorEnum errorEnum) {
        super(errorEnum.message);

        this.code = errorEnum.code;
    }
}
