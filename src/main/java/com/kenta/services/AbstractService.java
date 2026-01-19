package com.kenta.services;

import com.hypixel.hytale.protocol.ItemWithAllMetadata;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.kenta.data.StreamData;
import com.kenta.libs.ColorHelper;

import java.awt.*;
import java.util.List;

public abstract class AbstractService implements InterfaceService {
    public StreamData streamData;
    public Status status;
    public String username;

    private final PlayerRef playerRef;
    private final String servicePrefix;
    private final Color serviceColor;

    protected AbstractService(StreamData streamData, PlayerRef playerRef, String servicePrefix, Color serviceColor) {
        this.streamData = streamData;
        this.playerRef = playerRef;
        this.username = playerRef.getUsername();
        this.servicePrefix = servicePrefix;
        this.serviceColor = serviceColor;
        this.status = Status.DISCONNECTED;
    }

    @Override
    public void connect() {}

    @Override
    public void disconnect() { this.status = Status.DISCONNECTED; }

    @Override
    public void sendMessage(Message message) { this.playerRef.sendMessage(message); }

    @Override
    public void sendChatMessage(AbstractChatMessage chat) {
        Color userColor = ColorHelper.parseHexColor(chat.color);
        String badgePrefix = getBadgePrefix(chat.badges);

        sendMessage(
                Message.join(
                        Message.translation(this.servicePrefix).color(this.serviceColor),
                        Message.translation(badgePrefix).color(getBadgeColor(chat.badges)),
                        Message.translation(chat.username).color(userColor),
                        Message.translation(" : ").color(Color.LIGHT_GRAY),
                        Message.translation(chat.message)
                )
        );
    }

    @Override
    public void sendNotification(String item, String title, String message, Color color) {
        var packetHandler = this.playerRef.getPacketHandler();
        var primaryMessage = Message.raw(title).bold(true).color(color);
        var secondaryMessage = Message.raw(message).color(color);
        var icon = new ItemStack(item, 1).toPacket();

        NotificationUtil.sendNotification(
                packetHandler,
                primaryMessage,
                secondaryMessage,
                (ItemWithAllMetadata) icon
        );
    }

    @Override
    public String getBadgePrefix(List<String> badges) { return ""; }

    @Override
    public Color getBadgeColor(List<String> badges) { return new Color(255, 255, 255); }
}
