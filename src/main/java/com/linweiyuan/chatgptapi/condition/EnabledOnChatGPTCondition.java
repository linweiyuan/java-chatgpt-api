package com.linweiyuan.chatgptapi.condition;

import com.linweiyuan.chatgptapi.misc.Constant;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

@SuppressWarnings({"NullableProblems"})
public class EnabledOnChatGPTCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return context.getEnvironment().getProperty(Constant.ENV_VAR_ENABLE_CHATGPT) != null;
    }
}
