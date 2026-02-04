package com.kenta;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kenta.actions.factory.RuleBuilder;
import com.kenta.commands.StreamlinkCommands;
import com.kenta.config.ConfigItem;
import com.kenta.config.ConfigLoader;
import com.kenta.data.ActionData;
import com.kenta.data.StreamData;
import com.kenta.libs.SLMessage;
import com.kenta.services.StreamThread;
import com.kenta.utils.UpdateChecker;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;

@SuppressWarnings({"null", "removal"})
public class StreamLink extends JavaPlugin {

    private static StreamLink instance;
    private static final String CURRENT_VERSION = "1.3.0";
    private static UpdateChecker.VersionInfo currentVersionInfo;

    public static ComponentType<EntityStore, StreamData> streamDataComponentType;
    public static ComponentType<EntityStore, ActionData> actionDataComponentType;

    private static List<ConfigItem> npcList;
    private static List<ConfigItem> potionList;
    private static Map<String, String> npcMap;
    private static Map<String, String> potionMap;

    public static List<ConfigItem> getNPCList() { return npcList; }
    public static List<ConfigItem> getPotionList() { return potionList; }
    public static Map<String, String> getNPCMap() { return npcMap; }
    public static Map<String, String> getPotionMap() { return potionMap; }

    public StreamLink(@Nonnull JavaPluginInit init) { super(init); }

    public static StreamLink get() { return instance; }

    @Override
    protected void setup() {
        instance = this;
        streamDataComponentType = getEntityStoreRegistry().registerComponent(StreamData.class, "StreamData", StreamData.CODEC);
        actionDataComponentType = getEntityStoreRegistry().registerComponent(ActionData.class, "ActionData", ActionData.CODEC);

        getCommandRegistry().registerCommand(new StreamlinkCommands());
        registerEvents();
        loadConfigurations();

        getLogger().at(Level.INFO).log("StreamLink setup complete!");
    }

    private void loadConfigurations() {
        getLogger().at(Level.INFO).log("Loading configuration files...");

        npcList = ConfigLoader.loadConfig("/com/kenta/config/NPCList.json");
        potionList = ConfigLoader.loadConfig("/com/kenta/config/PotionList.json");

        npcMap = npcList.stream()
                .collect(Collectors.toMap(
                        ConfigItem::getValue,
                        ConfigItem::getDisplay,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        potionMap = potionList.stream()
                .collect(Collectors.toMap(
                        ConfigItem::getValue,
                        ConfigItem::getDisplay,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        getLogger().at(Level.INFO).log("Loaded " + npcMap.size() + " NPCs and " + potionMap.size() + " potions");
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
        boolean isOP = player.hasPermission("OP");
        if (!isOP || !currentVersionInfo.updateAvailable) return;

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        Message updateMessage = Message.join(
                SLMessage.formatMessage("A new version is available!"),
                Message.translation(" (v" + CURRENT_VERSION + " -> v" + currentVersionInfo.latestVersion + ")")
        );
        Message downloadMessage = SLMessage.formatMessageWithLink("Download here: ", currentVersionInfo.downloadUrl);
        player.sendMessage(updateMessage);
        player.sendMessage(downloadMessage);
    }

    @Override
    protected void shutdown() {
        getLogger().at(Level.INFO).log("StreamLink shutting down!");
        StreamThread.disconnectAll();
    }

    private void registerEvents() {
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnectEvent);
        getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerReadyEvent);
    }

    private void onPlayerDisconnectEvent(PlayerDisconnectEvent event) {
        String username = event.getPlayerRef().getUsername();

        if (StreamThread.isUserHasTwitchThread(username)) {
            StreamThread.disconnectTwitch(username);
        }
        if (StreamThread.isUserHasYouTubeThread(username)) {
            StreamThread.disconnectYouTube(username);
        }
    }

    private void onPlayerReadyEvent(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        Ref<EntityStore> playerRef = event.getPlayerRef();
        String username = Objects.requireNonNull(playerRef.getStore().getComponent(playerRef, PlayerRef.getComponentType())).getUsername();

        broadcastUpdateNotification(player);

        Store<EntityStore> entityStore = playerRef.getStore();
        entityStore.ensureComponent(playerRef, streamDataComponentType);
        entityStore.ensureComponent(playerRef, actionDataComponentType);

        StreamData streamData = entityStore.getComponent(playerRef, streamDataComponentType);
        assert streamData != null;

        streamData.setIsTwitchRunning(false);
        streamData.setIsYouTubeRunning(false);

        ActionData actionData = entityStore.getComponent(playerRef, actionDataComponentType);
        assert actionData != null;

        RuleBuilder.loadRulesForPlayer(username, actionData);
    }
}
