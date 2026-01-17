package com.kenta.services.twitch;

import com.hypixel.hytale.protocol.ItemWithAllMetadata;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.kenta.data.StreamData;
import com.kenta.libs.ColorHelper;
import com.kenta.libs.SLMessage;
import com.kenta.services.twitch.data.ChatMessage;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;

import java.awt.*;
import java.io.IOException;
import java.net.SocketException;
import java.util.List;

public class Twitch {
    private TwitchChat chatClient;
    private TwitchEventSub eventSub;
    private Thread chatThread;
    private boolean running = false;

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

    @Deprecated
    public void connectToChannel(StreamData streamData, Player player) {
        if (running) {
            sendMessageToPlayer(player, SLMessage.formatMessageWithError("Already connected!"));
            return;
        }

        chatClient = new TwitchChat();
        running = true;

        chatThread = new Thread(() -> {
            try {
                chatClient.connectAnonymous(streamData.getTwitchChannel(), player);
                sendMessageToPlayer(player, SLMessage.formatMessage("Connected to #" + streamData.getTwitchChannel()));
                streamData.setIsTwitchRunning(true);

                while (running && chatClient.isConnected()) {
                    ChatMessage chat = chatClient.readMessage();
                    if (chat != null) {
                        broadcastToServer(player, chat);
                    }
                    Thread.sleep(10);
                }
            } catch (SocketException e) {
                if (running) {
                    sendMessageToPlayer(player, SLMessage.formatMessageWithError("Connection lost."));
                }
                streamData.setIsTwitchRunning(false);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sendMessageToPlayer(player, SLMessage.formatMessageWithError("Connection interrupted."));
                streamData.setIsTwitchRunning(false);
            } catch (Exception e) {
                e.printStackTrace();
                sendMessageToPlayer(player, SLMessage.formatMessageWithError("Connection failed!"));
                streamData.setIsTwitchRunning(false);
            }
        });

        chatThread.setDaemon(true);
        chatThread.start();
    }

    public void connectWithEvents(StreamData streamData, Player player) {
        if (running) {
            sendMessageToPlayer(player, SLMessage.formatMessageWithError("Already connected!"));
            return;
        }

        running = true;

        chatClient = new TwitchChat();
        chatThread = new Thread(() -> {
            try {
                chatClient.connectAnonymous(streamData.getTwitchChannel(), player);
                sendMessageToPlayer(player, SLMessage.formatMessage("Connected to #" + streamData.getTwitchChannel()));
                streamData.setIsTwitchRunning(true);

                while (running && chatClient.isConnected()) {
                    ChatMessage chat = chatClient.readMessage();
                    if (chat != null) {
                        broadcastToServer(player, chat);
                    }
                    Thread.sleep(10);
                }
            } catch (SocketException e) {
                if (running) {
                    sendMessageToPlayer(player, SLMessage.formatMessageWithError("Connection lost."));
                }
                streamData.setIsTwitchRunning(false);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sendMessageToPlayer(player, SLMessage.formatMessageWithError("Connection interrupted."));
                streamData.setIsTwitchRunning(false);
            } catch (Exception e) {
                e.printStackTrace();
                sendMessageToPlayer(player, SLMessage.formatMessageWithError("Connection failed!"));
                streamData.setIsTwitchRunning(false);
            }
        });
        chatThread.setDaemon(true);
        chatThread.start();

        if (streamData.getTwitchAccessToken() != null && !streamData.getTwitchAccessToken().isEmpty()) {
            eventSub = new TwitchEventSub();
            eventSub.connect(
                    streamData.getTwitchAccessToken(),
                    streamData.getTwitchClientId(),
                    streamData.getBroadcasterId()
            );

            setupEventHandlers(player);
            sendMessageToPlayer(player, SLMessage.formatMessage("Events connected! You'll see follows, subs, raids, etc."));
        }
    }

    private void setupEventHandlers(Player player) {
        eventSub.onFollow(event -> {
            String followerName = event.get("user_name").getAsString();
            String message = followerName + " just followed!";

            sendNotification(player, FOLLOW_ITEM, "Follower", message, FOLLOW_COLOR);
        });

        eventSub.onSubscribe(event -> {
            String subName = event.get("user_name").getAsString();
            String tier = event.get("tier").getAsString();
            String tierDisplay = tier.equals("1000") ? "1" : tier.equals("2000") ? "2" : "3";
            String message = subName + " just subscribed! (Tier " + tierDisplay + ")";

            sendNotification(player, SUBSCRIBE_ITEM, "Subscriber", message, SUBSCRIBE_COLOR);
        });

        eventSub.onGiftSub(event -> {
            String gifterName = event.has("user_name") && !event.get("user_name").isJsonNull()
                    ? event.get("user_name").getAsString()
                    : "Anonymous";
            int total = event.get("total").getAsInt();
            String tier = event.get("tier").getAsString();
            String tierDisplay = tier.equals("1000") ? "1" : tier.equals("2000") ? "2" : "3";
            String message = gifterName + " gifted " + total + " Tier " + tierDisplay + " subs!";

            sendNotification(player, GIFTSUB_ITEM, "Gift Sub", message, GIFTSUB_COLOR);
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

            sendNotification(player, RESUB_ITEM, "Resubscribe", message, RESUB_COLOR);
        });

        eventSub.onRaid(event -> {
            String raiderName = event.get("from_broadcaster_user_name").getAsString();
            int viewers = event.get("viewers").getAsInt();
            String message = raiderName + " is raiding with " + viewers + " viewers!";

            sendNotification(player, RAID_ITEM, "Raid", message, RAID_COLOR);
        });

        eventSub.onCheer(event -> {
            String cheerName = event.has("user_name") && !event.get("user_name").isJsonNull()
                    ? event.get("user_name").getAsString()
                    : "Anonymous";
            int bits = event.get("bits").getAsInt();
            String eventMsg = event.has("message") ? event.get("message").getAsString() : "";
            String message = cheerName + " cheered " + bits + " bits!" + (eventMsg.isEmpty() ? "" : " - " + eventMsg);

            sendNotification(player, CHEER_ITEM, "Cheer", message, CHEER_COLOR);
        });

        eventSub.onChannelPointRedemption(event -> {
            String userName = event.get("user_name").getAsString();
            String rewardTitle = event.getAsJsonObject("reward").get("title").getAsString();
            String userInput = event.has("user_input") ? event.get("user_input").getAsString() : "";
            String message = userName + " redeemed: " + rewardTitle + (userInput.isEmpty() ? "" : " - " + userInput);

            sendNotification(player, POINT_ITEM, "Redeem", message, POINT_COLOR);
        });
    }

    public void disconnect() {
        running = false;

        if (chatClient != null) {
            try {
                chatClient.disconnect();
                sendMessageToPlayer(chatClient.player, SLMessage.formatMessage("Disconnected from #" + chatClient.channel));
            } catch (IOException e) {
                e.printStackTrace();
                sendMessageToPlayer(chatClient.player, SLMessage.formatMessageWithError("Error while disconnecting"));
            }
        }

        if (eventSub != null) {
            eventSub.disconnect();
        }
    }

    private void sendMessageToPlayer(Player player, Message message) {
        player.sendMessage(message);
    }

    private void broadcastToServer(Player player, ChatMessage chat) {
        Color userColor = ColorHelper.parseHexColor(chat.color());
        Color twitchColor = ColorHelper.parseHexColor("#6441a5");
        String badgePrefix = getBadgePrefix(chat.badges());

        player.sendMessage(
                Message.join(
                        Message.translation("[Twitch] ").color(twitchColor),
                        Message.translation(badgePrefix).color(getBadgeColor(chat.badges())),
                        Message.translation(chat.username()).color(userColor),
                        Message.translation(" : ").color(Color.LIGHT_GRAY),
                        Message.translation(chat.message())
                )
        );
    }

    private String getBadgePrefix(List<String> badges) {
        if (badges.isEmpty()) {return "";}

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

    private Color getBadgeColor(List<String> badges) {
        if (badges.isEmpty()) {
            return Color.WHITE;
        }

        if (badges.contains("broadcaster")) {
            return new Color(255, 215, 0);
        } else if (badges.contains("moderator")) {
            return new Color(0, 255, 0);
        } else if (badges.contains("vip")) {
            return new Color(255, 0, 255);
        } else if (badges.contains("subscriber") || badges.contains("founder")) {
            return new Color(138, 43, 226);
        } else if (badges.contains("premium")) {
            return new Color(135, 206, 250);
        }

        return Color.WHITE;
    }

    private void sendNotification(Player player, String item, String title, String message, Color color) {
        var playerRef = Universe.get().getPlayer(player.getUuid());
        assert playerRef != null;
        var packetHandler = playerRef.getPacketHandler();
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
}
