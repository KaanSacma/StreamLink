package com.kenta.hud;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.kenta.services.twitch.data.TwitchChatMessage;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.util.LinkedList;


public class ChatHUD extends CustomUIHud {
    private static final int MAX_MESSAGES = 40;
    private final LinkedList<TwitchChatMessage> messages = new LinkedList<>();

    public ChatHUD(@Nonnull PlayerRef playerRef) { super(playerRef); }

    @Override
    protected void build(@NonNullDecl UICommandBuilder builder) {
        builder.append("Hud/SL_Chat.ui");

        // Initialize state
        builder.set("#ViewerCount.Text", "0 watching");
        builder.set("#StatusDot.Background", "#E74C3C");

        // Clear messages (start empty)
        builder.clear("#ChatMessages");
    }

    public void addMessage(TwitchChatMessage msg) {
        // Add to queue
        messages.addLast(msg);

        // Remove oldest if over limit
        if (messages.size() > MAX_MESSAGES) {
            messages.removeFirst();
        }

        // Rebuild entire chat
        rebuildChat();
    }

    /**
     * Rebuild chat UI (following RespawnPage pattern)
     */
    private void rebuildChat() {
        UICommandBuilder builder = new UICommandBuilder();

        // Clear existing messages
        builder.clear("#ChatMessages");

        // Add each message
        for (int i = 0; i < messages.size(); i++) {
            TwitchChatMessage msg = messages.get(i);

            // Append message template
            builder.append("#ChatMessages", "Hud/ChatMessage.ui");

            // Build selector for this specific message
            String selector = "#ChatMessages[" + i + "] ";

            // Set username and color
            builder.set(selector + "#Username.Text", msg.username);
            builder.set(selector + "#Username.Style.TextColor", msg.color);

            // Set message text
            builder.set(selector + "#Message.Text", msg.message);
        }

        // Send update
        this.update(false, builder);
    }

    public void setConnected(boolean connected) {
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#StatusDot.Background", connected ? "#00D166" : "#E74C3C");
        this.update(false, builder);
    }

    public void setViewerCount(int count) {
        UICommandBuilder builder = new UICommandBuilder();
        String text = formatViewerCount(count) + " watching";
        builder.set("#ViewerCount.Text", text);
        this.update(false, builder);
    }

    public void clearMessages() {
        messages.clear();
        UICommandBuilder builder = new UICommandBuilder();
        builder.clear("#ChatMessages");
        this.update(false, builder);
    }

    private String formatViewerCount(int count) {
        if (count >= 1000000) {
            return String.format("%.1fM", count / 1000000.0);
        } else if (count >= 1000) {
            return String.format("%.1fK", count / 1000.0);
        }
        return String.valueOf(count);
    }
}
