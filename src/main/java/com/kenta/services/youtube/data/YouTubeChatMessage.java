package com.kenta.services.youtube.data;

import com.kenta.services.AbstractChatMessage;

import java.util.List;

public class YouTubeChatMessage extends AbstractChatMessage {
    public String messageId = "";

    public YouTubeChatMessage(String messageId, String username, String message, List<String> badges) {
        super(username, message, badges);
        this.messageId = messageId;
    }
}
