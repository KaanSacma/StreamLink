package com.kenta.commands;

import static com.kenta.StreamLink.*;

import com.kenta.data.StreamData;
import com.kenta.hud.ChatHUD;
import com.kenta.pages.DashboardPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
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
        this.addSubCommand(new SLDashboard());
        //this.addSubCommand(new SLChatHUD()); // TODO: Add back ChatHUD after YouTube and Kick are implemented.
        this.addSubCommand(new TwitchCommands.TwitchMainCommand());
        this.addSubCommand(new YouTubeCommands.YouTubeMainCommand());
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

        @Override
        protected boolean canGeneratePermission() { return false; }
    }

    static class SLChatHUD extends AbstractPlayerCommand {
        SLChatHUD() {
            super("hud", "Trigger Chat HUD", false);
        }

        @Override
        protected void execute(
                @Nonnull CommandContext context,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            Player player = store.getComponent(ref, Player.getComponentType());
            ChatHUD chatHUD = new ChatHUD(playerRef);

            assert player != null;

            player.getHudManager().setCustomHud(playerRef, chatHUD);
        }

        @Override
        protected boolean canGeneratePermission() { return false; }
    }

    @Override
    protected boolean canGeneratePermission() { return false; }
}
