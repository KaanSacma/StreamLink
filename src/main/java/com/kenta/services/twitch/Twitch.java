package com.kenta.services.twitch;

import com.hypixel.hytale.protocol.ItemWithAllMetadata;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.kenta.data.StreamData;
import com.kenta.services.twitch.data.ChatMessage;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;

import java.awt.*;
import java.io.IOException;
import java.net.SocketException;

public class Twitch {
    private TwitchChat chatClient;
    private Thread chatThread;
    private boolean running = false;

    public void connectToChannel(StreamData streamData, Player player) {
        if (running) {
            sendMessageToPlayer(player, "[StreamLink] Already connected!");
            return;
        }

        chatClient = new TwitchChat();
        running = true;

        chatThread = new Thread(() -> {
            try {
                chatClient.connectAnonymous(streamData.getTwitchChannel(), player);

                sendMessageToPlayer(player, "[StreamLink] Connected to #" + streamData.getTwitchChannel());
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
                    sendMessageToPlayer(player, "[StreamLink] Connection lost.");
                }
                streamData.setIsTwitchRunning(false);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sendMessageToPlayer(player, "[StreamLink] Connection interrupted.");
                streamData.setIsTwitchRunning(false);
            } catch (Exception e) {
                e.printStackTrace();
                sendMessageToPlayer(player, "[StreamLink] Connection failed!");
                streamData.setIsTwitchRunning(false);
            }
        });

        chatThread.setDaemon(true);
        chatThread.start();
    }

    public void disconnect() {
        running = false;
        if (chatClient != null) {
            try {
                chatClient.disconnect();
                sendMessageToPlayer(chatClient.player, "[StreamLink] Disconnected from #" + chatClient.channel);
            } catch (IOException e) {
                e.printStackTrace();
                sendMessageToPlayer(chatClient.player, "[StreamLink] Error while disconnecting");
            }
        }
    }

    private void sendMessageToPlayer(Player player, String message) {
        player.sendMessage(Message.translation("[To " + player.getDisplayName() + "] " + message));
    }

    private void broadcastToServer(Player player, ChatMessage chat) {
        player.sendMessage(
                Message.join(
                        Message.translation("[StreamLink] ").color(Color.LIGHT_GRAY),
                        Message.translation("[Twitch] ").color(Color.MAGENTA),
                        Message.translation(chat.username()).color(chat.color()),
                        Message.translation(" : "),
                        Message.translation(chat.message())
                )
        );
    }

    private void sendNotification(Player player, String username, String message) {
        var playerRef = Universe.get().getPlayer(player.getUuid());
        var packetHandler = playerRef.getPacketHandler();
        var primaryMessage = Message.raw(username).color("#00FF00");
        var secondaryMessage = Message.raw(message).color("#228B22");
        var icon = new ItemStack("Weapon_Sword_Mithril", 1).toPacket();

        NotificationUtil.sendNotification(
                packetHandler,
                primaryMessage,
                secondaryMessage,
                (ItemWithAllMetadata) icon
        );
    }
}
