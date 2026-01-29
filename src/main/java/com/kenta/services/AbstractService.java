package com.kenta.services;

import com.google.gson.JsonObject;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.ItemWithAllMetadata;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.kenta.StreamLink;
import com.kenta.actions.ActionManager;
import com.kenta.actions.context.ActionContext;
import com.kenta.data.StreamData;
import com.kenta.enums.Status;
import com.kenta.libs.ColorHelper;

import java.awt.*;
import java.util.List;

public abstract class AbstractService implements InterfaceService {
    public StreamData streamData;
    public Status status;
    public String username;

    protected final ActionManager actionManager = ActionManager.getInstance();
    protected Ref<EntityStore> entityRef;
    protected Store<EntityStore> entityStore;

    private final PlayerRef playerRef;
    private final String servicePrefix;
    private final Color serviceColor;

    protected AbstractService(
            StreamData streamData,
            Ref<EntityStore> entityRef,
            Store<EntityStore> entityStore,
            PlayerRef playerRef,
            String servicePrefix,
            Color serviceColor
    ) {
        this.streamData = streamData;
        this.entityRef = entityRef;
        this.entityStore = entityStore;
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

    protected abstract String getPlatformName();

    protected void triggerActionEvent(String eventType, JsonObject eventData) {
        ActionContext context = ActionContext.builder()
                .playerRef(this.playerRef)
                .platform(getPlatformName()) // "twitch", "youtube", "kick"
                .eventType(eventType)
                .eventData(eventData)
                .build();
        World world = Universe.get().getWorld(playerRef.getWorldUuid());

        world.execute(() -> {
            actionManager.processEvent(context, entityRef, entityStore);
        });
    }

    protected void triggerChatAction(AbstractChatMessage chatMessage) {
        ActionContext context = ActionContext.builder()
                .playerRef(this.playerRef)
                .platform(getPlatformName())
                .eventType("chat_message")
                .chatMessage(chatMessage)
                .build();
        World world = Universe.get().getWorld(playerRef.getWorldUuid());

        world.execute(() -> {
            actionManager.processEvent(context, entityRef, entityStore);
        });
    }
}
