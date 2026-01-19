package com.kenta.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class StreamData implements Component<EntityStore> {

    public static final BuilderCodec<StreamData> CODEC = BuilderCodec.builder(StreamData.class, StreamData::new)
            // Twitch fields
            .append(
                    new KeyedCodec<>("TwitchChannel", Codec.STRING),
                    (state, count) -> state.twitchChannel = count,
                    (state) -> state.twitchChannel
            ).add()
            .append(
                    new KeyedCodec<>("TwitchClientId", Codec.STRING),
                    (state, count) -> state.twitchClientId = count,
                    (state) -> state.twitchClientId
            ).add()
            .append(
                    new KeyedCodec<>("TwitchAccessToken", Codec.STRING),
                    (state, count) -> state.twitchAccessToken = count,
                    (state) -> state.twitchAccessToken
            ).add()
            .append(
                    new KeyedCodec<>("IsTwitchRunning", Codec.BOOLEAN),
                    (state, count) -> state.isTwitchRunning = count,
                    (state) -> state.isTwitchRunning
            ).add()
            .append(
                    new KeyedCodec<>("BroadcasterId", Codec.STRING),
                    (state, count) -> state.broadcasterId = count,
                    (state) -> state.broadcasterId
            ).add()
            // YouTube fields
            .append(
                    new KeyedCodec<>("YouTubeChannelId", Codec.STRING),
                    (state, count) -> state.youtubeChannelId = count,
                    (state) -> state.youtubeChannelId
            ).add()
            .append(
                    new KeyedCodec<>("YouTubeApiKey", Codec.STRING),
                    (state, count) -> state.youtubeApiKey = count,
                    (state) -> state.youtubeApiKey
            ).add()
            .append(
                    new KeyedCodec<>("IsYouTubeRunning", Codec.BOOLEAN),
                    (state, count) -> state.isYouTubeRunning = count,
                    (state) -> state.isYouTubeRunning
            ).add()
            .build();

    // Twitch fields
    private String twitchChannel;
    private Boolean isTwitchRunning;
    private String twitchClientId;
    private String twitchAccessToken;
    private String broadcasterId;

    // YouTube fields
    private String youtubeChannelId;
    private String youtubeApiKey;
    private Boolean isYouTubeRunning;

    public StreamData() {
        this("", false, "", "", "", "", "", false);
    }

    public StreamData(String twitchChannel, Boolean isTwitchRunning, String twitchClientId,
                      String twitchAccessToken, String broadcasterId,
                      String youtubeChannelId, String youtubeApiKey, Boolean isYouTubeRunning) {
        this.twitchChannel = twitchChannel;
        this.isTwitchRunning = isTwitchRunning;
        this.twitchClientId = twitchClientId;
        this.twitchAccessToken = twitchAccessToken;
        this.broadcasterId = broadcasterId;
        this.youtubeChannelId = youtubeChannelId;
        this.youtubeApiKey = youtubeApiKey;
        this.isYouTubeRunning = isYouTubeRunning;
    }

    // Twitch getters/setters
    public String getTwitchChannel() { return this.twitchChannel; }
    public void setTwitchChannel(String channel) { this.twitchChannel = channel; }

    public Boolean getIsTwitchRunning() { return this.isTwitchRunning; }
    public void setIsTwitchRunning(Boolean isTwitchRunning) { this.isTwitchRunning = isTwitchRunning; }

    public String getTwitchClientId() { return this.twitchClientId; }
    public void setTwitchClientId(String clientId) { this.twitchClientId = clientId; }

    public String getTwitchAccessToken() { return this.twitchAccessToken; }
    public void setTwitchAccessToken(String accessToken) { this.twitchAccessToken = accessToken; }

    public String getBroadcasterId() { return this.broadcasterId; }
    public void setBroadcasterId(String broadcasterId) { this.broadcasterId = broadcasterId; }

    // YouTube getters/setters
    public String getYouTubeChannelId() { return this.youtubeChannelId; }
    public void setYouTubeChannelId(String channelId) { this.youtubeChannelId = channelId; }

    public String getYouTubeApiKey() { return this.youtubeApiKey; }
    public void setYouTubeApiKey(String apiKey) { this.youtubeApiKey = apiKey; }

    public Boolean getIsYouTubeRunning() { return this.isYouTubeRunning; }
    public void setIsYouTubeRunning(Boolean isYouTubeRunning) { this.isYouTubeRunning = isYouTubeRunning; }

    @NullableDecl
    @Override
    public Component<EntityStore> clone() {
        return new StreamData(
                this.twitchChannel, this.isTwitchRunning, this.twitchClientId,
                this.twitchAccessToken, this.broadcasterId,
                this.youtubeChannelId, this.youtubeApiKey, this.isYouTubeRunning
        );
    }
}