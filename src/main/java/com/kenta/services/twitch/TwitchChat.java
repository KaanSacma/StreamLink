package com.kenta.services.twitch;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.kenta.services.twitch.data.ChatMessage;
import com.kenta.services.twitch.data.EmotePosition;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class TwitchChat {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    public String channel;
    public Player player;
    private boolean connected = false;

    public void connectAnonymous(String channel, Player player) throws IOException {
        this.channel = channel.toLowerCase();
        this.player = player;

        System.out.println("[Twitch] Connecting anonymously to #" + channel);

        socket = new Socket("irc.chat.twitch.tv", 6667);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        // Anonymous login
        String anonNick = "justinfan" + (int)(Math.random() * 100000);
        writer.write("NICK " + anonNick + "\r\n");
        writer.flush();

        // Request tags for colors and emotes
        writer.write("CAP REQ :twitch.tv/tags\r\n");
        writer.flush();

        System.out.println("[Twitch] Logged in as: " + anonNick);

        // Join channel
        writer.write("JOIN #" + this.channel + "\r\n");
        writer.flush();

        connected = true;
        System.out.println("[Twitch] Connected to #" + channel);
    }

    public ChatMessage readMessage() throws IOException {
        if (!connected) return null;

        String line = reader.readLine();
        if (line == null) {
            connected = false;
            return null;
        }

        if (line.startsWith("PING")) {
            writer.write("PONG " + line.substring(5) + "\r\n");
            writer.flush();
            return null;
        }

        if (line.contains("PRIVMSG")) {
            return parseChatMessage(line);
        }

        return null;
    }

    private ChatMessage parseChatMessage(String line) {
        try {
            String color = "#FFFFFF";
            String cleanLine = line;
            Map<String, String> emoteReplacements = new HashMap<>();
            List<String> badges = new ArrayList<>();

            if (line.startsWith("@")) {
                int tagsEnd = line.indexOf(" :");
                if (tagsEnd != -1) {
                    String tags = line.substring(1, tagsEnd);
                    cleanLine = line.substring(tagsEnd + 1);

                    color = extractColorFromTags(tags);
                    emoteReplacements = extractEmotesFromTags(tags);
                    badges = extractBadgesFromTags(tags);
                }
            }

            // Parse username
            int userStart = cleanLine.indexOf(':') + 1;
            int userEnd = cleanLine.indexOf('!', userStart);
            String username = cleanLine.substring(userStart, userEnd);

            // Parse message
            int msgStart = cleanLine.indexOf(':', userEnd) + 1;
            String message = cleanLine.substring(msgStart).trim();

            // Replace emotes in message
            String displayMessage = replaceEmotesInMessage(message, emoteReplacements);

            return new ChatMessage(username, displayMessage, color, badges);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<String> extractBadgesFromTags(String tags) {
        List<String> badgeList = new ArrayList<>();

        try {
            String[] tagPairs = tags.split(";");

            for (String tag : tagPairs) {
                if (tag.startsWith("badges=")) {
                    String badgesData = tag.substring(7);

                    if (badgesData.isEmpty()) { return badgeList; }

                    String[] badgePairs = badgesData.split(",");

                    for (String badgePair : badgePairs) {
                        String[] parts = badgePair.split("/");
                        if (parts.length > 0) {
                            String badgeName = parts[0];
                            badgeList.add(badgeName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return badgeList;
    }

    private String extractColorFromTags(String tags) {
        try {
            String[] tagPairs = tags.split(";");

            for (String tag : tagPairs) {
                if (tag.startsWith("color=")) {
                    String color = tag.substring(6);

                    if (color.isEmpty() || color.equals("")) {
                        return generateRandomColor();
                    }

                    return color;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "#FFFFFF";
    }

    private Map<String, String> extractEmotesFromTags(String tags) {
        Map<String, String> emoteMap = new HashMap<>();

        try {
            String[] tagPairs = tags.split(";");

            for (String tag : tagPairs) {
                if (tag.startsWith("emotes=")) {
                    String emotesData = tag.substring(7);

                    if (emotesData.isEmpty()) {
                        return emoteMap;
                    }

                    String[] emoteGroups = emotesData.split("/");

                    for (String emoteGroup : emoteGroups) {
                        String[] parts = emoteGroup.split(":");
                        if (parts.length < 2) continue;

                        String emoteId = parts[0];
                        String[] positions = parts[1].split(",");

                        for (String position : positions) {
                            emoteMap.put(position, emoteId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return emoteMap;
    }

    private String generateRandomColor() {
        String[] colors = {
                "#FF0000", "#0000FF", "#00FF00", "#B22222", "#FF7F50",
                "#9ACD32", "#FF4500", "#2E8B57", "#DAA520", "#D2691E",
                "#5F9EA0", "#1E90FF", "#FF69B4", "#8A2BE2", "#00FF7F"
        };
        return colors[(int)(Math.random() * colors.length)];
    }

    private String replaceEmotesInMessage(String message, Map<String, String> emoteReplacements) {
        if (emoteReplacements.isEmpty()) { return message; }

        try {
            List<EmotePosition> emotePositions = new ArrayList<>();

            for (Map.Entry<String, String> entry : emoteReplacements.entrySet()) {
                String[] range = entry.getKey().split("-");
                int start = Integer.parseInt(range[0]);
                int end = Integer.parseInt(range[1]);
                String emoteId = entry.getValue();

                emotePositions.add(new EmotePosition(start, end, emoteId));
            }

            emotePositions.sort((a, b) -> Integer.compare(b.start(), a.start()));

            StringBuilder result = new StringBuilder(message);

            for (EmotePosition emote : emotePositions) {
                String emoteText = message.substring(emote.start(), emote.end() + 1);

                String replacement = ":" + emoteText + ":";

                result.replace(emote.start(), emote.end() + 1, replacement);
            }

            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return message;
        }
    }

    public void disconnect() throws IOException {
        connected = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
}
