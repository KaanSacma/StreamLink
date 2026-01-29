package com.kenta.actions.context;

import com.google.gson.JsonObject;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.kenta.services.AbstractChatMessage;

import java.util.HashMap;
import java.util.Map;

public class ActionContext {
    private final PlayerRef playerRef;
    private final String platform; // "twitch", "youtube", "kick"
    private final String eventType;
    private final JsonObject eventData;
    private final AbstractChatMessage chatMessage;
    private final Map<String, Object> customData;

    private ActionContext(Builder builder) {
        this.playerRef = builder.playerRef;
        this.platform = builder.platform;
        this.eventType = builder.eventType;
        this.eventData = builder.eventData;
        this.chatMessage = builder.chatMessage;
        this.customData = builder.customData;
    }

    public PlayerRef getPlayerRef() { return playerRef; }
    public String getPlatform() { return platform; }
    public String getEventType() { return eventType; }
    public JsonObject getEventData() { return eventData; }
    public AbstractChatMessage getChatMessage() { return chatMessage; }
    public Map<String, Object> getCustomData() { return customData; }

    public <T> T getCustomData(String key, Class<T> type) {
        Object value = customData.get(key);
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    public String getEventDataString(String key) {
        if (eventData != null && eventData.has(key)) {
            return eventData.get(key).getAsString();
        }
        return "";
    }

    public int getEventDataInt(String key) {
        if (eventData != null && eventData.has(key)) {
            return eventData.get(key).getAsInt();
        }
        return 0;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private PlayerRef playerRef;
        private String platform;
        private String eventType;
        private JsonObject eventData;
        private AbstractChatMessage chatMessage;
        private Map<String, Object> customData = new HashMap<>();

        public Builder playerRef(PlayerRef playerRef) {
            this.playerRef = playerRef;
            return this;
        }

        public Builder platform(String platform) {
            this.platform = platform;
            return this;
        }

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder eventData(JsonObject eventData) {
            this.eventData = eventData;
            return this;
        }

        public Builder chatMessage(AbstractChatMessage chatMessage) {
            this.chatMessage = chatMessage;
            return this;
        }

        public Builder customData(String key, Object value) {
            this.customData.put(key, value);
            return this;
        }

        public ActionContext build() {
            return new ActionContext(this);
        }
    }
}
