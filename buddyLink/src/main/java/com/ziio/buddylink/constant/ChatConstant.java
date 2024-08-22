package com.ziio.buddylink.constant;

public class ChatConstant {
    private ChatConstant() {
    }

    /**
     * 私聊
     */
    public static final int PRIVATE_CHAT = 1;

    /**
     * 队伍群聊
     */

    public static final int TEAM_CHAT = 2;
    /**
     * 大厅聊天
     */
    public static final int HALL_CHAT = 3;

    /**
     * 缓存聊天大厅
     */
    public static final String CACHE_CHAT_HALL = "buddy:chat:chat_records:chat_hall";

    /**
     * 缓存私人聊天
     */
    public static final String CACHE_CHAT_PRIVATE = "buddy:chat:chat_records:chat_private:";

    /**
     * 缓存聊天团队
     */
    public static final String CACHE_CHAT_TEAM = "buddy:chat:chat_records:chat_team:";
}
