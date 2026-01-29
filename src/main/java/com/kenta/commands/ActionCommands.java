package com.kenta.commands;

import com.google.gson.JsonObject;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kenta.actions.ActionManager;
import com.kenta.actions.context.ActionContext;
import com.kenta.actions.rules.ActionRule;
import com.kenta.data.ActionData;
import com.kenta.libs.SLMessage;
import com.kenta.pages.ActionsPage;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

import static com.kenta.StreamLink.actionDataComponentType;

public class ActionCommands extends AbstractCommandCollection {

    public ActionCommands() {
        super("actions", "Manage stream-triggered actions.");

        this.addSubCommand(new ActionsUICommand());
        this.addSubCommand(new ListActionsCommand());
        this.addSubCommand(new RemoveActionCommand());
        this.addSubCommand(new ClearActionsCommand());
        this.addSubCommand(new StatsCommand());
        this.addSubCommand(new TestActionCommand());
    }

    static class ActionsUICommand extends AbstractPlayerCommand {

        public ActionsUICommand() {
            super("ui", "Open the Actions management page.", false);
        }

        @Override
        protected void execute(
                @Nonnull CommandContext context,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            ActionData actionData = store.getComponent(ref, actionDataComponentType);
            Player player = store.getComponent(ref, Player.getComponentType());
            ActionsPage page = new ActionsPage(playerRef, actionData, ref, store);

            assert player != null;

            player.getPageManager().openCustomPage(ref, store, page);
        }

        @Override
        protected boolean canGeneratePermission() {
            return false;
        }
    }

    static class ListActionsCommand extends AbstractPlayerCommand {
        ListActionsCommand() {
            super("list", "List all your action rules.", false);
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
            List<ActionRule> rules = ActionManager.getInstance().getRules(username);

            if (rules.isEmpty()) {
                context.sendMessage(SLMessage.formatMessage("You don't have any action rules configured."));
                context.sendMessage(SLMessage.formatMessage("Use /streamlink actions ui to create rules via the dashboard."));
                return;
            }

            context.sendMessage(SLMessage.formatMessage("Your Action Rules:"));

            for (int i = 0; i < rules.size(); i++) {
                ActionRule rule = rules.get(i);
                String status = rule.isEnabled() ? "✓" : "✗";
                String platform = rule.getEnabledPlatform();

                context.sendMessage(SLMessage.formatMessage(
                        String.format("%d. [%s] %s (platform: %s, actions: %d)",
                                i + 1,
                                status,
                                rule.getName(),
                                platform,
                                rule.getActions().size()
                        )
                ));
            }
        }

        @Override
        protected boolean canGeneratePermission() { return false; }
    }

    static class RemoveActionCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> ruleIdArg = this.withRequiredArg("rule_id", "ID of the rule to remove", ArgTypes.STRING);

        RemoveActionCommand() {
            super("remove", "Remove an action rule by ID.", false);
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
            String ruleId = ruleIdArg.get(context);

            boolean removed = ActionManager.getInstance().removeRule(username, ruleId);

            if (removed) {
                context.sendMessage(SLMessage.formatMessage("Rule removed successfully!"));
            } else {
                context.sendMessage(SLMessage.formatMessageWithError("Rule not found: " + ruleId));
            }
        }

        @Override
        protected boolean canGeneratePermission() { return false; }
    }

    static class ClearActionsCommand extends AbstractPlayerCommand {
        ClearActionsCommand() {
            super("clear", "Remove all your action rules.", false);
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
            ActionManager.getInstance().clearRules(username);
            context.sendMessage(SLMessage.formatMessage("All action rules cleared!"));
        }

        @Override
        protected boolean canGeneratePermission() { return false; }
    }

    static class StatsCommand extends AbstractPlayerCommand {
        StatsCommand() {
            super("stats", "View statistics about your action rules.", false);
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
            Map<String, Integer> stats = ActionManager.getInstance().getStatistics(username);

            context.sendMessage(SLMessage.formatMessage("Action Rules Statistics:"));
            context.sendMessage(SLMessage.formatMessage("Total Rules: " + stats.get("total")));
            context.sendMessage(SLMessage.formatMessage("Enabled: " + stats.get("enabled")));
            context.sendMessage(SLMessage.formatMessage("Disabled: " + stats.get("disabled")));
        }

        @Override
        protected boolean canGeneratePermission() { return false; }
    }

    static class TestActionCommand extends AbstractPlayerCommand {
        private final OptionalArg<String> ruleIdArg = this.withOptionalArg("rule_id", "ID of specific rule to test", ArgTypes.STRING);

        TestActionCommand() {
            super("test", "Test action rules (creates a mock follow event).", false);
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

            JsonObject mockEvent = new JsonObject();
            mockEvent.addProperty("user_name", "TestFollower");
            mockEvent.addProperty("user_id", "123456");

            ActionContext testContext = ActionContext.builder()
                            .playerRef(playerRef)
                            .platform("twitch")
                            .eventType("channel.follow")
                            .eventData(mockEvent)
                            .build();

            context.sendMessage(SLMessage.formatMessage("Testing action rules with mock follow event..."));
            ActionManager.getInstance().processEvent(testContext, ref, store);
            context.sendMessage(SLMessage.formatMessage("Test complete! Check if any actions were triggered."));
        }

        @Override
        protected boolean canGeneratePermission() { return false; }
    }

    @Override
    protected boolean canGeneratePermission() { return false; }
}
