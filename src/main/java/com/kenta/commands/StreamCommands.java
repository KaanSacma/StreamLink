package com.kenta.commands;

import static com.kenta.StreamLink.*;

import com.kenta.data.StreamData;
import com.kenta.libs.SLMessage;
import com.kenta.pages.DashboardPage;
import com.kenta.services.twitch.Twitch;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kenta.services.twitch.TwitchAuth;

import javax.annotation.Nonnull;

public class StreamCommands extends AbstractCommandCollection {

    public StreamCommands() {
        super("streamlink", "Stream service integration.");

        this.addAliases("sl");
        this.addSubCommand(new SLDashboard());
        this.addSubCommand(new TwitchCommands());
    }

    static class SLDashboard extends AbstractPlayerCommand {
        SLDashboard() {
            super("ui", "Open Dashboard UI", false);
        }

        @Override
        protected void execute(
                @Nonnull CommandContext context,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            StreamData streamData = store.getComponent(ref, streamDataComponentType);
            Player player = store.getComponent(ref, Player.getComponentType());
            DashboardPage page = new DashboardPage(playerRef, streamData);

            assert player != null;

            player.getPageManager().openCustomPage(ref, store, page);
        }
    }

    static class TwitchCommands extends AbstractCommandCollection {

        TwitchCommands() {
            super("twitch", "Twitch integration commands.");

            this.addSubCommand(new TwitchSetupCommands());
            this.addSubCommand(new TwitchInitCommands());
            this.addSubCommand(new TwitchLaunchCommands());
            this.addSubCommand(new TwitchStopCommands());
        }
    }

    static class TwitchSetupCommands extends AbstractPlayerCommand {

        TwitchSetupCommands() {
            super("setup", "Set up your Twitch Client ID and Access Token.", false);

            this.withRequiredArg("client_id", "Your Twitch Client ID", ArgTypes.STRING);
            this.withRequiredArg("access_token", "Your User Access Token", ArgTypes.STRING);
        }

        @Override
        protected void execute(
                @Nonnull CommandContext context,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            StreamData streamData = store.getComponent(ref, streamDataComponentType);
            String clientId = (String) context.get(this.getRequiredArguments().get(0));
            String accessToken = (String) context.get(this.getRequiredArguments().get(1));

            assert streamData != null;

            streamData.setTwitchClientId(clientId);
            streamData.setTwitchAccessToken(accessToken);

            context.sendMessage(SLMessage.formatMessage("Twitch credentials saved!"));
            context.sendMessage(SLMessage.formatMessage("Next: Set your channel with /streamlink twitch set <name>"));
        }
    }

    static class TwitchInitCommands extends AbstractPlayerCommand {

        TwitchInitCommands() {
            super("set", "Set your Twitch channel name.", false);

            this.withRequiredArg("channel", "Your Twitch channel name.", ArgTypes.STRING);
        }

        @Override
        protected void execute(
                @Nonnull CommandContext context,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            StreamData streamData = store.getComponent(ref, streamDataComponentType);
            String channel = (String) context.get(this.getRequiredArguments().getFirst());

            assert streamData != null;

            streamData.setTwitchChannel(channel);
            context.sendMessage(SLMessage.formatMessage("Twitch channel set to: " + channel));
        }
    }

    static class TwitchLaunchCommands extends AbstractPlayerCommand {

        TwitchLaunchCommands() {
            super("connect", "Connect to your Twitch chat and events.", false);
        }

        @Override
        protected void execute(
                @Nonnull CommandContext context,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            StreamData streamData = store.getComponent(ref, streamDataComponentType);
            Player player = store.getComponent(ref, Player.getComponentType());
            String username = playerRef.getUsername();

            assert streamData != null;

            if (streamData.getTwitchChannel().isEmpty()) {
                playerRef.sendMessage(SLMessage.formatMessageWithError("Set your channel first: /streamlink twitch channel <name>"));
                return;
            }

            if (streamData.getTwitchClientId().isEmpty() || streamData.getTwitchAccessToken().isEmpty()) {
                playerRef.sendMessage(SLMessage.formatMessageWithError("Run setup first: /streamlink twitch setup <client_id> <access_token>"));
                playerRef.sendMessage(SLMessage.formatMessageWithLink("Please check this link: ", "https://twitchtokengenerator.com/quick/HvO1CktuVV"));
                return;
            }

            if (getPlayersTwitch().containsKey(username)) {
                playerRef.sendMessage(SLMessage.formatMessageWithError("Already connected!"));
                return;
            }

            playerRef.sendMessage(SLMessage.formatMessage("Validating credentials..."));

            new Thread(() -> {
                try {
                    if (!TwitchAuth.validateToken(streamData.getTwitchAccessToken())) {
                        playerRef.sendMessage(SLMessage.formatMessage("Invalid or expired access token!"));
                        playerRef.sendMessage(SLMessage.formatMessageWithLink("Please generate a new token at ", "https://twitchtokengenerator.com"));
                        return;
                    }

                    playerRef.sendMessage(SLMessage.formatMessage("Token validated!"));
                    playerRef.sendMessage(SLMessage.formatMessage("Connecting to Twitch..."));

                    String broadcasterId = TwitchAuth.getBroadcasterId(
                            streamData.getTwitchChannel(),
                            streamData.getTwitchClientId(),
                            streamData.getTwitchAccessToken()
                    );

                    streamData.setBroadcasterId(broadcasterId);

                    playerRef.sendMessage(SLMessage.formatMessage("Authentication successful!"));
                    playerRef.sendMessage(SLMessage.formatMessage("Connecting to chat and events..."));

                    Twitch twitch = new Twitch();
                    addPlayersTwitch(username, twitch);
                    twitch.connectWithEvents(streamData, player);

                } catch (Exception e) {
                    playerRef.sendMessage(SLMessage.formatMessageWithError("Connection failed: " + e.getMessage()));
                    e.printStackTrace();
                }
            }).start();
        }
    }

    static class TwitchStopCommands extends AbstractPlayerCommand {

        TwitchStopCommands() {
            super("disconnect", "Disconnect from your Twitch chat.", false);
        }

        @Override
        protected void execute(
                @Nonnull CommandContext context,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            String username = playerRef.getUsername();

            if (!getPlayersTwitch().containsKey(username)) {
                playerRef.sendMessage(SLMessage.formatMessageWithError("Not connected! Connect first: /streamlink twitch connect"));
                return;
            }

            disconnectPlayersTwitch(username);
        }
    }
}
