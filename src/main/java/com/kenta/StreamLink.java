package com.kenta;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kenta.commands.StreamCommands;
import com.kenta.data.StreamData;
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
    public static void connectPlayersTwitch(Player player, StreamData streamData, String username) {
        playersTwitch.get(username).connectToChannel(streamData, player);
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
