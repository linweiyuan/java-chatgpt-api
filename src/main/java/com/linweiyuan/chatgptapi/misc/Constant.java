package com.linweiyuan.chatgptapi.misc;

public class Constant {
    public static final String ENV_VAR_CHATGPT = "CHATGPT";
    public static final String WELCOME_TEXT = "Welcome to ChatGPT";
    public static final String CHATGPT_URL = "https://chat.openai.com";
    public static final String CHATGPT_API_URL = CHATGPT_URL + "/backend-api";
    public static final String GET_CONVERSATIONS_URL = CHATGPT_API_URL + "/conversations?offset=%d&limit=%d";
    public static final String START_CONVERSATIONS_URL = CHATGPT_API_URL + "/conversation";
    public static final String GENERATE_TITLE_URL = CHATGPT_API_URL + "/conversation/gen_title/%s";
    public static final String GET_CONVERSATION_CONTENT_URL = CHATGPT_API_URL + "/conversation/%s";
    public static final String UPDATE_CONVERSATION_URL = CHATGPT_API_URL + "/conversation/%s";
    public static final String CLEAR_CONVERSATIONS_URL = CHATGPT_API_URL + "/conversations";
    public static final String FEEDBACK_MESSAGE_URL = CHATGPT_API_URL + "/conversation/message_feedback";
    public static final String GET_MODELS_URL = CHATGPT_API_URL + "/models";
    public static final String CHECK_ACCOUNT_URL = CHATGPT_API_URL + "/accounts/check";

    public static final String DEFAULT_OFFSET = "0";
    public static final String DEFAULT_LIMIT = "20";

    public static final String ERROR_MESSAGE_GET_CONVERSATIONS = "Failed to get conversations.";
    public static final String ERROR_MESSAGE_GENERATE_TITLE = "Failed to generate title.";
    public static final String ERROR_MESSAGE_GET_CONTENT = "Failed to get content.";
    public static final String ERROR_MESSAGE_UPDATE_CONVERSATION = "Failed to update conversation.";
    public static final String ERROR_MESSAGE_CLEAR_CONVERSATIONS = "Failed to clear conversations.";
    public static final String ERROR_MESSAGE_FEEDBACK_MESSAGE = "Failed to add feedback.";
    public static final String ERROR_MESSAGE_GET_MODELS = "Failed to get models.";
    public static final String ERROR_MESSAGE_CHECK_ACCOUNT = "Failed to check account.";
    public static final String ERROR_MESSAGE_ACCESS_DENIED = "Access denied.";

    public static final String DONE_FLAG = "[DONE]";

    public static final String API_URL = "https://api.openai.com";
    private static final String API_VERSION = "v1";
    public static final String API_CHAT_COMPLETIONS = API_VERSION + "/chat/completions";
    public static final String API_CHECK_CREDIT_GRANTS = "/dashboard/billing/credit_grants";
}
