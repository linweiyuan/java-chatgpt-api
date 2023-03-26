package com.linweiyuan.chatgptapi.misc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;

@Slf4j
public class LogUtil {
    public static void info(String msg) {
        log.info(AnsiOutput.toString(AnsiColor.GREEN, msg, AnsiColor.DEFAULT));
    }

    public static void warn(String msg) {
        log.warn(AnsiOutput.toString(AnsiColor.YELLOW, msg, AnsiColor.DEFAULT));
    }

    public static void error(String msg) {
        log.error(AnsiOutput.toString(AnsiColor.RED, msg, AnsiColor.DEFAULT));
    }
}
