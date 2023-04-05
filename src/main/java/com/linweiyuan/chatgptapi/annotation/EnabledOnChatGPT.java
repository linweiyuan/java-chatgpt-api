 package com.linweiyuan.chatgptapi.annotation;

import com.linweiyuan.chatgptapi.condition.EnabledOnChatGPTCondition;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE,ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(EnabledOnChatGPTCondition.class)
public @interface EnabledOnChatGPT {
}
