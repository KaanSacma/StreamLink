package com.kenta.services.youtube;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kenta.services.youtube.data.YouTubeChatMessage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class YouTubeChat {
    private static final String YOUTUBE_API_BASE = "https://www.googleapis.com/youtube/v3";
    private static final Gson gson = new Gson();

    private String liveChatId;
    private String apiKey;
    private String nextPageToken;
    private long pollingIntervalMillis = 5000;
    private boolean connected = false;
    private boolean firstPoll = true;
    public String channelId;

    public void connect(String liveChatId, String apiKey, String channelId) {
        this.liveChatId = liveChatId;
        this.apiKey = apiKey;
        this.channelId = channelId;
        this.connected = true;
        this.nextPageToken = null;
        this.firstPoll = true;

        System.out.println("[YouTube] Connected to live chat: " + liveChatId);
    }

    public List<YouTubeChatMessage> pollMessages() throws Exception {
        if (!connected) {
            return List.of();
        }

        HttpClient client = HttpClient.newHttpClient();

        String cleanApiKey = apiKey.trim().replaceAll("\\s+", "");
        String encodedApiKey = java.net.URLEncoder.encode(cleanApiKey, "UTF-8");

        String url = YOUTUBE_API_BASE + "/liveChat/messages?liveChatId=" + liveChatId + "&part=snippet,authorDetails&key=" + encodedApiKey;

        if (nextPageToken != null && !nextPageToken.isEmpty()) {
            url += "&pageToken=" + nextPageToken;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.println("[YouTube] Failed to fetch messages: " + response.body());
            return List.of();
        }

        JsonObject json = gson.fromJson(response.body(), JsonObject.class);

        if (json.has("pollingIntervalMillis")) {
            pollingIntervalMillis = json.get("pollingIntervalMillis").getAsLong();
        }

        if (json.has("nextPageToken")) {
            nextPageToken = json.get("nextPageToken").getAsString();
        }

        if (firstPoll) {
            firstPoll = false;
            return List.of();
        }

        List<YouTubeChatMessage> messages = new ArrayList<>();
        JsonArray items = json.getAsJsonArray("items");

        for (int i = 0; i < items.size(); i++) {
            JsonObject item = items.get(i).getAsJsonObject();
            JsonObject snippet = item.getAsJsonObject("snippet");
            JsonObject authorDetails = item.getAsJsonObject("authorDetails");

            String messageId = item.get("id").getAsString();
            String authorName = authorDetails.get("displayName").getAsString();
            String messageText = "";

            if (snippet.has("textMessageDetails")) {
                messageText = snippet.getAsJsonObject("textMessageDetails").get("messageText").getAsString();
            } else if (snippet.has("superChatDetails")) {
                messageText = snippet.getAsJsonObject("superChatDetails").get("userComment").getAsString();
            }

            List<String> badges = new ArrayList<>();
            if (authorDetails.get("isChatOwner").getAsBoolean()) {
                badges.add("owner");
            }
            if (authorDetails.get("isChatModerator").getAsBoolean()) {
                badges.add("moderator");
            }
            if (authorDetails.get("isChatSponsor").getAsBoolean()) {
                badges.add("member");
            }

            messages.add(new YouTubeChatMessage(
                    messageId,
                    authorName,
                    messageText,
                    badges
            ));
        }

        return messages;
    }

    public long getPollingIntervalMillis() {
        return pollingIntervalMillis;
    }

    public void disconnect() {
        connected = false;
    }

    public boolean isConnected() {
        return connected;
    }
}