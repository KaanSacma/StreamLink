package com.kenta.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class DashboardData {
    // Twitch fields
    public String twitchChannelInput;
    public String twitchAccessTokenInput;
    public String twitchClientIdInput;

    // YouTube fields
    public String youtubeChannelIdInput;
    public String youtubeApiKeyInput;

    // Common fields
    public String eventType;

    public static final BuilderCodec<DashboardData> CODEC =
            BuilderCodec.builder(DashboardData.class, DashboardData::new)
                    .append(
                            new KeyedCodec<>("EventType", Codec.STRING),
                            (data, value) -> data.eventType = value,
                            data -> data.eventType
                    ).add()
                    // Twitch fields
                    .append(
                            new KeyedCodec<>("@TwitchChannelInput", Codec.STRING),
                            (data, value) -> data.twitchChannelInput = value,
                            data -> data.twitchChannelInput
                    ).add()
                    .append(
                            new KeyedCodec<>("@TwitchAccessTokenInput", Codec.STRING),
                            (data, value) -> data.twitchAccessTokenInput = value,
                            data -> data.twitchAccessTokenInput
                    ).add()
                    .append(
                            new KeyedCodec<>("@TwitchClientIdInput", Codec.STRING),
                            (data, value) -> data.twitchClientIdInput = value,
                            data -> data.twitchClientIdInput
                    ).add()
                    // YouTube fields
                    .append(
                            new KeyedCodec<>("@YouTubeChannelIdInput", Codec.STRING),
                            (data, value) -> data.youtubeChannelIdInput = value,
                            data -> data.youtubeChannelIdInput
                    ).add()
                    .append(
                            new KeyedCodec<>("@YouTubeApiKeyInput", Codec.STRING),
                            (data, value) -> data.youtubeApiKeyInput = value,
                            data -> data.youtubeApiKeyInput
                    ).add()
                    .build();
}