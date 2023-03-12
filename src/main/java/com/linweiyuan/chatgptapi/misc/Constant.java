package com.linweiyuan.chatgptapi.misc;

@SuppressWarnings("SpellCheckingInspection")
public class Constant {
    public static final String SERVER_URL = "https://chat.openai.com";
    public static final String CHATGPT_URL = SERVER_URL + "/chat";
    public static final String API_URL = SERVER_URL + "/backend-api";
    public static final String GET_CONVERSATIONS_URL = API_URL + "/conversations?offset=%d&limit=%d";
    public static final String START_CONVERSATIONS_URL = API_URL + "/conversation";
    public static final String GEN_CONVERSATION_TITLE_URL = API_URL + "/conversation/gen_title/%s";
    public static final String GET_CONVERSATION_CONTENT_URL = API_URL + "/conversation/%s";
    public static final String UPDATE_CONVERSATION_URL = API_URL + "/conversation/%s";

    public static final String MODEL = "text-davinci-002-render-sha";

    public static final int SCRIPT_EXECUTION_TIMEOUT = 1;

    public static final String DEFAULT_OFFSET = "0";
    public static final String DEFAULT_LIMIT = "20";

    public static final int MAXIMUM_RETRY_COUNT = 3;
}
