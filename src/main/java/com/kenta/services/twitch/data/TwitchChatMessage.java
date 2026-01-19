package com.kenta.services.twitch.data;

import com.kenta.services.AbstractChatMessage;

import java.util.List;

public class TwitchChatMessage extends AbstractChatMessage {
    public TwitchChatMessage(String username, String message, String color, List<String> badges) {
        super(username, message, badges);
        this.color = color;
    }
}
