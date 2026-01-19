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
import com.kenta.services.youtube.YouTube;
import com.kenta.services.youtube.YouTubeAuth;

import javax.annotation.Nonnull;

import static com.kenta.StreamLink.streamDataComponentType;

public class YouTubeCommands {
    static class YouTubeMainCommand extends AbstractCommandCollection {

        YouTubeMainCommand() {
            super("youtube", "YouTube integration commands.");

            this.addSubCommand(new YouTubeSetupCommand());
            this.addSubCommand(new YouTubeSetChannelCommand());
            this.addSubCommand(new YouTubeLaunchCommand());
            this.addSubCommand(new YouTubeStopCommand());
        }

        @Override
        protected boolean canGeneratePermission() { return false; }
    }

    static class YouTubeSetupCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> apiKeyArg = this.withRequiredArg("api_key", "Your YouTube API Key", ArgTypes.STRING);

        YouTubeSetupCommand() {
            super("setup", "Set up your YouTube API Key.", false);
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
            String apiKey = apiKeyArg.get(context);

            assert streamData != null;

            streamData.setYouTubeApiKey(apiKey);

            context.sendMessage(SLMessage.formatMessage("YouTube API Key saved!"));
            context.sendMessage(SLMessage.formatMessage("Next: Set your channel with /streamlink youtube set <channel_id>"));
        }

        @Override
        protected boolean canGeneratePermission() { return false; }
    }

    static class YouTubeSetChannelCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> channelIdArg = this.withRequiredArg("channel_id", "Your YouTube channel ID", ArgTypes.STRING);

        YouTubeSetChannelCommand() {
            super("set", "Set your YouTube channel ID.", false);
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
            String channelId = channelIdArg.get(context);

            assert streamData != null;

            streamData.setYouTubeChannelId(channelId);
            context.sendMessage(SLMessage.formatMessage("YouTube channel ID set to: " + channelId));
        }

        @Override
        protected boolean canGeneratePermission() { return false; }
    }

    static class YouTubeLaunchCommand extends AbstractPlayerCommand {

        YouTubeLaunchCommand() {
            super("connect", "Connect to your YouTube live chat.", false);
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
            String username = playerRef.getUsername();

            assert streamData != null;

            if (streamData.getYouTubeChannelId().isEmpty()) {
                context.sendMessage(SLMessage.formatMessageWithError("Set your channel ID first: /streamlink youtube set <channel_id>"));
                context.sendMessage(SLMessage.formatMessageWithLink("Check this setup guide: ", "https://kentatetsu.gitbook.io/streamlink/guides/youtube-setup-guide"));
                return;
            }

            if (streamData.getYouTubeApiKey().isEmpty()) {
                context.sendMessage(SLMessage.formatMessageWithError("Run setup first: /streamlink youtube setup <api_key>"));
                context.sendMessage(SLMessage.formatMessageWithLink("Check this setup guide: ", "https://kentatetsu.gitbook.io/streamlink/guides/youtube-setup-guide"));
                return;
            }

            if (StreamThread.isUserHasYouTubeThread(username)) {
                context.sendMessage(SLMessage.formatMessageWithError("Already connected!"));
                context.sendMessage(SLMessage.formatMessageWithError("isUserHasYouTubeThread: " + StreamThread.isUserHasYouTubeThread(username)));
                return;
            }

            context.sendMessage(SLMessage.formatMessage("Validating credentials..."));

            new Thread(() -> {
                try {
                    if (!YouTubeAuth.validateApiKey(streamData.getYouTubeApiKey())) {
                        context.sendMessage(SLMessage.formatMessage("Invalid or expired API key!"));
                        context.sendMessage(SLMessage.formatMessageWithLink("Check this troubleshooting guide: ", "https://kentatetsu.gitbook.io/streamlink/guides/youtube-setup-guide/troubleshooting"));
                        return;
                    }

                    context.sendMessage(SLMessage.formatMessage("API Key validated!"));
                    context.sendMessage(SLMessage.formatMessage("Connecting to YouTube live chat..."));

                    YouTube youtube = new YouTube(streamData, playerRef);
                    StreamThread.putToYouTube(username, youtube);
                    youtube.connect();

                } catch (Exception e) {
                    context.sendMessage(SLMessage.formatMessageWithError("A: Connection failed: " + e.getMessage()));
                    e.printStackTrace();
                }
            }).start();
        }

        @Override
        protected boolean canGeneratePermission() { return false; }
    }

    static class YouTubeStopCommand extends AbstractPlayerCommand {

        YouTubeStopCommand() {
            super("disconnect", "Disconnect from your YouTube live chat.", false);
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

            if (!StreamThread.isUserHasYouTubeThread(username)) {
                playerRef.sendMessage(SLMessage.formatMessageWithError("Not connected! Connect first: /streamlink youtube connect"));
                return;
            }

            StreamThread.disconnectYouTube(username);
        }

        @Override
        protected boolean canGeneratePermission() { return false; }
    }
}
