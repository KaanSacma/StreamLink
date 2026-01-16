package com.kenta.services.twitch.data;

import java.util.List;

public record ChatMessage(String username, String message, String color, List<String> badges) {
    public ChatMessage(String username, String message, String color) {
        this(username, message, color, List.of());
    }
}
