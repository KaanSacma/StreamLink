package com.kenta.services.youtube;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.kenta.data.StreamData;
import com.kenta.libs.ColorHelper;
import com.kenta.libs.SLMessage;
import com.kenta.services.AbstractService;
import com.kenta.services.Status;
import com.kenta.services.StreamThread;
import com.kenta.services.youtube.data.YouTubeChatMessage;

import java.awt.*;
import java.util.List;

public class YouTube extends AbstractService {
    private YouTubeChat chat;

    private final String MEMBER_ITEM = "Deco_Treasure";
    private final String SUPERCHAT_ITEM = "Rock_Gem_Ruby";
    private final String SUBSCRIBER_ITEM = "Deco_Starfish";

    private final Color OWNER_COLOR = ColorHelper.parseHexColor("#FFD700");
    private final Color MODERATOR_COLOR = ColorHelper.parseHexColor("#00FF00");
    private final Color MEMBER_COLOR = ColorHelper.parseHexColor("#0096FF");
    private final Color SUPERCHAT_COLOR = ColorHelper.parseHexColor("#FFD700");

    public YouTube(StreamData streamData, PlayerRef playerRef) {
        super(streamData, playerRef, "[YOUTUBE] ", ColorHelper.parseHexColor("#FF0000"));
    }

    @Override
    public void connect() {
        if (status == Status.CONNECTED) {
            sendMessage(SLMessage.formatMessageWithError("Already connected!"));
            return;
        }

        this.status = Status.CONNECTING;
        this.chat = new YouTubeChat();

        Thread thread = new Thread(() -> {
            try {
                String liveChatId = YouTubeAuth.getActiveLiveChatId(
                        streamData.getYouTubeChannelId(),
                        streamData.getYouTubeApiKey()
                );

                this.chat.connect(liveChatId, streamData.getYouTubeApiKey(), streamData.getYouTubeChannelId());
                sendMessage(SLMessage.formatMessage("Connected to YouTube live chat"));
                this.status = Status.CONNECTED;
                streamData.setIsYouTubeRunning(true);

                while (this.status == Status.CONNECTED && this.chat.isConnected()) {
                    try {
                        List<YouTubeChatMessage> messages = this.chat.pollMessages();

                        for (YouTubeChatMessage msg : messages) {
                            sendChatMessage(msg);
                        }

                        Thread.sleep(this.chat.getPollingIntervalMillis());
                    } catch (Exception e) {
                        e.printStackTrace();
                        Thread.sleep(5000);
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sendMessage(SLMessage.formatMessageWithError("Connection interrupted."));
                sendMessage(SLMessage.formatMessageWithLink("Check this troubleshooting guide: ", "https://kentatetsu.gitbook.io/streamlink/guides/youtube-setup-guide/troubleshooting"));
                StreamThread.disconnectYouTube(username);
            } catch (Exception e) {
                e.printStackTrace();
                sendMessage(SLMessage.formatMessageWithError("Connection failed: " + e.getMessage()));
                sendMessage(SLMessage.formatMessageWithLink("Check this troubleshooting guide: ", "https://kentatetsu.gitbook.io/streamlink/guides/youtube-setup-guide/troubleshooting"));
                StreamThread.disconnectYouTube(username);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void disconnect() {
        streamData.setIsYouTubeRunning(false);

        if (this.chat != null) {
            this.chat.disconnect();
            if (this.status == Status.CONNECTED)
                sendMessage(SLMessage.formatMessage("Disconnected from YouTube live chat"));
            this.status = Status.DISCONNECTED;
        }
    }

    @Override
    public String getBadgePrefix(List<String> badges) {
        if (badges.isEmpty()) { return ""; }

        if (badges.contains("owner")) {
            return "[OWNER] ";
        } else if (badges.contains("moderator")) {
            return "[MOD] ";
        } else if (badges.contains("member")) {
            return "[MEMBER] ";
        }
        return "";
    }

    @Override
    public Color getBadgeColor(List<String> badges) {
        if (badges.isEmpty()) { return Color.WHITE; }

        if (badges.contains("owner")) {
            return OWNER_COLOR;
        } else if (badges.contains("moderator")) {
            return MODERATOR_COLOR;
        } else if (badges.contains("member")) {
            return MEMBER_COLOR;
        }

        return Color.WHITE;
    }
}
