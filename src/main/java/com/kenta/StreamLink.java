package com.kenta;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kenta.commands.StreamCommands;
import com.kenta.data.StreamData;
import com.kenta.libs.SLMessage;
import com.kenta.services.UpdateChecker;
import com.kenta.services.twitch.Twitch;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

@SuppressWarnings({"null", "removal"})
public class StreamLink extends JavaPlugin {

    private static StreamLink instance;
    public static ComponentType<EntityStore, StreamData> streamDataComponentType;
    private static final String CURRENT_VERSION = "1.1.0";
    private static UpdateChecker.VersionInfo currentVersionInfo;

    public StreamLink(@Nonnull JavaPluginInit init) {
        super(init);
    }

    public static StreamLink get() {
        return instance;
    }

    private static final Map<String, Twitch> playersTwitch = new HashMap<>();

    public static Map<String, Twitch> getPlayersTwitch() { return playersTwitch; }
    public static void addPlayersTwitch(String username, Twitch twitch) {
        playersTwitch.put(username, twitch);
    }
    public static void disconnectPlayersTwitch(String username) {
        playersTwitch.get(username).disconnect();
        playersTwitch.remove(username);
    }

    @Override
    protected void setup() {
        instance = this;
        streamDataComponentType = getEntityStoreRegistry().registerComponent(StreamData.class, "StreamData", StreamData.CODEC);

        getCommandRegistry().registerCommand(new StreamCommands());
        registerEvents();

        getLogger().at(Level.INFO).log("StreamLink setup complete!");
    }

    @Override
    protected void start() {
        getLogger().at(Level.INFO).log("StreamLink started!");
        new Thread(this::checkForUpdates).start();
    }

    private void checkForUpdates() {
        getLogger().at(Level.INFO).log("Checking for updates...");

        currentVersionInfo = UpdateChecker.checkForUpdates(CURRENT_VERSION);

        if (currentVersionInfo == null) {
            getLogger().at(Level.WARNING).log("Could not check for updates (network error or GitHub unavailable)");
            return;
        }

        if (currentVersionInfo.updateAvailable) {
            getLogger().at(Level.WARNING).log("========================================");
            getLogger().at(Level.WARNING).log("StreamLink Update Available!");
            getLogger().at(Level.WARNING).log("Current: v" + CURRENT_VERSION);
            getLogger().at(Level.WARNING).log("Latest: v" + currentVersionInfo.latestVersion);
            getLogger().at(Level.WARNING).log("Download: " + currentVersionInfo.downloadUrl);
            if (!currentVersionInfo.changelog.isEmpty()) {
                getLogger().at(Level.WARNING).log("Changelog: " + currentVersionInfo.changelog);
            }
            getLogger().at(Level.WARNING).log("========================================");
        } else {
            getLogger().at(Level.INFO).log("StreamLink is up to date! (v" + CURRENT_VERSION + ")");
        }
    }

    private void broadcastUpdateNotification(Player player) {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        Message updateMessage = Message.join(
                SLMessage.formatMessage("A new version is available!"),
                Message.translation(" (v" + CURRENT_VERSION + " → v" + currentVersionInfo.latestVersion + ")")
        );
        Message downloadMessage = SLMessage.formatMessageWithLink("Download here: ", currentVersionInfo.downloadUrl);
        player.sendMessage(updateMessage);
        player.sendMessage(downloadMessage);
    }

    @Override
    protected void shutdown() {
        for (Map.Entry<String, Twitch> entry : playersTwitch.entrySet()) {
            Twitch twitch = entry.getValue();
            twitch.disconnect();
        }
        playersTwitch.clear();
        getLogger().at(Level.INFO).log("StreamLink shutting down!");
    }

    private void registerEvents() {
        getEventRegistry().registerGlobal(
                PlayerDisconnectEvent.class,
                this::onPlayerDisconnectEvent
        );
        getEventRegistry().registerGlobal(
                PlayerReadyEvent.class, e -> {
                    Store<EntityStore> entityStore = e.getPlayerRef().getStore();
                    entityStore.ensureComponent(e.getPlayerRef(), streamDataComponentType);
                    StreamData streamData = entityStore.getComponent(e.getPlayerRef(), streamDataComponentType);
                    assert streamData != null;
                    streamData.setIsTwitchRunning(false);
                    broadcastUpdateNotification(e.getPlayer());
                }
        );
    }

    private void onPlayerDisconnectEvent(PlayerDisconnectEvent event) {
        String username = event.getPlayerRef().getUsername();

        if (playersTwitch.containsKey(username)) {
            disconnectPlayersTwitch(username);
        }
    }
}
