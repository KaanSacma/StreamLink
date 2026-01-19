package com.kenta.services.twitch;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.kenta.data.StreamData;
import com.kenta.libs.ColorHelper;
import com.kenta.libs.SLMessage;
import com.kenta.services.AbstractService;
import com.kenta.services.Status;
import com.kenta.services.StreamThread;
import com.kenta.services.twitch.data.TwitchChatMessage;

import java.awt.*;
import java.io.IOException;
import java.net.SocketException;
import java.util.List;

public class Twitch extends AbstractService {
    private TwitchChat chat;
    private TwitchEventSub eventSub;

    private final String FOLLOW_ITEM = "Deco_Starfish";
    private final String SUBSCRIBE_ITEM = "Deco_Treasure";
    private final String GIFTSUB_ITEM = "Deco_Treasure_Pile_Small";
    private final String RESUB_ITEM = "Deco_Treasure_Pile_Large";
    private final String RAID_ITEM = "Deco_Coral_Shell_Swirly";
    private final String CHEER_ITEM = "Rock_Gem_Ruby";
    private final String POINT_ITEM = "Ore_Onyxium";

    private final Color FOLLOW_COLOR = ColorHelper.parseHexColor("#9146FF");
    private final Color SUBSCRIBE_COLOR = ColorHelper.parseHexColor("#FFD700");
    private final Color GIFTSUB_COLOR = ColorHelper.parseHexColor("#00D166");
    private final Color RESUB_COLOR = ColorHelper.parseHexColor("#FF6B00");
    private final Color RAID_COLOR = ColorHelper.parseHexColor("#E74C3C");
    private final Color CHEER_COLOR = ColorHelper.parseHexColor("#9C27B0");
    private final Color POINT_COLOR = ColorHelper.parseHexColor("#00D9CC");

    private final Color BROADCASTER_COLOR = ColorHelper.parseHexColor("#ffd700");
    private final Color MODERATOR_COLOR = ColorHelper.parseHexColor("#00ff00");
    private final Color VIP_COLOR = ColorHelper.parseHexColor("#ff00ff");
    private final Color FOUNDER_COLOR = ColorHelper.parseHexColor("#8a2be2");
    private final Color PREMIUM_COLOR = ColorHelper.parseHexColor("#87cefa");

    public Twitch(StreamData streamData, PlayerRef playerRef) {
        super(streamData, playerRef, "[TWITCH] ", ColorHelper.parseHexColor("#6441a5"));
    }

    @Override
    public void connect() {
        if (status == Status.CONNECTED) {
            sendMessage(SLMessage.formatMessageWithError("Already connected!"));
            return;
        }

        this.status = Status.CONNECTING;
        this.chat = new TwitchChat();

        Thread thread = new Thread(() -> {
            try {
                this.chat.connectAnonymous(this.streamData.getTwitchChannel());
                sendMessage(SLMessage.formatMessage("Connected to #" + this.streamData.getTwitchChannel()));
                this.status = Status.CONNECTED;
                this.streamData.setIsTwitchRunning(true);

                while (this.status == Status.CONNECTED && this.chat.isConnected()) {
                    TwitchChatMessage chatMessage = this.chat.readMessage();
                    if (chatMessage != null) { sendChatMessage(chatMessage); }
                    Thread.sleep(10);
                }
            } catch (SocketException e) {
                if (this.status == Status.CONNECTED) {
                    sendMessage(SLMessage.formatMessageWithError("Connection lost."));
                    sendMessage(SLMessage.formatMessageWithLink("Check this troubleshooting guide: ", "https://kentatetsu.gitbook.io/streamlink/guides/twitch-setup-guide/troubleshooting"));
                }
                StreamThread.disconnectTwitch(this.username);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sendMessage(SLMessage.formatMessageWithError("Connection interrupted."));
                sendMessage(SLMessage.formatMessageWithLink("Check this troubleshooting guide: ", "https://kentatetsu.gitbook.io/streamlink/guides/twitch-setup-guide/troubleshooting"));
                StreamThread.disconnectTwitch(this.username);
            } catch (Exception e) {
                e.printStackTrace();
                sendMessage(SLMessage.formatMessageWithError("Connection failed!"));
                sendMessage(SLMessage.formatMessageWithLink("Check this troubleshooting guide: ", "https://kentatetsu.gitbook.io/streamlink/guides/twitch-setup-guide/troubleshooting"));
                StreamThread.disconnectTwitch(this.username);
            }
        });

        thread.setDaemon(true);
        thread.start();

        if (this.streamData.getTwitchAccessToken() != null && !this.streamData.getTwitchAccessToken().isEmpty()) {
            eventSub = new TwitchEventSub();
            eventSub.connect(
                    this.streamData.getTwitchAccessToken(),
                    this.streamData.getTwitchClientId(),
                    this.streamData.getBroadcasterId()
            );
            setupEventHandlers();
            sendMessage(SLMessage.formatMessage("Events connected! You'll see follows, subs, raids, etc."));
        }
    }

    private void setupEventHandlers() {
        eventSub.onFollow(event -> {
            String followerName = event.get("user_name").getAsString();
            String message = followerName + " just followed!";

            sendNotification(FOLLOW_ITEM, "Follower", message, FOLLOW_COLOR);
        });

        eventSub.onSubscribe(event -> {
            String subName = event.get("user_name").getAsString();
            String tier = event.get("tier").getAsString();
            String tierDisplay = tier.equals("1000") ? "1" : tier.equals("2000") ? "2" : "3";
            String message = subName + " just subscribed! (Tier " + tierDisplay + ")";

            sendNotification(SUBSCRIBE_ITEM, "Subscriber", message, SUBSCRIBE_COLOR);
        });

        eventSub.onGiftSub(event -> {
            String gifterName = event.has("user_name") && !event.get("user_name").isJsonNull()
                    ? event.get("user_name").getAsString()
                    : "Anonymous";
            int total = event.get("total").getAsInt();
            String tier = event.get("tier").getAsString();
            String tierDisplay = tier.equals("1000") ? "1" : tier.equals("2000") ? "2" : "3";
            String message = gifterName + " gifted " + total + " Tier " + tierDisplay + " subs!";

            sendNotification(GIFTSUB_ITEM, "Gift Sub", message, GIFTSUB_COLOR);
        });

        eventSub.onResub(event -> {
            String subName = event.get("user_name").getAsString();
            String tier = event.get("tier").getAsString();
            String tierDisplay = tier.equals("1000") ? "1" : tier.equals("2000") ? "2" : "3";
            int months = event.has("cumulative_months") ? event.get("cumulative_months").getAsInt() : 0;
            String eventMsg = event.has("message") && event.getAsJsonObject("message").has("text")
                    ? event.getAsJsonObject("message").get("text").getAsString()
                    : "";
            String message = subName + " resubscribed for " + months + " months! (Tier " + tierDisplay + ")" + (eventMsg.isEmpty() ? "" : " - " + eventMsg);

            sendNotification(RESUB_ITEM, "Resubscribe", message, RESUB_COLOR);
        });

        eventSub.onRaid(event -> {
            String raiderName = event.get("from_broadcaster_user_name").getAsString();
            int viewers = event.get("viewers").getAsInt();
            String message = raiderName + " is raiding with " + viewers + " viewers!";

            sendNotification(RAID_ITEM, "Raid", message, RAID_COLOR);
        });

        eventSub.onCheer(event -> {
            String cheerName = event.has("user_name") && !event.get("user_name").isJsonNull()
                    ? event.get("user_name").getAsString()
                    : "Anonymous";
            int bits = event.get("bits").getAsInt();
            String eventMsg = event.has("message") ? event.get("message").getAsString() : "";
            String message = cheerName + " cheered " + bits + " bits!" + (eventMsg.isEmpty() ? "" : " - " + eventMsg);

            sendNotification(CHEER_ITEM, "Cheer", message, CHEER_COLOR);
        });

        eventSub.onChannelPointRedemption(event -> {
            String userName = event.get("user_name").getAsString();
            String rewardTitle = event.getAsJsonObject("reward").get("title").getAsString();
            String userInput = event.has("user_input") ? event.get("user_input").getAsString() : "";
            String message = userName + " redeemed: " + rewardTitle + (userInput.isEmpty() ? "" : " - " + userInput);

            sendNotification(POINT_ITEM, "Channel Point Redeem", message, POINT_COLOR);
        });
    }

    @Override
    public void disconnect() {
        this.streamData.setIsYouTubeRunning(false);

        if (this.chat != null) {
            try {
                if (this.status == Status.CONNECTED)
                    sendMessage(SLMessage.formatMessage("Disconnected from #" + this.chat.channel));
                this.status = Status.DISCONNECTED;
                this.chat.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
                if (this.status == Status.CONNECTED)
                    sendMessage(SLMessage.formatMessageWithError("Error while disconnecting"));
                this.status = Status.DISCONNECTED;
            }
        }

        if (eventSub != null) {
            eventSub.disconnect();
        }
    }

    @Override
    public String getBadgePrefix(List<String> badges) {
        if (badges.isEmpty()) { return ""; }

        if (badges.contains("broadcaster")) {
            return "[BROADCASTER] ";
        } else if (badges.contains("moderator")) {
            return "[MOD] ";
        } else if (badges.contains("vip")) {
            return "[VIP] ";
        } else if (badges.contains("subscriber") || badges.contains("founder")) {
            return "[SUB] ";
        } else if (badges.contains("premium")) {
            return "[PRIME] ";
        }
        return "";
    }

    @Override
    public Color getBadgeColor(List<String> badges) {
        if (badges.isEmpty()) { return Color.WHITE; }

        if (badges.contains("broadcaster")) {
            return BROADCASTER_COLOR;
        } else if (badges.contains("moderator")) {
            return MODERATOR_COLOR;
        } else if (badges.contains("vip")) {
            return VIP_COLOR;
        } else if (badges.contains("subscriber") || badges.contains("founder")) {
            return FOUNDER_COLOR;
        } else if (badges.contains("premium")) {
            return PREMIUM_COLOR;
        }

        return Color.WHITE;
    }
}
