package com.kenta.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kenta.data.StreamData;
import com.kenta.libs.SLMessage;
import com.kenta.services.StreamThread;
import com.kenta.services.twitch.Twitch;
import com.kenta.services.twitch.TwitchAuth;

import javax.annotation.Nonnull;

import static com.kenta.StreamLink.streamDataComponentType;

public class TwitchCommands {
    static class TwitchMainCommand extends AbstractCommandCollection {

        TwitchMainCommand() {
            super("twitch", "Twitch integration commands.");

            this.addSubCommand(new TwitchSetupCommand());
            this.addSubCommand(new TwitchInitCommand());
            this.addSubCommand(new TwitchLaunchCommand());
            this.addSubCommand(new TwitchStopCommand());
        }

        @Override
        protected boolean canGeneratePermission() { return false; }
    }

    static class TwitchSetupCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> clientIdArg = this.withRequiredArg("client_id", "Your Twitch Client ID", ArgTypes.STRING);
        private final RequiredArg<String> accessTokenArg = this.withRequiredArg("access_token", "Your User Access Token", ArgTypes.STRING);

        TwitchSetupCommand() { super("setup", "Set up your Twitch Client ID and Access Token.", false); }

        @Override
        protected void execute(
                @Nonnull CommandContext context,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            StreamData streamData = store.getComponent(ref, streamDataComponentType);
            String clientId = clientIdArg.get(context);
            String accessToken = accessTokenArg.get(context);

            assert streamData != null;

            streamData.setTwitchClientId(clientId);
            streamData.setTwitchAccessToken(accessToken);

            context.sendMessage(SLMessage.formatMessage("Twitch credentials saved!"));
            context.sendMessage(SLMessage.formatMessage("Next: Set your channel with /streamlink twitch set <name>"));
        }

        @Override
        protected boolean canGeneratePermission() { return false; }
    }

    static class TwitchInitCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> channelArg = this.withRequiredArg("channel", "Your Twitch channel name.", ArgTypes.STRING);

        TwitchInitCommand() { super("set", "Set your Twitch channel name.", false); }

        @Override
        protected void execute(
                @Nonnull CommandContext context,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            StreamData streamData = store.getComponent(ref, streamDataComponentType);
            String channel = channelArg.get(context);

            assert streamData != null;

            streamData.setTwitchChannel(channel);
            context.sendMessage(SLMessage.formatMessage("Twitch channel set to: " + channel));
        }

        @Override
        protected boolean canGeneratePermission() {
            return false;
        }
    }

    static class TwitchLaunchCommand extends AbstractPlayerCommand {

        TwitchLaunchCommand() { super("connect", "Connect to your Twitch chat and events.", false); }

        @Override
        protected void execute(
                @Nonnull CommandContext context,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            StreamData streamData = store.getComponent(ref, streamDataComponentType);
            String username = playerRef.getUsername();

            assert streamData != null;

            if (streamData.getTwitchChannel().isEmpty()) {
                context.sendMessage(SLMessage.formatMessageWithError("Set your channel first: /streamlink twitch channel <name>"));
                context.sendMessage(SLMessage.formatMessageWithLink("Check this setup guide: ", "https://kentatetsu.gitbook.io/streamlink/guides/twitch-setup-guide"));
                return;
            }

            if (streamData.getTwitchClientId().isEmpty() || streamData.getTwitchAccessToken().isEmpty()) {
                context.sendMessage(SLMessage.formatMessageWithError("Run setup first: /streamlink twitch setup <client_id> <access_token>"));
                context.sendMessage(SLMessage.formatMessageWithLink("Check this setup guide: ", "https://kentatetsu.gitbook.io/streamlink/guides/twitch-setup-guide"));
                return;
            }

            if (StreamThread.isUserHasTwitchThread(username)) {
                context.sendMessage(SLMessage.formatMessageWithError("Already connected!"));
                return;
            }

            context.sendMessage(SLMessage.formatMessage("Validating credentials..."));

            new Thread(() -> {
                try {
                    if (!TwitchAuth.validateToken(streamData.getTwitchAccessToken())) {
                        context.sendMessage(SLMessage.formatMessage("Invalid or expired access token!"));
                        context.sendMessage(SLMessage.formatMessageWithLink("Check this troubleshooting guide: ", "https://kentatetsu.gitbook.io/streamlink/guides/twitch-setup-guide/troubleshooting"));
                        return;
                    }

                    context.sendMessage(SLMessage.formatMessage("Token validated!"));
                    context.sendMessage(SLMessage.formatMessage("Connecting to Twitch..."));

                    String broadcasterId = TwitchAuth.getBroadcasterId(
                            streamData.getTwitchChannel(),
                            streamData.getTwitchClientId(),
                            streamData.getTwitchAccessToken()
                    );

                    streamData.setBroadcasterId(broadcasterId);

                    context.sendMessage(SLMessage.formatMessage("Authentication successful!"));
                    context.sendMessage(SLMessage.formatMessage("Connecting to chat and events..."));

                    Twitch twitch = new Twitch(streamData, playerRef);
                    StreamThread.putToTwitch(username, twitch);
                    twitch.connect();

                } catch (Exception e) {
                    context.sendMessage(SLMessage.formatMessageWithError("Connection failed: " + e.getMessage()));
                    if (StreamThread.isUserHasTwitchThread(username))
                        StreamThread.disconnectTwitch(username);
                    e.printStackTrace();
                }
            }).start();
        }

        @Override
        protected boolean canGeneratePermission() {
            return false;
        }
    }

    static class TwitchStopCommand extends AbstractPlayerCommand {

        TwitchStopCommand() { super("disconnect", "Disconnect from your Twitch chat.", false); }

        @Override
        protected void execute(
                @Nonnull CommandContext context,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            String username = playerRef.getUsername();

            if (!StreamThread.isUserHasTwitchThread(username)) {
                playerRef.sendMessage(SLMessage.formatMessageWithError("Not connected! Connect first: /streamlink twitch connect"));
                return;
            }

            StreamThread.disconnectTwitch(username);
        }

        @Override
        protected boolean canGeneratePermission() {
            return false;
        }
    }
}
