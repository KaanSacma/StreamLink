package com.kenta.services.youtube;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class YouTubeAuth {
    private static final Gson gson = new Gson();
    private static final String YOUTUBE_API_BASE = "https://www.googleapis.com/youtube/v3";

    /**
     * Validates the YouTube API key by making a test request
     * Uses a simple search query that doesn't require OAuth
     */
    public static boolean validateApiKey(String apiKey) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            // Clean the API key - remove any whitespace or newlines
            String cleanApiKey = apiKey.trim().replaceAll("\\s+", "");

            // URL encode the API key to handle special characters
            String encodedApiKey = java.net.URLEncoder.encode(cleanApiKey, "UTF-8");

            // Test API key with a simple search request (doesn't require OAuth)
            // Just search for "test" to verify the API key works
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(YOUTUBE_API_BASE + "/search?part=id&maxResults=1&q=test&type=video&key=" + encodedApiKey))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Check if request was successful
            if (response.statusCode() == 200) {
                return true;
            }

            // Log the error for debugging
            System.err.println("[YouTubeAuth] API validation failed with status " + response.statusCode());
            System.err.println("[YouTubeAuth] Response: " + response.body());

            // If we got 403, the API key might be valid but restricted
            // Check the error message
            if (response.statusCode() == 403) {
                JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                if (json.has("error")) {
                    JsonObject error = json.getAsJsonObject("error");
                    String message = error.get("message").getAsString();

                    System.err.println("[YouTubeAuth] Error message: " + message);

                    // If it says "YouTube Data API has not been used" or quota exceeded,
                    // the key itself is valid
                    if (message.contains("YouTube Data API") || message.contains("quota")) {
                        System.out.println("[YouTubeAuth] API key is valid but: " + message);
                        return true;
                    }
                }
            }

            return false;
        } catch (Exception e) {
            System.err.println("[YouTubeAuth] Error validating API key: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets the channel ID from a channel handle or custom URL
     * This is a helper method - users should provide their Channel ID directly
     */
    public static String getChannelId(String channelIdentifier, String apiKey) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // If it already looks like a channel ID (starts with UC and is 24 chars), return it
        if (channelIdentifier.startsWith("UC") && channelIdentifier.length() == 24) {
            return channelIdentifier;
        }

        // Try to get channel by username/handle
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(YOUTUBE_API_BASE + "/channels?part=id&forUsername=" + channelIdentifier + "&key=" + apiKey))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Failed to get channel info: " + response.body());
        }

        JsonObject json = gson.fromJson(response.body(), JsonObject.class);

        if (json.getAsJsonArray("items").isEmpty()) {
            // Try searching by channel name as fallback
            throw new Exception("Channel not found. Please provide your Channel ID directly (starts with UC, 24 characters). Find it in YouTube Studio → Settings → Channel → Advanced.");
        }

        JsonObject channelData = json.getAsJsonArray("items").get(0).getAsJsonObject();
        return channelData.get("id").getAsString();
    }

    /**
     * Gets the active live chat ID for a channel's current live broadcast
     * Uses search API instead of channelId parameter to avoid OAuth issues
     */
    public static String getActiveLiveChatId(String channelId, String apiKey) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // Clean and encode the API key
        String cleanApiKey = apiKey.trim().replaceAll("\\s+", "");
        String encodedApiKey = java.net.URLEncoder.encode(cleanApiKey, "UTF-8");

        // Step 1: Search for live broadcasts from this channel
        // Using search instead of liveBroadcasts with channelId to avoid OAuth requirements
        HttpRequest searchRequest = HttpRequest.newBuilder()
                .uri(URI.create(YOUTUBE_API_BASE + "/search?part=id&channelId=" + channelId + "&eventType=live&type=video&key=" + encodedApiKey))
                .GET()
                .build();

        HttpResponse<String> searchResponse = client.send(searchRequest, HttpResponse.BodyHandlers.ofString());

        if (searchResponse.statusCode() != 200) {
            System.err.println("[YouTubeAuth] Search failed with status " + searchResponse.statusCode());
            System.err.println("[YouTubeAuth] Response: " + searchResponse.body());
            throw new Exception("Failed to search for live streams: " + searchResponse.body());
        }

        JsonObject searchJson = gson.fromJson(searchResponse.body(), JsonObject.class);

        if (searchJson.getAsJsonArray("items").isEmpty()) {
            throw new NoActiveLiveStreamException("No active live stream found. You must be currently streaming on YouTube for chat integration to work.");
        }

        // Get the video ID of the live stream
        String videoId = searchJson.getAsJsonArray("items").get(0)
                .getAsJsonObject()
                .getAsJsonObject("id")
                .get("videoId").getAsString();

        System.out.println("[YouTubeAuth] Found live video ID: " + videoId);

        // Step 2: Get the live broadcast details using the video ID
        HttpRequest videoRequest = HttpRequest.newBuilder()
                .uri(URI.create(YOUTUBE_API_BASE + "/videos?part=liveStreamingDetails&id=" + videoId + "&key=" + encodedApiKey))
                .GET()
                .build();

        HttpResponse<String> videoResponse = client.send(videoRequest, HttpResponse.BodyHandlers.ofString());

        if (videoResponse.statusCode() != 200) {
            System.err.println("[YouTubeAuth] Video details failed with status " + videoResponse.statusCode());
            System.err.println("[YouTubeAuth] Response: " + videoResponse.body());
            throw new Exception("Failed to get live stream details: " + videoResponse.body());
        }

        JsonObject videoJson = gson.fromJson(videoResponse.body(), JsonObject.class);

        if (videoJson.getAsJsonArray("items").isEmpty()) {
            throw new NoActiveLiveStreamException("Could not retrieve live stream details.");
        }

        JsonObject videoItem = videoJson.getAsJsonArray("items").get(0).getAsJsonObject();

        // Check if live streaming details exist
        if (!videoItem.has("liveStreamingDetails")) {
            throw new NoActiveLiveStreamException("This video is not a live stream.");
        }

        JsonObject liveStreamingDetails = videoItem.getAsJsonObject("liveStreamingDetails");

        // Check if activeLiveChatId exists
        if (!liveStreamingDetails.has("activeLiveChatId")) {
            throw new NoActiveLiveStreamException("Live chat is not enabled on your current stream. Please enable chat in YouTube Studio.");
        }

        String liveChatId = liveStreamingDetails.get("activeLiveChatId").getAsString();

        System.out.println("[YouTubeAuth] Found active live chat ID: " + liveChatId);
        return liveChatId;
    }

    /**
     * Custom exception for no active live stream
     */
    public static class NoActiveLiveStreamException extends Exception {
        public NoActiveLiveStreamException(String message) {
            super(message);
        }
    }

    /**
     * Validates OAuth access token
     */
    public static boolean validateOAuthToken(String accessToken) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=" + accessToken))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}