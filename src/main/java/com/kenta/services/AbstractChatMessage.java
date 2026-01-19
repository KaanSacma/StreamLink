package com.kenta.services;

import java.util.List;

public abstract class AbstractChatMessage {
    public String username = "";
    public String message = "";
    public String color = "";
    public List<String> badges = List.of();

    public AbstractChatMessage(String username, String message, List<String> badges) {
        this.username = username;
        this.message = message;
        this.badges = badges;
        this.color = "#ffffff";
    }
}
