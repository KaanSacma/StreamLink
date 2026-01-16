package com.kenta.services.twitch;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class TwitchEventSub {
    private static final String EVENTBUS_WEBSOCKET_URL = "wss://eventsub.wss.twitch.tv/ws";

    private WebSocketClient client;
    private String sessionId;
    private String accessToken;
    private String clientId;
    private String broadcasterId;
    private final Gson gson = new Gson();

    private final Map<String, Consumer<JsonObject>> eventHandlers = new HashMap<>();
    private boolean connected = false;

    public void connect(String accessToken, String clientId, String broadcasterId) {
        this.accessToken = accessToken;
        this.clientId = clientId;
        this.broadcasterId = broadcasterId;

        try {
            System.out.println("[EventSub] Connecting to Twitch EventSub...");

            client = new WebSocketClient(new URI(EVENTBUS_WEBSOCKET_URL)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("[EventSub] WebSocket connected");
                }

                @Override
                public void onMessage(String message) {
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("[EventSub] Disconnected: " + reason);
                    connected = false;
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("[EventSub] Error: " + ex.getMessage());
                    ex.printStackTrace();
                }
            };

            client.connect();

        } catch (Exception e) {
            System.err.println("[EventSub] Failed to connect: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleMessage(String message) {
        try {
            JsonObject json = gson.fromJson(message, JsonObject.class);
            JsonObject metadata = json.getAsJsonObject("metadata");
            String messageType = metadata.get("message_type").getAsString();

            System.out.println("[EventSub] Received: " + messageType);

            switch (messageType) {
                case "session_welcome":
                    handleWelcome(json);
                    break;

                case "notification":
                    handleNotification(json);
                    break;

                case "session_keepalive":
                    break;

                case "session_reconnect":
                    handleReconnect(json);
                    break;

                case "revocation":
                    System.out.println("[EventSub] Subscription revoked");
                    break;
            }

        } catch (Exception e) {
            System.err.println("[EventSub] Error handling message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleWelcome(JsonObject json) {
        try {
            JsonObject payload = json.getAsJsonObject("payload");
            JsonObject session = payload.getAsJsonObject("session");
            sessionId = session.get("id").getAsString();

            System.out.println("[EventSub] Session ID: " + sessionId);
            connected = true;

            subscribeToEvents();

        } catch (Exception e) {
            System.err.println("[EventSub] Error in welcome: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleNotification(JsonObject json) {
        try {
            JsonObject payload = json.getAsJsonObject("payload");
            JsonObject subscription = payload.getAsJsonObject("subscription");
            String subscriptionType = subscription.get("type").getAsString();

            JsonObject event = payload.getAsJsonObject("event");

            System.out.println("[EventSub] Event: " + subscriptionType);

            Consumer<JsonObject> handler = eventHandlers.get(subscriptionType);
            if (handler != null) {
                handler.accept(event);
            } else {
                System.out.println("[EventSub] No handler for: " + subscriptionType);
            }

        } catch (Exception e) {
            System.err.println("[EventSub] Error in notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleReconnect(JsonObject json) {
        System.out.println("[EventSub] Reconnect requested");
        // TODO: Implement reconnection logic
    }

    private void subscribeToEvents() {
        System.out.println("[EventSub] Subscribing to events...");

        subscribeToEvent("channel.follow", "2");
        subscribeToEvent("channel.subscribe", "1");
        subscribeToEvent("channel.subscription.gift", "1");
        subscribeToEvent("channel.subscription.message", "1");
        subscribeToEvent("channel.raid", "1");
        subscribeToEvent("channel.cheer", "1");
        subscribeToEvent("channel.channel_points_custom_reward_redemption.add", "1");
    }

    private void subscribeToEvent(String type, String version) {
        try {
            HttpClient httpClient = HttpClient.newHttpClient();

            JsonObject body = new JsonObject();
            body.addProperty("type", type);
            body.addProperty("version", version);

            JsonObject transport = new JsonObject();
            transport.addProperty("method", "websocket");
            transport.addProperty("session_id", sessionId);
            body.add("transport", transport);

            JsonObject condition = new JsonObject();

            if (type.equals("channel.follow")) {
                condition.addProperty("broadcaster_user_id", broadcasterId);
                condition.addProperty("moderator_user_id", broadcasterId);
            } else if (type.equals("channel.raid")) {
                condition.addProperty("to_broadcaster_user_id", broadcasterId);
            } else {
                condition.addProperty("broadcaster_user_id", broadcasterId);
            }

            body.add("condition", condition);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.twitch.tv/helix/eventsub/subscriptions"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Client-Id", clientId)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 202) {
                System.out.println("[EventSub] Subscribed to: " + type);
            } else {
                System.err.println("[EventSub] Failed to subscribe to " + type);
                System.err.println("[EventSub] Status: " + response.statusCode());
                System.err.println("[EventSub] Body: " + response.body());
            }

        } catch (Exception e) {
            System.err.println("[EventSub] Error subscribing to " + type + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Register event handlers
    public void onFollow(Consumer<JsonObject> handler) {
        eventHandlers.put("channel.follow", handler);
    }

    public void onSubscribe(Consumer<JsonObject> handler) {
        eventHandlers.put("channel.subscribe", handler);
    }

    public void onGiftSub(Consumer<JsonObject> handler) {
        eventHandlers.put("channel.subscription.gift", handler);
    }

    public void onResub(Consumer<JsonObject> handler) {
        eventHandlers.put("channel.subscription.message", handler);
    }

    public void onRaid(Consumer<JsonObject> handler) {
        eventHandlers.put("channel.raid", handler);
    }

    public void onCheer(Consumer<JsonObject> handler) {
        eventHandlers.put("channel.cheer", handler);
    }

    public void onChannelPointRedemption(Consumer<JsonObject> handler) {
        eventHandlers.put("channel.channel_points_custom_reward_redemption.add", handler);
    }

    public boolean isConnected() {
        return connected && client != null && client.isOpen();
    }

    public void disconnect() {
        connected = false;
        if (client != null) {
            client.close();
        }
    }
}
