package com.kenta.pages;

import com.kenta.data.StreamData;
import com.kenta.libs.SLMessage;
import com.kenta.enums.Status;
import com.kenta.services.StreamThread;
import com.kenta.services.twitch.Twitch;
import com.kenta.services.twitch.TwitchAuth;
import com.kenta.services.youtube.YouTube;
import com.kenta.services.youtube.YouTubeAuth;

import com.kenta.flowui.data.InteractiveData;
import com.kenta.flowui.core.UIBuilder;
import com.kenta.flowui.core.EventDispatcher;
import com.kenta.flowui.core.UIState;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Objects;

public class DashboardPage extends InteractiveCustomUIPage<InteractiveData> {

    private UIBuilder ui;
    private EventDispatcher dispatcher;

    private final UIState<String> twitchChannelName = new UIState<>("");
    private final UIState<String> twitchAccessToken = new UIState<>("");
    private final UIState<String> twitchClientId = new UIState<>("");

    private final UIState<String> youtubeApiKey = new UIState<>("");
    private final UIState<String> youtubeChannelId = new UIState<>("");

    private final StreamData streamData;
    private static final String CONNECTED_COLOR = "#00D166";
    private static final String DISCONNECTED_COLOR = "#E74C3C";
    private static final String CONNECTING_COLOR = "#F39C12";

    public DashboardPage(PlayerRef playerRef, StreamData streamData) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, InteractiveData.CODEC);
        this.streamData = streamData;
    }

    @Override
    public void build(
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl UICommandBuilder uiCommandBuilder,
            @NonNullDecl UIEventBuilder uiEventBuilder,
            @NonNullDecl Store<EntityStore> store
    ) {
        ui = new UIBuilder(uiCommandBuilder, "Pages/SL_DashboardPage.ui", this::sendUpdate);

        Status connexionStatus = this.streamData.getIsTwitchRunning() ? Status.CONNECTED : Status.DISCONNECTED;
        updateStatusIndicator(connexionStatus);
        buildTabSection();
        buildTwitchSection();
        buildYouTubeSection();

        ui.applyAll();
        dispatcher = new EventDispatcher(ui);

        setupTwitchReactiveListeners();
        setupYouTubeReactiveListeners();
    }

    private void buildTabSection() {
        ui.textButton("#TabTwitch").onClick(this::switchTabToTwitch).build();
        ui.textButton("#TabYouTube").onClick(this::switchTabToYouTube).build();
        ui.textButton("#TabKick").onClick(this::switchTabToKick).build();
        ui.group("#TwitchContent").visible(true).build();
        ui.group("#TabTwitchActive").visible(true).build();
        ui.group("#YouTubeContent").visible(false).build();
        ui.group("#TabYouTubeActive").visible(false).build();
        ui.group("#KickContent").visible(false).build();
        ui.group("#TabKickActive").visible(false).build();
    }

    private void buildTwitchSection() {
        twitchAccessToken.set(safeValue(streamData.getTwitchAccessToken()));
        twitchClientId.set(safeValue(streamData.getTwitchClientId()));
        twitchChannelName.set(streamData.getTwitchChannel());

        ui.textInput("#TwitchAccessTokenInput")
                .onChange(streamData::setTwitchAccessToken)
                .onFocusGained(_ -> {
                    twitchAccessToken.set(streamData.getTwitchAccessToken());
                })
                .onFocusLost(value -> {
                    twitchAccessToken.set(safeValue(value));
                })
                .value(twitchAccessToken.get())
        .build();

        ui.textInput("#TwitchClientIdInput")
                .onChange(streamData::setTwitchClientId)
                .onFocusGained(_ -> {
                    twitchClientId.set(streamData.getTwitchClientId());
                })
                .onFocusLost(value -> {
                    twitchClientId.set(safeValue(value));
                })
                .value(twitchClientId.get())
        .build();

        ui.textInput("#TwitchChannelInput")
                .onChange(streamData::setTwitchChannel)
                .value(twitchChannelName.get())
        .build();

        ui.button("#TwitchDisconnectButton")
                .onClick(this::handleTwitchDisconnectButton)
                .visible(this.streamData.getIsTwitchRunning())
        .build();
        ui.button("#TwitchConnectButton")
                .onClick(this::handleTwitchConnectButton)
                .visible(!this.streamData.getIsTwitchRunning())
        .build();
    }

    private void buildYouTubeSection() {
        youtubeApiKey.set(safeValue(streamData.getYouTubeApiKey()));
        youtubeChannelId.set(safeValue(streamData.getYouTubeChannelId()));

        ui.textInput("#YouTubeApiKeyInput")
                .onChange(streamData::setYouTubeApiKey)
                .onFocusGained(_ -> {
                    youtubeApiKey.set(streamData.getYouTubeApiKey());
                })
                .onFocusLost(value -> {
                    youtubeApiKey.set(safeValue(value));
                })
                .value(youtubeApiKey.get())
        .build();

        ui.textInput("#YouTubeChannelIdInput")
                .onChange(streamData::setYouTubeChannelId)
                .onFocusGained(_ -> {
                    youtubeChannelId.set(streamData.getYouTubeChannelId());
                })
                .onFocusLost(value -> {
                    youtubeChannelId.set(safeValue(value));
                })
                .value(youtubeChannelId.get())
        .build();

        ui.button("#YouTubeDisconnectButton")
                .onClick(this::handleYouTubeDisconnectButton)
                .visible(false)
        .build();
        ui.button("#YouTubeConnectButton")
                .onClick(this::handleYouTubeConnectButton)
                .visible(true)
        .build();
    }

    private void setupTwitchReactiveListeners() {
        twitchAccessToken.addListener(value -> {
            ui.textInput("#TwitchAccessTokenInput").value(value).update();
        });

        twitchClientId.addListener(value -> {
            ui.textInput("#TwitchClientIdInput").value(value).update();
        });
    }
    
    private void setupYouTubeReactiveListeners() {
        youtubeApiKey.addListener(value -> {
            ui.textInput("#YouTubeApiKeyInput").value(value).update();
        });

        youtubeChannelId.addListener(value -> {
            ui.textInput("#YouTubeChannelIdInput").value(value).update();
        });
    }

    private void switchTabToTwitch() {
        ui.group("#TwitchContent").visible(true).update();
        ui.group("#TabTwitchActive").visible(true).update();
        ui.group("#YouTubeContent").visible(false).update();
        ui.group("#TabYouTubeActive").visible(false).update();
        ui.group("#KickContent").visible(false).update();
        ui.group("#TabKickActive").visible(false).update();
        updateStatusIndicator(this.streamData.getIsTwitchRunning() ? Status.CONNECTED : Status.DISCONNECTED);
    }

    private void switchTabToYouTube() {
        ui.group("#TwitchContent").visible(false).update();
        ui.group("#TabTwitchActive").visible(false).update();
        ui.group("#YouTubeContent").visible(true).update();
        ui.group("#TabYouTubeActive").visible(true).update();
        ui.group("#KickContent").visible(false).update();
        ui.group("#TabKickActive").visible(false).update();
        updateStatusIndicator(this.streamData.getIsYouTubeRunning() ? Status.CONNECTED : Status.DISCONNECTED);
    }

    private void switchTabToKick() {
        ui.group("#TwitchContent").visible(false).update();
        ui.group("#TabTwitchActive").visible(false).update();
        ui.group("#YouTubeContent").visible(false).update();
        ui.group("#TabYouTubeActive").visible(false).update();
        ui.group("#KickContent").visible(true).update();
        ui.group("#TabKickActive").visible(true).update();
        updateStatusIndicator(Status.DISCONNECTED);
    }

    private void handleTwitchConnectButton() {
        String username = playerRef.getUsername();
        Ref<EntityStore> ref = playerRef.getReference();
        Store<EntityStore> store = Objects.requireNonNull(playerRef.getReference()).getStore();

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

        updateTwitchConnectionState(Status.CONNECTING);
        playerRef.sendMessage(SLMessage.formatMessage("Validating credentials..."));

        new Thread(() -> {
            try {
                if (!TwitchAuth.validateToken(streamData.getTwitchAccessToken())) {
                    playerRef.sendMessage(SLMessage.formatMessage("Invalid or expired access token!"));
                    playerRef.sendMessage(SLMessage.formatMessageWithLink("Check this troubleshooting guide: ", "https://kentatetsu.gitbook.io/streamlink/guides/twitch-setup-guide/troubleshooting"));
                    updateTwitchConnectionState(Status.DISCONNECTED);
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

                Twitch twitch = new Twitch(streamData, playerRef, ref, store);
                StreamThread.putToTwitch(username, twitch);
                twitch.connect();
                updateTwitchConnectionState(Status.CONNECTED);
            } catch (Exception e) {
                updateTwitchConnectionState(Status.DISCONNECTED);
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
        updateTwitchConnectionState(Status.DISCONNECTED);
    }

    private void updateTwitchConnectionState(Status status) {
        switch (status)  {
            case CONNECTED -> {
                ui.button("#TwitchDisconnectButton").visible(true).update();
                ui.button("#TwitchConnectButton").visible(false).update();
                this.streamData.setIsTwitchRunning(true);
            }
            case DISCONNECTED -> {
                ui.button("#TwitchDisconnectButton").visible(false).update();
                ui.button("#TwitchConnectButton").visible(true).update();
                this.streamData.setIsTwitchRunning(false);
            }
            case CONNECTING -> {
                ui.button("#TwitchDisconnectButton").visible(false).update();
                ui.button("#TwitchConnectButton").visible(false).update();
                this.streamData.setIsTwitchRunning(false);
            }
        }
        updateStatusIndicator(status);
    }

    private void handleYouTubeConnectButton() {
        String username = playerRef.getUsername();
        Ref<EntityStore> ref = playerRef.getReference();
        Store<EntityStore> store = Objects.requireNonNull(playerRef.getReference()).getStore();

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

        updateYouTubeConnectionState(Status.CONNECTING);
        playerRef.sendMessage(SLMessage.formatMessage("Validating credentials..."));

        new Thread(() -> {
            try {
                if (!YouTubeAuth.validateApiKey(streamData.getYouTubeApiKey())) {
                    playerRef.sendMessage(SLMessage.formatMessage("Invalid or expired API key!"));
                    playerRef.sendMessage(SLMessage.formatMessageWithLink("Check this troubleshooting guide: ", "https://kentatetsu.gitbook.io/streamlink/guides/youtube-setup-guide/troubleshooting"));
                    updateYouTubeConnectionState(Status.DISCONNECTED);
                    return;
                }

                playerRef.sendMessage(SLMessage.formatMessage("API Key validated!"));
                playerRef.sendMessage(SLMessage.formatMessage("Connecting to YouTube live chat..."));

                YouTube youtube = new YouTube(streamData, playerRef, ref, store);
                StreamThread.putToYouTube(username, youtube);
                youtube.connect();
                Thread.sleep(100);
                if (youtube.status == Status.CONNECTED)
                    updateYouTubeConnectionState(Status.CONNECTED);
                else
                    updateYouTubeConnectionState(Status.DISCONNECTED);
            } catch (Exception e) {
                updateYouTubeConnectionState(Status.DISCONNECTED);
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
        updateYouTubeConnectionState(Status.DISCONNECTED);
    }

    private void updateYouTubeConnectionState(Status status) {
        switch (status) {
            case CONNECTED -> {
                ui.button("#YouTubeDisconnectButton").visible(true).update();
                ui.button("#YouTubeConnectButton").visible(false).update();
                this.streamData.setIsTwitchRunning(true);
            }
            case DISCONNECTED -> {
                ui.button("#YouTubeDisconnectButton").visible(false).update();
                ui.button("#YouTubeConnectButton").visible(true).update();
                this.streamData.setIsTwitchRunning(false);
            }
            case CONNECTING -> {
                ui.button("#YouTubeDisconnectButton").visible(false).update();
                ui.button("#YouTubeConnectButton").visible(false).update();
                this.streamData.setIsTwitchRunning(false);
            }
        }
        updateStatusIndicator(status);
    }

    private void updateStatusIndicator(Status status) {
        switch (status) {
            case CONNECTED -> {
                ui.label("#StatusLabel").text("Connected").update();
                ui.group("#StatusDot").background(CONNECTED_COLOR).update();
            }
            case DISCONNECTED -> {
                ui.label("#StatusLabel").text("Disconnected").update();
                ui.group("#StatusDot").background(DISCONNECTED_COLOR).update();
            }
            case CONNECTING -> {
                ui.label("#StatusLabel").text("Connecting...").update();
                ui.group("#StatusDot").background(CONNECTING_COLOR).update();
            }
        }
    }

    @Override
    public void handleDataEvent(
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl Store<EntityStore> store,
            InteractiveData data
    ) {
        dispatcher.dispatch(data.eventId, data.value);
    }

    // ========== UTILITIES ==========
    private String safeValue(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return "*".repeat(Math.min(value.length(), 32));
    }
}
