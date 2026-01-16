package com.kenta.services.twitch;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TwitchAuth {
    private static final Gson gson = new Gson();

    public static String getBroadcasterId(String username, String clientId, String accessToken) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.twitch.tv/helix/users?login=" + username))
                .header("Client-Id", clientId)
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Failed to get user info: " + response.body());
        }

        JsonObject json = gson.fromJson(response.body(), JsonObject.class);

        if (json.getAsJsonArray("data").isEmpty()) {
            throw new Exception("User not found: " + username);
        }

        JsonObject userData = json.getAsJsonArray("data").get(0).getAsJsonObject();

        return userData.get("id").getAsString();
    }

    public static boolean validateToken(String accessToken) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://id.twitch.tv/oauth2/validate"))
                    .header("Authorization", "OAuth " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
