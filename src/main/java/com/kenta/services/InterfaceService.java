package com.kenta.services;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.kenta.data.StreamData;

import java.awt.*;
import java.util.List;

public interface InterfaceService {
    void connect();
    void disconnect();

    void sendMessage(Message message);
    void sendChatMessage(AbstractChatMessage chat);
    void sendNotification(String item, String title, String message, Color color);

    String getBadgePrefix(List<String> badges);
    Color getBadgeColor(List<String> badges);
}
