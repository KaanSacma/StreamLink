package com.kenta.commands;

import static com.kenta.StreamLink.*;

import com.kenta.data.StreamData;
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

import javax.annotation.Nonnull;

public class StreamCommands extends AbstractCommandCollection {

    public StreamCommands() {
        super("streamlink", "Stream service integration.");

        this.addAliases("sl");
        this.addSubCommand(new TwitchCommands());
        this.addSubCommand(new SLDashboard());
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

            this.addSubCommand(new TwitchInitCommands());
            this.addSubCommand(new TwitchLaunchCommands());
            this.addSubCommand(new TwitchStopCommands());
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
            context.sendMessage(Message.translation("Twitch channel set to: " + channel));
        }
    }

    static class TwitchLaunchCommands extends AbstractPlayerCommand {

        TwitchLaunchCommands() {
            super("connect", "Connect to your Twitch chat.", false);
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
                context.sendMessage(Message.translation("[Error] Set your channel first: /streamlink twitch set <channel>"));
                return;
            }
            
            if (getPlayersTwitch().containsKey(username)) {
                context.sendMessage(Message.translation("[Error] Already connected! Disconnect first: /streamlink twitch disconnect"));
                return;
            }

            Twitch twitch = new Twitch();
            addPlayersTwitch(username, twitch);
            connectPlayersTwitch(player, streamData, username);
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
                context.sendMessage(Message.translation("[Error] Not connected! Connect first: /streamlink twitch connect"));
                return;
            }

            disconnectPlayersTwitch(username);
        }
    }
}
