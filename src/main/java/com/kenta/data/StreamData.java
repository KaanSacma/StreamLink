package com.kenta.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class StreamData implements Component<EntityStore> {

    public static final BuilderCodec<StreamData> CODEC = BuilderCodec.builder(StreamData.class, StreamData::new)
            .append(
                    new KeyedCodec<>("TwitchChannel", Codec.STRING),
                    (state, count) -> state.twitchChannel = count,
                    (state) -> state.twitchChannel
            ).add()
            .append(
                    new KeyedCodec<>("IsTwitchRunning", Codec.BOOLEAN),
                    (state, count) -> state.isTwitchRunning = count,
                    (state) -> state.isTwitchRunning
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
                    new KeyedCodec<>("BroadcasterId", Codec.STRING),
                    (state, count) -> state.broadcasterId = count,
                    (state) -> state.broadcasterId
            ).add()
            .build();

    private String twitchChannel;
    private Boolean isTwitchRunning;
    private String twitchClientId;
    private String twitchAccessToken;
    private String broadcasterId;

    public StreamData() { this("", false, "", "", ""); }
    public StreamData(String channel, Boolean isTwitchRunning, String clientId, String accessToken, String broadcasterId) {
        this.twitchChannel = channel;
        this.isTwitchRunning = isTwitchRunning;
        this.twitchClientId = clientId;

        this.twitchAccessToken = accessToken;
        this.broadcasterId = broadcasterId;
    }

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

    @NullableDecl
    @Override
    public Component<EntityStore> clone() {
        return new StreamData(this.twitchChannel, this.isTwitchRunning, this.twitchClientId, this.twitchAccessToken, this.broadcasterId);
    }
}
