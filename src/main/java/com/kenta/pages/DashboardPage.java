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
import com.kenta.libs.SLMessage;
import com.kenta.services.twitch.Twitch;
import com.kenta.services.twitch.TwitchAuth;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import static com.kenta.StreamLink.*;

public class DashboardPage extends InteractiveCustomUIPage<DashboardData> {

    private final StreamData streamData;
    private static final String CONNECTED_COLOR = "#00D166";
    private static final String DISCONNECTED_COLOR = "#E74C3C";
    private static final String CONNECTING_COLOR = "#F39C12";

    public DashboardPage(PlayerRef playerRef, StreamData streamData) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, DashboardData.CODEC);
        this.streamData = streamData;
    }

    @Override
    public void build(
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl UICommandBuilder uiCommandBuilder,
            @NonNullDecl UIEventBuilder uiEventBuilder,
            @NonNullDecl Store<EntityStore> store
    ) {
        uiCommandBuilder.append("Pages/SL_DashboardPage.ui");

        initializeValues(uiCommandBuilder);
        setupEventBindings(uiEventBuilder);
    }

    private void initializeValues(UICommandBuilder uiCommandBuilder) {
        String channel = this.streamData.getTwitchChannel();
        String accessToken = safeValue(this.streamData.getTwitchAccessToken());
        String clientId = safeValue(this.streamData.getTwitchClientId());
        boolean isRunning = this.streamData.getIsTwitchRunning();

        // Set form values
        uiCommandBuilder.set("#TwitchChannelInput.Value", channel);
        uiCommandBuilder.set("#TwitchAccessTokenInput.Value", accessToken);
        uiCommandBuilder.set("#TwitchClientIdInput.Value", clientId);

        // Update status indicator
        updateStatusIndicator(uiCommandBuilder, isRunning);

        // Update button states
        updateButtonStates(uiCommandBuilder, isRunning);
    }

    private void setupEventBindings(UIEventBuilder uiEventBuilder) {
        // Channel Input
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.FocusLost,
                "#TwitchChannelInput",
                new EventData()
                        .append("EventType", "twitch_channel_input")
                        .append("@TwitchChannelInput", "#TwitchChannelInput.Value"),
                false
        );

        // Access Token Input
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#TwitchAccessTokenInput",
                new EventData()
                        .append("EventType", "twitch_accessToken_input")
                        .append("@TwitchAccessTokenInput", "#TwitchAccessTokenInput.Value"),
                false
        );
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.FocusGained,
                "#TwitchAccessTokenInput",
                new EventData()
                        .append("EventType", "twitch_accessToken_focus_gained")
                        .append("@TwitchAccessTokenInput", "#TwitchAccessTokenInput.Value"),
                false
        );
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.FocusLost,
                "#TwitchAccessTokenInput",
                new EventData()
                        .append("EventType", "twitch_accessToken_focus_lost")
                        .append("@TwitchAccessTokenInput", "#TwitchAccessTokenInput.Value"),
                false
        );

        // Client ID Input
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#TwitchClientIdInput",
                new EventData()
                        .append("EventType", "twitch_clientID_input")
                        .append("@TwitchClientIdInput", "#TwitchClientIdInput.Value"),
                false
        );
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.FocusGained,
                "#TwitchClientIdInput",
                new EventData()
                        .append("EventType", "twitch_clientID_focus_gained")
                        .append("@TwitchClientIdInput", "#TwitchClientIdInput.Value"),
                false
        );
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.FocusLost,
                "#TwitchClientIdInput",
                new EventData()
                        .append("EventType", "twitch_clientID_focus_lost")
                        .append("@TwitchClientIdInput", "#TwitchClientIdInput.Value"),
                false
        );

        // Connect Button
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TwitchConnectButton",
                new EventData().append("EventType", "twitch_button_connect"),
                false
        );

        // Disconnect Button
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TwitchDisconnectButton",
                new EventData().append("EventType", "twitch_button_disconnect"),
                false
        );

        // Help Link
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#HelpLink",
                new EventData().append("EventType", "help_link_clicked"),
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
                handleChannelInput(data.twitchChannelInput);
                break;

            case "twitch_accessToken_input":
                handleAccessTokenInput(data.twitchAccessTokenInput);
                break;
            case "twitch_accessToken_focus_gained":
                updateAccessTokenDisplay(false);
                break;
            case "twitch_accessToken_focus_lost":
                updateAccessTokenDisplay(true);
                break;

            case "twitch_clientID_input":
                handleClientIdInput(data.twitchClientIdInput);
                break;
            case "twitch_clientID_focus_gained":
                updateClientIdDisplay(false);
                break;
            case "twitch_clientID_focus_lost":
                updateClientIdDisplay(true);
                break;

            case "twitch_button_connect":
                handleConnectButton(ref, store);
                break;
            case "twitch_button_disconnect":
                handleDisconnectButton();
                break;
            case "help_link_clicked":
                handleHelpLinkClick();
                break;
            default:
                break;
        }
    }

    private void handleChannelInput(String input) {
        String channel = (input != null && !input.isEmpty()) ? input : "";
        this.streamData.setTwitchChannel(channel);
    }

    private void handleAccessTokenInput(String input) {
        String accessToken = (input != null && !input.isEmpty()) ? input : "";
        this.streamData.setTwitchAccessToken(accessToken);
    }

    private void handleClientIdInput(String input) {
        String clientId = (input != null && !input.isEmpty()) ? input : "";
        this.streamData.setTwitchClientId(clientId);
    }

    private void handleConnectButton(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        String username = playerRef.getUsername();

        assert streamData != null;

        if (streamData.getTwitchChannel().isEmpty()) {
            playerRef.sendMessage(SLMessage.formatMessageWithError("Set your channel first: /streamlink twitch channel <name>"));
            return;
        }

        if (streamData.getTwitchClientId().isEmpty() || streamData.getTwitchAccessToken().isEmpty()) {
            playerRef.sendMessage(SLMessage.formatMessageWithError("Run setup first: /streamlink twitch setup <client_id> <access_token>"));
            playerRef.sendMessage(SLMessage.formatMessageWithLink("Please check this link: ", "https://twitchtokengenerator.com/quick/HvO1CktuVV"));
            return;
        }

        if (getPlayersTwitch().containsKey(username)) {
            playerRef.sendMessage(SLMessage.formatMessageWithError("Already connected!"));
            return;
        }

        setConnectingState();
        playerRef.sendMessage(SLMessage.formatMessage("Validating credentials..."));

        new Thread(() -> {
            try {
                if (!TwitchAuth.validateToken(streamData.getTwitchAccessToken())) {
                    playerRef.sendMessage(SLMessage.formatMessage("Invalid or expired access token!"));
                    playerRef.sendMessage(SLMessage.formatMessageWithLink("Please generate a new access token with ", "https://twitchtokengenerator.com/quick/HvO1CktuVV"));
                    return;
                }

                playerRef.sendMessage(SLMessage.formatMessage("Token validated!"));
                playerRef.sendMessage(SLMessage.formatMessage("Connecting to Twitch..."));

                String broadcasterId = TwitchAuth.getBroadcasterId(
                        streamData.getTwitchChannel(),
                        streamData.getTwitchClientId(),
                        streamData.getTwitchAccessToken()
                );

                streamData.setBroadcasterId(broadcasterId);

                playerRef.sendMessage(SLMessage.formatMessage("Authentication successful!"));
                playerRef.sendMessage(SLMessage.formatMessage("Connecting to chat and events..."));

                Twitch twitch = new Twitch();
                addPlayersTwitch(username, twitch);
                twitch.connectWithEvents(streamData, player);
                setConnectedState();
            } catch (Exception e) {
                setDisconnectedState();
                playerRef.sendMessage(SLMessage.formatMessageWithError("Connection failed: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void handleDisconnectButton() {
        String username = playerRef.getUsername();

        if (!getPlayersTwitch().containsKey(username)) {
            playerRef.sendMessage(SLMessage.formatMessageWithError("Not connected! Connect first: /streamlink twitch connect"));
            return;
        }

        disconnectPlayersTwitch(username);
        setDisconnectedState();
    }

    private void handleHelpLinkClick() {
        playerRef.sendMessage(SLMessage.formatMessageWithLink("Get your credentials here: ", "https://twitchtokengenerator.com/quick/HvO1CktuVV"));
    }


    // UI Update Methods
    private void setConnectingState() {
        UICommandBuilder builder = new UICommandBuilder();
        updateStatusIndicator(builder, false, CONNECTING_COLOR, "Connecting...");
        updateButtonStates(builder, false);
        this.sendUpdate(builder, null, false);
    }

    private void setConnectedState() {
        UICommandBuilder builder = new UICommandBuilder();
        this.streamData.setIsTwitchRunning(true);
        updateStatusIndicator(builder, true);
        updateButtonStates(builder, true);
        this.sendUpdate(builder, null, false);
    }

    private void setDisconnectedState() {
        UICommandBuilder builder = new UICommandBuilder();
        this.streamData.setIsTwitchRunning(false);
        updateStatusIndicator(builder, false);
        updateButtonStates(builder, false);
        this.sendUpdate(builder, null, false);
    }

    private void updateStatusIndicator(UICommandBuilder builder, boolean isConnected) {
        String color = isConnected ? CONNECTED_COLOR : DISCONNECTED_COLOR;
        String status = isConnected ? "Connected" : "Disconnected";
        updateStatusIndicator(builder, isConnected, color, status);
    }

    private void updateStatusIndicator(UICommandBuilder builder, boolean isConnected, String color, String status) {
        builder.set("#StatusDot.Background", color);
        builder.set("#StatusLabel.Text", status);
    }

    private void updateButtonStates(UICommandBuilder builder, boolean isConnected) {
        builder.set("#TwitchConnectButton.Visible", !isConnected);
        builder.set("#TwitchDisconnectButton.Visible", isConnected);
    }

    private void updateAccessTokenDisplay(boolean masked) {
        UICommandBuilder builder = new UICommandBuilder();
        String displayValue = masked
                ? safeValue(this.streamData.getTwitchAccessToken())
                : this.streamData.getTwitchAccessToken();

        builder.set("#TwitchAccessTokenInput.Value", displayValue);
        this.sendUpdate(builder, null, false);
    }

    private void updateClientIdDisplay(boolean masked) {
        UICommandBuilder builder = new UICommandBuilder();
        String displayValue = masked
                ? safeValue(this.streamData.getTwitchClientId())
                : this.streamData.getTwitchClientId();

        builder.set("#TwitchClientIdInput.Value", displayValue);
        this.sendUpdate(builder, null, false);
    }

    private String safeValue(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return "*".repeat(Math.min(value.length(), 32));
    }
}
