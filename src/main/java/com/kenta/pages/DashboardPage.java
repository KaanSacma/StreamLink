package com.kenta.pages;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kenta.data.DashboardData;
import com.kenta.data.StreamData;
import com.kenta.libs.SLMessage;
import com.kenta.services.Status;
import com.kenta.services.StreamThread;
import com.kenta.services.twitch.Twitch;
import com.kenta.services.twitch.TwitchAuth;
import com.kenta.services.youtube.YouTube;
import com.kenta.services.youtube.YouTubeAuth;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

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
        // Twitch values
        String twitchChannel = this.streamData.getTwitchChannel();
        String twitchAccessToken = safeValue(this.streamData.getTwitchAccessToken());
        String twitchClientId = safeValue(this.streamData.getTwitchClientId());
        boolean isTwitchRunning = this.streamData.getIsTwitchRunning();

        // YouTube values
        String youtubeChannelId = safeValue(this.streamData.getYouTubeChannelId());
        String youtubeApiKey = safeValue(this.streamData.getYouTubeApiKey());
        boolean isYouTubeRunning = this.streamData.getIsYouTubeRunning();

        // Set Twitch form values
        uiCommandBuilder.set("#TwitchChannelInput.Value", twitchChannel);
        uiCommandBuilder.set("#TwitchAccessTokenInput.Value", twitchAccessToken);
        uiCommandBuilder.set("#TwitchClientIdInput.Value", twitchClientId);

        // Set YouTube form values
        uiCommandBuilder.set("#YouTubeChannelIdInput.Value", youtubeChannelId);
        uiCommandBuilder.set("#YouTubeApiKeyInput.Value", youtubeApiKey);
        
        updateStatusIndicator(uiCommandBuilder, isTwitchRunning);
        updateTwitchButtonStates(uiCommandBuilder, isTwitchRunning);

        updateYoutubeButtonStates(uiCommandBuilder, isYouTubeRunning);
    }

    private void setupEventBindings(UIEventBuilder uiEventBuilder) {
        // ========== PLATFORM TAB BUTTONS ==========
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabTwitch",
                new EventData().append("EventType", "tab_twitch_clicked"),
                false
        );

        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabYouTube",
                new EventData().append("EventType", "tab_youtube_clicked"),
                false
        );

        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabKick",
                new EventData().append("EventType", "tab_kick_clicked"),
                false
        );

        // ========== TWITCH BINDINGS ==========
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

        // Connect/Disconnect Buttons
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

        // Help Link
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#HelpLink",
                new EventData().append("EventType", "help_link_clicked"),
                false
        );

        // ========== YOUTUBE BINDINGS ==========
        // Channel ID Input
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#YouTubeChannelIdInput",
                new EventData()
                        .append("EventType", "youtube_channelId_input")
                        .append("@YouTubeChannelIdInput", "#YouTubeChannelIdInput.Value"),
                false
        );
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.FocusGained,
                "#YouTubeChannelIdInput",
                new EventData()
                        .append("EventType", "youtube_channelId_focus_gained")
                        .append("@YouTubeChannelIdInput", "#YouTubeChannelIdInput.Value"),
                false
        );
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.FocusLost,
                "#YouTubeChannelIdInput",
                new EventData()
                        .append("EventType", "youtube_channelId_focus_lost")
                        .append("@YouTubeChannelIdInput", "#YouTubeChannelIdInput.Value"),
                false
        );

        // API Key Input
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#YouTubeApiKeyInput",
                new EventData()
                        .append("EventType", "youtube_apiKey_input")
                        .append("@YouTubeApiKeyInput", "#YouTubeApiKeyInput.Value"),
                false
        );
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.FocusGained,
                "#YouTubeApiKeyInput",
                new EventData()
                        .append("EventType", "youtube_apiKey_focus_gained")
                        .append("@YouTubeApiKeyInput", "#YouTubeApiKeyInput.Value"),
                false
        );
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.FocusLost,
                "#YouTubeApiKeyInput",
                new EventData()
                        .append("EventType", "youtube_apiKey_focus_lost")
                        .append("@YouTubeApiKeyInput", "#YouTubeApiKeyInput.Value"),
                false
        );

        // Connect/Disconnect Buttons
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#YouTubeConnectButton",
                new EventData().append("EventType", "youtube_button_connect"),
                false
        );

        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#YouTubeDisconnectButton",
                new EventData().append("EventType", "youtube_button_disconnect"),
                false
        );

        // Help Link
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#YouTubeHelpLink",
                new EventData().append("EventType", "youtube_help_link_clicked"),
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
            // ========== TAB SWITCHING ==========
            case "tab_twitch_clicked":
                switchToTab("twitch");
                break;
            case "tab_youtube_clicked":
                switchToTab("youtube");
                break;
            case "tab_kick_clicked":
                switchToTab("kick");
                break;

            // ========== TWITCH EVENTS ==========
            case "twitch_channel_input":
                handleTwitchChannelInput(data.twitchChannelInput);
                break;

            case "twitch_accessToken_input":
                handleTwitchAccessTokenInput(data.twitchAccessTokenInput);
                break;
            case "twitch_accessToken_focus_gained":
                updateTwitchAccessTokenDisplay(false);
                break;
            case "twitch_accessToken_focus_lost":
                updateTwitchAccessTokenDisplay(true);
                break;

            case "twitch_clientID_input":
                handleTwitchClientIdInput(data.twitchClientIdInput);
                break;
            case "twitch_clientID_focus_gained":
                updateTwitchClientIdDisplay(false);
                break;
            case "twitch_clientID_focus_lost":
                updateTwitchClientIdDisplay(true);
                break;

            case "twitch_button_connect":
                handleTwitchConnectButton();
                break;
            case "twitch_button_disconnect":
                handleTwitchDisconnectButton();
                break;
            case "help_link_clicked":
                handleTwitchHelpLinkClick();
                break;

            // ========== YOUTUBE EVENTS ==========
            case "youtube_channelId_input":
                handleYouTubeChannelIdInput(data.youtubeChannelIdInput);
                break;
            case "youtube_channelId_focus_gained":
                updateYouTubeChannelIdDisplay(false);
                break;
            case "youtube_channelId_focus_lost":
                updateYouTubeChannelIdDisplay(true);
                break;

            case "youtube_apiKey_input":
                handleYouTubeApiKeyInput(data.youtubeApiKeyInput);
                break;
            case "youtube_apiKey_focus_gained":
                updateYouTubeApiKeyDisplay(false);
                break;
            case "youtube_apiKey_focus_lost":
                updateYouTubeApiKeyDisplay(true);
                break;

            case "youtube_button_connect":
                handleYouTubeConnectButton();
                break;
            case "youtube_button_disconnect":
                handleYouTubeDisconnectButton();
                break;
            case "youtube_help_link_clicked":
                handleYouTubeHelpLinkClick();
                break;

            default:
                break;
        }
    }

    // ========== TWITCH HANDLERS ==========
    private void handleTwitchChannelInput(String input) {
        String channel = (input != null && !input.isEmpty()) ? input : "";
        this.streamData.setTwitchChannel(channel);
    }

    private void handleTwitchAccessTokenInput(String input) {
        String accessToken = (input != null && !input.isEmpty()) ? input : "";
        this.streamData.setTwitchAccessToken(accessToken);
    }

    private void handleTwitchClientIdInput(String input) {
        String clientId = (input != null && !input.isEmpty()) ? input : "";
        this.streamData.setTwitchClientId(clientId);
    }

    private void handleTwitchConnectButton() {
        String username = playerRef.getUsername();

        assert streamData != null;

        if (streamData.getTwitchChannel().isEmpty()) {
            playerRef.sendMessage(SLMessage.formatMessageWithError("Set your channel first: /streamlink twitch channel <name>"));
            playerRef.sendMessage(SLMessage.formatMessageWithLink("Check this setup guide: ", "https://kentatetsu.gitbook.io/streamlink/guides/twitch-setup-guide"));
            return;
        }

        if (streamData.getTwitchClientId().isEmpty() || streamData.getTwitchAccessToken().isEmpty()) {
            playerRef.sendMessage(SLMessage.formatMessageWithError("Run setup first: /streamlink twitch setup <client_id> <access_token>"));
            playerRef.sendMessage(SLMessage.formatMessageWithLink("Check this setup guide: ", "https://kentatetsu.gitbook.io/streamlink/guides/twitch-setup-guide"));
            return;
        }

        if (StreamThread.isUserHasTwitchThread(username)) {
            playerRef.sendMessage(SLMessage.formatMessageWithError("Already connected!"));
            return;
        }

        setTwitchConnectingState();
        playerRef.sendMessage(SLMessage.formatMessage("Validating credentials..."));

        new Thread(() -> {
            try {
                if (!TwitchAuth.validateToken(streamData.getTwitchAccessToken())) {
                    playerRef.sendMessage(SLMessage.formatMessage("Invalid or expired access token!"));
                    playerRef.sendMessage(SLMessage.formatMessageWithLink("Check this troubleshooting guide: ", "https://kentatetsu.gitbook.io/streamlink/guides/twitch-setup-guide/troubleshooting"));
                    setTwitchDisconnectedState();
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

                Twitch twitch = new Twitch(streamData, playerRef);
                StreamThread.putToTwitch(username, twitch);
                twitch.connect();
                setTwitchConnectedState();
            } catch (Exception e) {
                setTwitchDisconnectedState();
                playerRef.sendMessage(SLMessage.formatMessageWithError("Connection failed: " + e.getMessage()));
                playerRef.sendMessage(SLMessage.formatMessageWithLink("Check this troubleshooting guide: ", "https://kentatetsu.gitbook.io/streamlink/guides/twitch-setup-guide/troubleshooting"));
                e.printStackTrace();
            }
        }).start();
    }

    private void handleTwitchDisconnectButton() {
        String username = playerRef.getUsername();

        if (!StreamThread.isUserHasTwitchThread(username)) {
            playerRef.sendMessage(SLMessage.formatMessageWithError("Not connected! Connect first: /streamlink twitch connect"));
            return;
        }

        StreamThread.disconnectTwitch(username);
        setTwitchDisconnectedState();
    }

    private void handleTwitchHelpLinkClick() {
        playerRef.sendMessage(SLMessage.formatMessageWithLink("Check this setup guide: ", "https://kentatetsu.gitbook.io/streamlink/guides/twitch-setup-guide"));
    }

    // ========== YOUTUBE HANDLERS ==========
    private void handleYouTubeChannelIdInput(String input) {
        String channelId = (input != null && !input.isEmpty()) ? input : "";
        this.streamData.setYouTubeChannelId(channelId);
    }

    private void handleYouTubeApiKeyInput(String input) {
        String apiKey = (input != null && !input.isEmpty()) ? input : "";
        this.streamData.setYouTubeApiKey(apiKey);
    }

    private void handleYouTubeConnectButton() {
        String username = playerRef.getUsername();

        assert streamData != null;

        if (streamData.getYouTubeChannelId().isEmpty()) {
            playerRef.sendMessage(SLMessage.formatMessageWithError("Set your channel ID first"));
            playerRef.sendMessage(SLMessage.formatMessageWithLink("Check this setup guide: ", "https://kentatetsu.gitbook.io/streamlink/guides/twitch-setup-guide"));
            return;
        }

        if (streamData.getYouTubeApiKey().isEmpty()) {
            playerRef.sendMessage(SLMessage.formatMessageWithError("Set your API key first"));
            playerRef.sendMessage(SLMessage.formatMessageWithLink("Check this setup guide: ", "https://kentatetsu.gitbook.io/streamlink/guides/twitch-setup-guide"));
            return;
        }

        if (StreamThread.isUserHasYouTubeThread(username)) {
            playerRef.sendMessage(SLMessage.formatMessageWithError("Already connected!"));
            return;
        }

        setYouTubeConnectingState();
        playerRef.sendMessage(SLMessage.formatMessage("Validating credentials..."));

        new Thread(() -> {
            try {
                if (!YouTubeAuth.validateApiKey(streamData.getYouTubeApiKey())) {
                    playerRef.sendMessage(SLMessage.formatMessage("Invalid or expired API key!"));
                    playerRef.sendMessage(SLMessage.formatMessageWithLink("Check this troubleshooting guide: ", "https://kentatetsu.gitbook.io/streamlink/guides/youtube-setup-guide/troubleshooting"));
                    setYouTubeDisconnectedState();
                    return;
                }

                playerRef.sendMessage(SLMessage.formatMessage("API Key validated!"));
                playerRef.sendMessage(SLMessage.formatMessage("Connecting to YouTube live chat..."));

                YouTube youtube = new YouTube(streamData, playerRef);
                StreamThread.putToYouTube(username, youtube);
                youtube.connect();
                Thread.sleep(100);
                if (youtube.status == Status.CONNECTED)
                    setYouTubeConnectedState();
                else
                    setYouTubeDisconnectedState();
            } catch (Exception e) {
                setYouTubeDisconnectedState();
                playerRef.sendMessage(SLMessage.formatMessageWithError("Connection failed: " + e.getMessage()));
                playerRef.sendMessage(SLMessage.formatMessageWithLink("Check this troubleshooting guide: ", "https://kentatetsu.gitbook.io/streamlink/guides/youtube-setup-guide/troubleshooting"));
                e.printStackTrace();
            }
        }).start();
    }

    private void handleYouTubeDisconnectButton() {
        String username = playerRef.getUsername();

        if (!StreamThread.isUserHasYouTubeThread(username)) {
            playerRef.sendMessage(SLMessage.formatMessageWithError("Not connected! Connect first: /streamlink youtube connect"));
            return;
        }

        StreamThread.disconnectYouTube(username);
        setYouTubeDisconnectedState();
    }

    private void handleYouTubeHelpLinkClick() {
        playerRef.sendMessage(SLMessage.formatMessageWithLink("Check this setup guide: ", "https://kentatetsu.gitbook.io/streamlink/guides/youtube-setup-guide"));
    }

    // ========== TWITCH UI UPDATES ==========
    private void setTwitchConnectingState() {
        UICommandBuilder builder = new UICommandBuilder();
        updateStatusIndicator(builder, CONNECTING_COLOR, "Connecting...");
        updateTwitchButtonStates(builder, false);
        this.sendUpdate(builder, null, false);
    }

    private void setTwitchConnectedState() {
        UICommandBuilder builder = new UICommandBuilder();
        this.streamData.setIsTwitchRunning(true);
        updateStatusIndicator(builder, true);
        updateTwitchButtonStates(builder, true);
        this.sendUpdate(builder, null, false);
    }

    private void setTwitchDisconnectedState() {
        UICommandBuilder builder = new UICommandBuilder();
        this.streamData.setIsTwitchRunning(false);
        updateStatusIndicator(builder, false);
        updateTwitchButtonStates(builder, false);
        this.sendUpdate(builder, null, false);
    }

    private void updateStatusIndicator(UICommandBuilder builder, boolean isConnected) {
        String color = isConnected ? CONNECTED_COLOR : DISCONNECTED_COLOR;
        String status = isConnected ? "Connected" : "Disconnected";
        updateStatusIndicator(builder, color, status);
    }

    private void updateStatusIndicator(UICommandBuilder builder, String color, String status) {
        builder.set("#StatusDot.Background", color);
        builder.set("#StatusLabel.Text", status);
    }

    private void updateTwitchButtonStates(UICommandBuilder builder, boolean isConnected) {
        builder.set("#TwitchConnectButton.Visible", !isConnected);
        builder.set("#TwitchDisconnectButton.Visible", isConnected);
    }

    private void updateTwitchAccessTokenDisplay(boolean masked) {
        UICommandBuilder builder = new UICommandBuilder();
        String displayValue = masked
                ? safeValue(this.streamData.getTwitchAccessToken())
                : this.streamData.getTwitchAccessToken();

        builder.set("#TwitchAccessTokenInput.Value", displayValue);
        this.sendUpdate(builder, null, false);
    }

    private void updateTwitchClientIdDisplay(boolean masked) {
        UICommandBuilder builder = new UICommandBuilder();
        String displayValue = masked
                ? safeValue(this.streamData.getTwitchClientId())
                : this.streamData.getTwitchClientId();

        builder.set("#TwitchClientIdInput.Value", displayValue);
        this.sendUpdate(builder, null, false);
    }

    // ========== YOUTUBE UI UPDATES ==========
    private void setYouTubeConnectingState() {
        UICommandBuilder builder = new UICommandBuilder();
        this.updateStatusIndicator(builder, CONNECTING_COLOR, "Connecting...");
        this.updateYoutubeButtonStates(builder, false);
        this.sendUpdate(builder, null, false);
    }

    private void setYouTubeConnectedState() {
        UICommandBuilder builder = new UICommandBuilder();
        this.streamData.setIsYouTubeRunning(true);
        this.updateStatusIndicator(builder, true);
        this.updateYoutubeButtonStates(builder, true);
        this.sendUpdate(builder, null, false);
    }

    private void setYouTubeDisconnectedState() {
        UICommandBuilder builder = new UICommandBuilder();
        this.streamData.setIsYouTubeRunning(false);
        this.updateStatusIndicator(builder, false);
        this.updateYoutubeButtonStates(builder, false);
        this.sendUpdate(builder, null, false);
    }

    private void updateYoutubeButtonStates(UICommandBuilder builder, boolean isConnected) {
        builder.set("#YouTubeConnectButton.Visible", !isConnected);
        builder.set("#YouTubeDisconnectButton.Visible", isConnected);
    }

    private void updateYouTubeChannelIdDisplay(boolean masked) {
        UICommandBuilder builder = new UICommandBuilder();
        String displayValue = masked
                ? safeValue(this.streamData.getYouTubeChannelId())
                : this.streamData.getYouTubeChannelId();

        builder.set("#YouTubeChannelIdInput.Value", displayValue);
        this.sendUpdate(builder, null, false);
    }

    private void updateYouTubeApiKeyDisplay(boolean masked) {
        UICommandBuilder builder = new UICommandBuilder();
        String displayValue = masked
                ? safeValue(this.streamData.getYouTubeApiKey())
                : this.streamData.getYouTubeApiKey();

        builder.set("#YouTubeApiKeyInput.Value", displayValue);
        this.sendUpdate(builder, null, false);
    }

    // ========== TAB SWITCHING ==========
    private void switchToTab(String platform) {
        UICommandBuilder builder = new UICommandBuilder();
        String username = playerRef.getUsername();

        builder.set("#TwitchContent.Visible", false);
        builder.set("#YouTubeContent.Visible", false);
        builder.set("#KickContent.Visible", false);

        builder.set("#TabTwitchActive.Visible", false);
        builder.set("#TabYouTubeActive.Visible", false);
        builder.set("#TabKickActive.Visible", false);

        switch (platform) {
            case "twitch":
                Twitch twitch = StreamThread.getTwitch().get(username);

                builder.set("#TwitchContent.Visible", true);
                builder.set("#TabTwitchActive.Visible", true);
                this.updateStatusIndicator(builder, StreamThread.isUserHasTwitchThread(username) && twitch.status == Status.CONNECTED);
                break;
            case "youtube":
                YouTube youtube = StreamThread.getYouTube().get(username);

                builder.set("#YouTubeContent.Visible", true);
                builder.set("#TabYouTubeActive.Visible", true);
                this.updateStatusIndicator(builder, StreamThread.isUserHasYouTubeThread(username) && youtube.status == Status.CONNECTED);
                break;
            case "kick":
                builder.set("#KickContent.Visible", true);
                builder.set("#TabKickActive.Visible", true);
                this.updateStatusIndicator(builder, false);
                break;
        }

        this.sendUpdate(builder, null, false);
    }

    // ========== UTILITIES ==========
    private String safeValue(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return "*".repeat(Math.min(value.length(), 32));
    }
}