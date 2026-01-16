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
            .build();

    private String twitchChannel;
    private Boolean isTwitchRunning;

    public StreamData() { this("", false); }
    public StreamData(String channel, Boolean isTwitchRunning) {
        this.twitchChannel = channel;
        this.isTwitchRunning = isTwitchRunning;
    }

    public String getTwitchChannel() {
        return this.twitchChannel;
    }
    public void setTwitchChannel(String channel) { this.twitchChannel = channel; }

    public Boolean getIsTwitchRunning() { return this.isTwitchRunning; }
    public void setIsTwitchRunning(Boolean isTwitchRunning) { this.isTwitchRunning = isTwitchRunning; }

    @NullableDecl
    @Override
    public Component<EntityStore> clone() {
        return new StreamData(this.twitchChannel, this.isTwitchRunning);
    }
}
