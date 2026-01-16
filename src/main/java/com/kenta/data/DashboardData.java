package com.kenta.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class DashboardData {
    public String twitchChannelInput;
    public String eventType;

    public static final BuilderCodec<DashboardData> CODEC =
            BuilderCodec.builder(DashboardData.class, DashboardData::new)
                    .append(
                            new KeyedCodec<>("EventType", Codec.STRING),
                            (data, value) -> data.eventType = value,
                            data -> data.eventType
                    ).add()
                    .append(
                            new KeyedCodec<>("@TwitchChannelInput", Codec.STRING),
                            (data, value) -> data.twitchChannelInput = value,
                            data -> data.twitchChannelInput
                    ).add()
                    .build();
}
