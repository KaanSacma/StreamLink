package com.kenta.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class UpdateChecker {
    private static final String VERSION_URL = "https://raw.githubusercontent.com/KaanSacma/StreamLink/main/version.json";
    private static final Gson gson = new Gson();

    public static class VersionInfo {
        public final String latestVersion;
        public final String downloadUrl;
        public final String changelog;
        public final boolean updateAvailable;

        public VersionInfo(String latestVersion, String downloadUrl, String changelog, boolean updateAvailable) {
            this.latestVersion = latestVersion;
            this.downloadUrl = downloadUrl;
            this.changelog = changelog;
            this.updateAvailable = updateAvailable;
        }
    }

    public static VersionInfo checkForUpdates(String currentVersion) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(VERSION_URL))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("[UpdateChecker] Failed to fetch version info: HTTP " + response.statusCode());
                return null;
            }

            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            String latestVersion = json.get("latest_version").getAsString();
            String downloadUrl = json.get("download_url").getAsString();
            String changelog = json.has("changelog") ? json.get("changelog").getAsString() : "";

            boolean updateAvailable = isNewerVersion(currentVersion, latestVersion);

            return new VersionInfo(latestVersion, downloadUrl, changelog, updateAvailable);

        } catch (Exception e) {
            System.err.println("[UpdateChecker] Error checking for updates: " + e.getMessage());
            return null;
        }
    }

    private static boolean isNewerVersion(String current, String latest) {
        try {
            String[] currentParts = current.split("\\.");
            String[] latestParts = latest.split("\\.");

            for (int i = 0; i < Math.max(currentParts.length, latestParts.length); i++) {
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;

                if (latestPart > currentPart) {
                    return true;
                } else if (latestPart < currentPart) {
                    return false;
                }
            }

            return false;

        } catch (Exception e) {
            System.err.println("[UpdateChecker] Error parsing versions: " + e.getMessage());
            return false;
        }
    }

    public static String formatVersionComparison(String current, String latest) {
        return "Current: v" + current + " | Latest: v" + latest;
    }
}
