package com.kenta.services;

import com.kenta.services.twitch.Twitch;

import java.util.HashMap;
import java.util.Map;

public class StreamThread {
    private static final Map<String, Twitch> twitch = new HashMap<>();

    public static Map<String, Twitch> getTwitch() { return twitch; }

    public static boolean isUserHasTwitchThread(String username) {
        return twitch.containsKey(username);
    }

    public static void putToTwitch(String username, Twitch userTwitch) {
        twitch.put(username, userTwitch);
    }

    public static void disconnectTwitch(String username) {
        twitch.get(username).disconnect();
        twitch.remove(username);
    }

    public static void disconnectAllTwitch() {
        for (Map.Entry<String, Twitch> entry : twitch.entrySet()) {
            Twitch userTwitch = entry.getValue();
            userTwitch.disconnect();
        }
        twitch.clear();
    }
}
