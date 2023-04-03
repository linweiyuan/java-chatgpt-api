package com.linweiyuan.chatgptapi.misc;

public class Constant {
    public static final String CHATGPT_URL = "https://chat.openai.com";
    public static final String API_URL = CHATGPT_URL + "/backend-api";
    public static final String GET_CONVERSATIONS_URL = API_URL + "/conversations?offset=%d&limit=%d";
    public static final String START_CONVERSATIONS_URL = API_URL + "/conversation";
    public static final String GENERATE_TITLE_URL = API_URL + "/conversation/gen_title/%s";
    public static final String GET_CONVERSATION_CONTENT_URL = API_URL + "/conversation/%s";
    public static final String UPDATE_CONVERSATION_URL = API_URL + "/conversation/%s";
    public static final String CLEAR_CONVERSATIONS_URL = API_URL + "/conversations";
    public static final String FEEDBACK_MESSAGE_URL = API_URL + "/conversation/message_feedback";

    public static final int SCRIPT_EXECUTION_TIMEOUT = 10;

    public static final int CHECK_WELCOME_TEXT_TIMEOUT = 5;
    public static final int CHECK_CAPTCHA_TIMEOUT = 15;
    public static final int CHECK_CAPTCHA_INTERVAL = 1;
    public static final int CHECK_NEXT_INTERVAL = 5;

    public static final String DEFAULT_OFFSET = "0";
    public static final String DEFAULT_LIMIT = "20";

    public static final String ERROR_MESSAGE_GET_CONVERSATIONS = "Failed to get conversations.";
    public static final String ERROR_MESSAGE_GENERATE_TITLE = "Failed to generate title.";
    public static final String ERROR_MESSAGE_GET_CONTENT = "Failed to get content.";
    public static final String ERROR_MESSAGE_UPDATE_CONVERSATION = "Failed to update conversation.";
    public static final String ERROR_MESSAGE_CLEAR_CONVERSATIONS = "Failed to clear conversations.";
    public static final String ERROR_MESSAGE_FEEDBACK_MESSAGE = "Failed to add feedback.";
}
