package com.kenta.services.twitch;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.kenta.services.twitch.data.ChatMessage;

import java.io.*;
import java.net.Socket;

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

        // Request tags
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
            if (line.startsWith("@")) {
                color = extractColor(line);
            }

            int userStart = line.indexOf(':', 0) + 1;
            int userEnd = line.indexOf('!', userStart);
            String username = line.substring(userStart, userEnd);

            int msgStart = line.indexOf(':', userEnd) + 1;
            String message = line.substring(msgStart).trim();

            return new ChatMessage(username, message, color);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String extractColor(String line) {
        try {
            int tagsEnd = line.indexOf(" :");
            if (tagsEnd == -1) return "#FFFFFF";

            String tags = line.substring(1, tagsEnd);
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

    private String generateRandomColor() {
        String[] colors = {
                "#FF0000", "#0000FF", "#00FF00", "#B22222", "#FF7F50",
                "#9ACD32", "#FF4500", "#2E8B57", "#DAA520", "#D2691E",
                "#5F9EA0", "#1E90FF", "#FF69B4", "#8A2BE2", "#00FF7F"
        };
        return colors[(int)(Math.random() * colors.length)];
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
