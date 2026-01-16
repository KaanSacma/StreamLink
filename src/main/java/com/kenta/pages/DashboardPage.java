package com.kenta.pages;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kenta.data.DashboardData;
import com.kenta.data.StreamData;
import com.kenta.services.twitch.Twitch;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import static com.kenta.StreamLink.*;
import static com.kenta.StreamLink.connectPlayersTwitch;

public class DashboardPage extends InteractiveCustomUIPage<DashboardData> {

    private final StreamData streamData;

    public DashboardPage(PlayerRef playerRef, StreamData streamData) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, DashboardData.CODEC);
        this.streamData = streamData;
    }

    @Override
    public void build(
            @NonNullDecl Ref<EntityStore> ref,
            UICommandBuilder uiCommandBuilder,
            @NonNullDecl UIEventBuilder uiEventBuilder,
            @NonNullDecl Store<EntityStore> store
    ) {
        uiCommandBuilder.append("Pages/SL_DashboardPage.ui");

        initTwitchValue(uiCommandBuilder);
        setupTwitchEventBuilder(uiEventBuilder);
    }

    private void initTwitchValue(UICommandBuilder uiCommandBuilder) {
        String channel = this.streamData.getTwitchChannel();
        String status = this.streamData.getIsTwitchRunning() ? "Connected" : "Disconnected";

        uiCommandBuilder.set("#TwitchChannelInput.Value", channel);
        uiCommandBuilder.set("#StatusLabel.Text", status);
    }

    private void setupTwitchEventBuilder(UIEventBuilder uiEventBuilder) {
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.FocusLost,
                "#TwitchChannelInput",
                new EventData()
                        .append("EventType", "twitch_channel_input")
                        .append("@TwitchChannelInput", "#TwitchChannelInput.Value"),
                false
        );

        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TwitchConnectButton",
                new EventData().append("EventType", "twitch_button_connect"),
                false
        );

        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TwitchDisconnectButton",
                new EventData().append("EventType", "twitch_button_disconnect"),
                false
        );
    }

    @Override
    public void handleDataEvent(
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl Store<EntityStore> store,
            DashboardData data
    ) {
        switch (data.eventType) {
            case "twitch_channel_input":
                String name = data.twitchChannelInput != null && !data.twitchChannelInput.isEmpty() ? data.twitchChannelInput : "";
                this.streamData.setTwitchChannel(name);
                return;
            case "twitch_button_connect":
                handleConnectButton(ref, store);
                return;
            case "twitch_button_disconnect":
                handleDisconnectButton();
                return;
            default:
        }
    }

    private void handleConnectButton(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        String username = playerRef.getUsername();


        if (this.streamData.getTwitchChannel().isEmpty()) {
            playerRef.sendMessage(Message.translation("[Error] Set your channel first: /streamlink twitch set <channel>"));
            return;
        }

        if (getPlayersTwitch().containsKey(username)) {
            playerRef.sendMessage(Message.translation("[Error] Already connected! Disconnect first: /streamlink twitch disconnect"));
            return;
        }

        Twitch twitch = new Twitch();
        addPlayersTwitch(username, twitch);
        connectPlayersTwitch(player, this.streamData, username);
    }

    private void handleDisconnectButton() {
        String username = playerRef.getUsername();

        if (!getPlayersTwitch().containsKey(username)) {
            playerRef.sendMessage(Message.translation("[Error] Not connected! Connect first: /streamlink twitch connect"));
            return;
        }

        disconnectPlayersTwitch(username);
    }
}
