package com.kenta.services;

import com.kenta.services.twitch.Twitch;
import com.kenta.services.youtube.YouTube;

import java.util.HashMap;
import java.util.Map;

public final class StreamThread {

    private static final Map<String, Twitch> twitch = new HashMap<>();
    private static final Map<String, YouTube> youtube = new HashMap<>();

    // Twitch methods
    public static Map<String, Twitch> getTwitch() { return twitch; }

    public static boolean isUserHasTwitchThread(String username) {
        return twitch.containsKey(username);
    }

    public static void putToTwitch(String username, Twitch userTwitch) {
        twitch.put(username, userTwitch);
    }

    public static void disconnectTwitch(String username) {
        Twitch userTwitch = twitch.remove(username);
        if (userTwitch != null)
            userTwitch.disconnect();
    }

    public static void disconnectAllTwitch() {
        for (Map.Entry<String, Twitch> entry : twitch.entrySet()) {
            Twitch userTwitch = entry.getValue();
            if (userTwitch != null)
                userTwitch.disconnect();
        }
        twitch.clear();
    }

    // YouTube methods
    public static Map<String, YouTube> getYouTube() { return youtube; }

    public static boolean isUserHasYouTubeThread(String username) {
        return youtube.containsKey(username);
    }

    public static void putToYouTube(String username, YouTube userYouTube) {
        youtube.put(username, userYouTube);
    }

    public static void disconnectYouTube(String username) {
        YouTube userYouTube = youtube.remove(username);
        if (userYouTube != null)
            userYouTube.disconnect();
    }

    public static void disconnectAllYouTube() {
        for (Map.Entry<String, YouTube> entry : youtube.entrySet()) {
            YouTube userYouTube = entry.getValue();
            if (userYouTube != null)
                userYouTube.disconnect();
        }
        youtube.clear();
    }

    public static void disconnectAll() {
        disconnectAllTwitch();
        disconnectAllYouTube();
    }
}