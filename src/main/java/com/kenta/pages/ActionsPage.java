package com.kenta.pages;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kenta.actions.Action;
import com.kenta.actions.ActionManager;
import com.kenta.actions.conditions.Condition;
import com.kenta.actions.conditions.types.ChatMessageCondition;
import com.kenta.actions.conditions.types.EventCondition;
import com.kenta.actions.factory.RuleBuilder;
import com.kenta.actions.rules.ActionRule;
import com.kenta.actions.types.GiveEffectAction;
import com.kenta.actions.types.RunCommandAction;
import com.kenta.actions.types.SpawnMobAction;
import com.kenta.actions.types.TeleportAction;
import com.kenta.data.ActionData;
import com.kenta.data.FormData;
import com.kenta.flowui.core.EventDispatcher;
import com.kenta.flowui.core.UIBuilder;
import com.kenta.flowui.core.UIState;
import com.kenta.flowui.data.InteractiveData;
import com.kenta.libs.SLMessage;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.kenta.StreamLink.getNPCMap;
import static com.kenta.StreamLink.getPotionMap;

public class ActionsPage extends InteractiveCustomUIPage<InteractiveData> {

    private UIBuilder ui;
    private EventDispatcher dispatcher;

    private UIState<String> newRuleName = new UIState<>("New Rule");
    private UIState<String> newPlatformSelected = new UIState<>("twitch");
    private UIState<Integer> newCooldown = new UIState<>(0);
    private UIState<FormData> conditionFormData = new UIState<>(new FormData("event", new FormData.TwitchConditionEventData("channel.follow")));
    private UIState<List<FormData>> actionFormData = new UIState<>(new ArrayList<>());

    private final ActionData actionData;
    private final Ref<EntityStore> entityRef;
    private final Store<EntityStore> entityStore;

    public ActionsPage(
            PlayerRef playerRef,
            ActionData actionData,
            Ref<EntityStore> ref,
            Store<EntityStore> store
    ) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, InteractiveData.CODEC);
        this.actionData = actionData;
        this.entityRef = ref;
        this.entityStore = store;
    }

    @Override
    public void build(
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl UICommandBuilder uiCommandBuilder,
            @NonNullDecl UIEventBuilder uiEventBuilder,
            @NonNullDecl Store<EntityStore> store
    ) {
        ui = new UIBuilder(uiCommandBuilder, "Pages/SL_ActionsPage.ui", this::sendUpdate);

        buildButtons();

        ui.applyAll();
        dispatcher = new EventDispatcher(ui);

        initializeRulesList();
    }

    private void buildButtons() {
        ui.textButton("#AddRuleButton").onClick(this::addRuleHandler).build();
        ui.textButton("#TwitchToggleButton").onClick(() -> { updatePlatformToggle("twitch"); }).build();
        ui.textButton("#YoutubeToggleButton").onClick(() -> { updatePlatformToggle("youtube"); }).build();
        ui.textInput("#RuleNameInput").onChange(newRuleName::set).build();
        ui.dropdown("#ConditionTypeDropdown").onChange(this::updateConditionType).build();
        ui.dropdown("#TwitchEventDropdown")
                .value(((FormData.TwitchConditionEventData) conditionFormData.get().data).event)
                .onChange(this::updateConditionTwitchEventParam)
        .build();
        ui.textButton("#AddActionButton").onClick(this::addActionHandler).build();
        ui.numberInput("#CooldownValue").value(newCooldown.get()).min(0).max(60).onChange(newCooldown::set).build();
        ui.textButton("#CancelButton").onClick(this::handleCancelEdit).build();
        ui.textButton("#CloseEditorButton").onClick(this::handleCancelEdit).build();
        ui.textButton("#SaveRuleButton").onClick(this::handleSaveRule).build();
        ui.textButton("#OnRulesButton").onClick(() -> { this.toggleAllRuleHandler(true); }).build();
        ui.textButton("#OffRulesButton").onClick(() -> { this.toggleAllRuleHandler(false); }).build();
        ui.textButton("#ClearRulesButton").onClick(this::removeAllRuleHandler).build();
    }

    private void addRuleHandler() {
        newRuleName.set("New Rule");
        conditionFormData.set(new FormData("event", new FormData.TwitchConditionEventData("channel.follow")));
        newPlatformSelected.set("twitch");
        actionFormData.get().clear();
        newCooldown.set(0);

        ui.clear("#ActionsList");

        ui.group("#EmptyState").visible(false).update();
        ui.group("#RuleEditor").visible(true).update();

        ui.textInput("#RuleNameInput").value(newRuleName.get()).update();

        ui.group("#TwitchToggleActive").visible(true).update();
        ui.group("#YoutubeToggleActive").visible(false).update();

        ui.dropdown("#ConditionTypeDropdown").value(conditionFormData.get().type).update();
        ui.dropdown("#TwitchEventDropdown").value(((FormData.TwitchConditionEventData) conditionFormData.get().data).event).update();
        ui.group("#ConditionEventParams").visible(true).update();
        ui.group("#ConditionMessageParams").visible(false).update();
        //ui.group("#ConditionUserParams").visible(false).update();
    }

    private void updatePlatformToggle(String newPlatform) {
        if (newPlatformSelected.get().equals(newPlatform)) return;

        String selector = "#" + capitalize(newPlatform) + "ToggleActive";
        newPlatformSelected.set(newPlatform);

        ui.group("#TwitchToggleActive").visible(false).update();
        ui.group("#YoutubeToggleActive").visible(false).update();
        ui.group(selector).visible(true).update();
    }

    private void updateConditionType(String newType) {
        if (conditionFormData.get().type.equals(newType)) return;

        conditionFormData.get().type = newType;

        ui.group("#ConditionEventParams").visible(false).update();
        ui.group("#ConditionMessageParams").visible(false).update();
        //ui.group("#ConditionUserParams").visible(false).update();

        switch (newType) {
            case "event": {
                conditionFormData.get().data = new FormData.TwitchConditionEventData("channel.follow");
                ui.dropdown("#TwitchEventDropdown")
                        .value(((FormData.TwitchConditionEventData) conditionFormData.get().data).event)
                        .onChange(this::updateConditionTwitchEventParam)
                .update();
                ui.group("#ConditionEventParams").visible(true).update();
                break;
            }
            case "chat_message": {
                conditionFormData.get().data = new FormData.MessageConditionData("equals", "");
                ui.dropdown("#ConditionMessageTypeDropdown")
                        .value(((FormData.MessageConditionData) conditionFormData.get().data).method)
                        .onChange(this::updateConditionMessageMethodParam)
                .update();
                ui.textInput("#ConditionMessagePattern")
                        .value(((FormData.MessageConditionData) conditionFormData.get().data).value)
                        .onChange(this::updateConditionMessageValueParam)
                .update();
                ui.group("#ConditionMessageParams").visible(true).update();
                break;
            }
            case "user": {
                //ui.group("#ConditionUserParams").visible(true).update();
                break;
            }
            default:
                break;
        }

        ui.applyNew();
        dispatcher.refresh();
    }

    private void updateConditionTwitchEventParam(String value) {
        FormData.TwitchConditionEventData data = (FormData.TwitchConditionEventData) conditionFormData.get().data;
        data.event = value;
    }

    private void updateConditionMessageMethodParam(String value) {
        FormData.MessageConditionData data = (FormData.MessageConditionData) conditionFormData.get().data;
        data.method = value;
    }

    private void updateConditionMessageValueParam(String value) {
        FormData.MessageConditionData data = (FormData.MessageConditionData) conditionFormData.get().data;
        data.value = value;
    }

    private void addActionHandler() {
        int index = actionFormData.get().size();
        String selector = "#ActionsList[" + index + "] ";

        actionFormData.get().add(new FormData("teleport", new FormData.TeleportData(0, 0, 0, true)));
        ui.append("#ActionsList", "Pages/Actions/ActionItem.ui");

        ui.dropdown(selector + "#ActionTypeDropdown")
                .value("teleport")
                .onChange(value -> { this.updateActionType(selector, value, false); })
        .build();

        ui.textButton(selector + "#RemoveAction")
                .onClick(() -> { removeActionHandler(selector); })
        .build();

        setTeleportParamInput(selector);

        ui.applyNew();
        dispatcher.refresh();
    }

    private void setCommandParamInput(String selector) {
        int index = extractIndexFromSelector(selector);

        ui.textInput(selector + "#CommandBuffer")
                .value(((FormData.RunCommandData) actionFormData.get().get(index).data).buffer)
                .onChange(value -> { setCommandBuffer(value, index); })
        .build();
    }

    private void setCommandBuffer(String value, int index) {
        actionFormData.get().get(index).setData(new FormData.RunCommandData(value));
    }

    private void setEffectParamInput(String selector) {
        int index = extractIndexFromSelector(selector);

        ui.dropdown(selector + "#EffectType")
                .value(((FormData.GiveEffectData) actionFormData.get().get(index).data).effectID)
                .options(getPotionMap())
                .onChange(value -> { setEffectID(value, index); })
        .build();
        ui.numberInput(selector + "#EffectDuration")
                .value(((FormData.GiveEffectData) actionFormData.get().get(index).data).duration)
                .onChange((int value) -> { setDurationEffect(value, index); })
        .build();
    }

    private void setEffectID(String value, int index) {
        FormData.GiveEffectData oldData = (FormData.GiveEffectData) actionFormData.get().get(index).data;
        actionFormData.get().get(index).setData(new FormData.GiveEffectData(value, oldData.duration));
    }

    private void setDurationEffect(int value, int index) {
        FormData.GiveEffectData oldData = (FormData.GiveEffectData) actionFormData.get().get(index).data;
        actionFormData.get().get(index).setData(new FormData.GiveEffectData(oldData.effectID, value));
    }

    private void setSpawnMobParamInput(String selector) {
        int index = extractIndexFromSelector(selector);

        ui.dropdown(selector + "#MobType")
                .value(((FormData.SpawnMobData) actionFormData.get().get(index).data).mobID)
                .options(getNPCMap()).onChange(value -> {
                    setMobId(value, index);
                })
        .update();
        //ui.textInput(selector + "#MobType")
        //        .value(((FormData.SpawnMobData) actionFormData.get().get(index).data).mobID)
        //        .onChange(value -> { setMobId(value, index); })
        //.build();
        ui.numberInput(selector + "#MobCount")
                .value(((FormData.SpawnMobData) actionFormData.get().get(index).data).count)
                .onChange((int value) -> { setMobCount(value, index); })
        .update();
        ui.numberInput(selector + "#MobRadius")
                .value(((FormData.SpawnMobData) actionFormData.get().get(index).data).radius)
                .onChange((int value) -> { setMobRadius(value, index); })
        .update();
    }

    private void setMobId(String value, int index) {
        FormData.SpawnMobData oldData = (FormData.SpawnMobData) actionFormData.get().get(index).data;
        actionFormData.get().get(index).setData(new FormData.SpawnMobData(value, oldData.count, oldData.radius));
    }
    private void setMobCount(int value, int index) {
        FormData.SpawnMobData oldData = (FormData.SpawnMobData) actionFormData.get().get(index).data;
        actionFormData.get().get(index).setData(new FormData.SpawnMobData(oldData.mobID, value, oldData.radius));
    }
    private void setMobRadius(int value, int index) {
        FormData.SpawnMobData oldData = (FormData.SpawnMobData) actionFormData.get().get(index).data;
        actionFormData.get().get(index).setData(new FormData.SpawnMobData(oldData.mobID, oldData.count, value));
    }

    private void setTeleportParamInput(String selector) {
        int index = extractIndexFromSelector(selector);

        ui.numberInput(selector + "#TeleportX")
                .value(((FormData.TeleportData) actionFormData.get().get(index).data).x)
                .onChange((int value) -> { setTeleportX(value, index); })
        .build();
        ui.numberInput(selector + "#TeleportY")
                .value(((FormData.TeleportData) actionFormData.get().get(index).data).y)
                .onChange((int value) -> { setTeleportY(value, index); })
        .build();
        ui.numberInput(selector + "#TeleportZ")
                .value(((FormData.TeleportData) actionFormData.get().get(index).data).z)
                .onChange((int value) -> { setTeleportZ(value, index); })
        .build();
        // TODO: ADD Checkbox Input
    }

    private void setTeleportX(int value, int index) {
        FormData.TeleportData oldData = (FormData.TeleportData) actionFormData.get().get(index).data;
        actionFormData.get().get(index).setData(new FormData.TeleportData(value, oldData.y, oldData.z, oldData.isRelative));
    }
    private void setTeleportY(int value, int index) {
        FormData.TeleportData oldData = (FormData.TeleportData) actionFormData.get().get(index).data;
        actionFormData.get().get(index).setData(new FormData.TeleportData(oldData.x, value, oldData.z, oldData.isRelative));
    }
    private void setTeleportZ(int value, int index) {
        FormData.TeleportData oldData = (FormData.TeleportData) actionFormData.get().get(index).data;
        actionFormData.get().get(index).setData(new FormData.TeleportData(oldData.x, oldData.y, value, oldData.isRelative));
    }

    private void updateActionType(String selector, String newType, boolean forceUpdate) {
        int indexToUpdate = extractIndexFromSelector(selector);
        FormData actionForm = actionFormData.get().get(indexToUpdate);

        if (actionForm.type.equals(newType) && !forceUpdate) return;
        actionForm.type = newType;

        ui.group(selector + "#TeleportParams").visible(false).update();
        ui.group(selector + "#SpawnMobParams").visible(false).update();
        ui.group(selector + "#EffectParams").visible(false).update();
        ui.group(selector + "#CommandParams").visible(false).update();

        switch (newType) {
            case "teleport": {
                if (actionFormData.get().get(indexToUpdate).data.getClass() != FormData.TeleportData.class)
                    actionFormData.get().get(indexToUpdate).data = new FormData.TeleportData(0, 0, 0, true);
                setTeleportParamInput(selector);
                ui.group(selector + "#TeleportParams").visible(true).update();
                break;
            }
            case "spawn_mob": {
                if (actionFormData.get().get(indexToUpdate).data.getClass() != FormData.SpawnMobData.class)
                    actionFormData.get().get(indexToUpdate).data = new FormData.SpawnMobData("Random", 0, 0);
                setSpawnMobParamInput(selector);
                ui.group(selector + "#SpawnMobParams").visible(true).update();
                break;
            }
            case "give_effect": {
                if (actionFormData.get().get(indexToUpdate).data.getClass() != FormData.GiveEffectData.class)
                    actionFormData.get().get(indexToUpdate).data = new FormData.GiveEffectData("Potion_Health", 0);
                setEffectParamInput(selector);
                ui.group(selector + "#EffectParams").visible(true).update();
                break;
            }
            case "run_command": {
                if (actionFormData.get().get(indexToUpdate).data.getClass() != FormData.RunCommandData.class)
                    actionFormData.get().get(indexToUpdate).data = new FormData.RunCommandData("");
                setCommandParamInput(selector);
                ui.group(selector + "#CommandParams").visible(true).update();
                break;
            }
        }
        ui.applyNew();
        dispatcher.refresh();
    }

    private void removeActionHandler(String selectorToRemove) {
        int indexToRemove = extractIndexFromSelector(selectorToRemove);

        ui.clear("#ActionsList");
        actionFormData.get().remove(indexToRemove);

        for (int i = 0; i < actionFormData.get().size(); i++) {
            String selector = "#ActionsList[" + i + "] ";

            ui.append("#ActionsList", "Pages/Actions/ActionItem.ui");
            ui.dropdown(selector + "#ActionTypeDropdown")
                    .value(actionFormData.get().get(i).type)
                    .onChange(value -> { this.updateActionType(selector, value, false); })
            .update();
            ui.textButton(selector + "#RemoveAction")
                    .onClick(() -> { removeActionHandler(selector); })
            .update();

            this.updateActionType(selector, actionFormData.get().get(i).type, true);
        }
        ui.applyNew();
        dispatcher.refresh();
    }

    private void handleCancelEdit() {
        String username = playerRef.getUsername();
        List<ActionRule> rules = ActionManager.getInstance().getRules(username);

        ui.group("#RuleEditor").visible(false).update();
        ui.group("#EmptyState").visible(rules.isEmpty()).update();

        newPlatformSelected.set("twitch");
        actionFormData.get().clear();
    }

    private void handleSaveRule() {
        if (newRuleName.get().isEmpty())
            newRuleName.set("New Rule");

        switch (conditionFormData.get().type) {
            case "event": { break; }
            case "chat_message": {
                if (((FormData.MessageConditionData) conditionFormData.get().data).value.isEmpty()) {
                    playerRef.sendMessage(SLMessage.formatMessageWithError("Set a value to Chat Message!"));
                    return;
                }
                break;
            }
            default:
                playerRef.sendMessage(SLMessage.formatMessageWithError("Unknown Condition Type!"));
                return;
        }

        if (actionFormData.get().isEmpty()) {
            playerRef.sendMessage(SLMessage.formatMessageWithError("Add at least one action!"));
            return;
        }

        try {
            ActionRule.Builder builder = ActionRule.builder()
                    .id(UUID.randomUUID().toString())
                    .name(newRuleName.get())
                    .platform(newPlatformSelected.get())
                    .cooldown(Math.max(newCooldown.get(), 0))
                    .enabled(true);
            Condition condition = buildConditionFromForm();

            if (condition == null) {
                playerRef.sendMessage(SLMessage.formatMessageWithError("Your condition trigger is not setup correctly!"));
                return;
            }
            builder.condition(condition);

            for (int i = 0; i < actionFormData.get().size(); i++) {
                Action action = buildActionFromForm(actionFormData.get().get(i));
                if (action == null) {
                    playerRef.sendMessage(SLMessage.formatMessageWithError("Your action " + i + " is not setup correctly!"));
                    return;
                }
                builder.action(action);
            }

            ActionRule rule = builder.build();
            String username = playerRef.getUsername();

            ActionManager.getInstance().addRule(username, rule);
            RuleBuilder.saveRulesForPlayer(username, actionData);

            playerRef.sendMessage(SLMessage.formatMessage("Rule '" + newRuleName.get() + "' created successfully!"));

            handleCancelEdit();

            initializeRulesList();
            newRuleName.set("New Rule");
            conditionFormData.set(new FormData("event", new FormData.TwitchConditionEventData("channel.follow")));
            newPlatformSelected.set("twitch");
            actionFormData.get().clear();
            newCooldown.set(0);
        } catch (Exception e) {
            playerRef.sendMessage(SLMessage.formatMessageWithError("Failed to create rule: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private Condition buildConditionFromForm() {
        switch (conditionFormData.get().type) {
            case "event":
                FormData.TwitchConditionEventData eventData = (FormData.TwitchConditionEventData) conditionFormData.get().data;
                return new EventCondition(eventData.event, newPlatformSelected.get());

            case "chat_message":
                FormData.MessageConditionData messageData = (FormData.MessageConditionData) conditionFormData.get().data;
                ChatMessageCondition.MatchType type = ChatMessageCondition.MatchType.EQUALS;

                switch (messageData.method) {
                    case "contains":
                        type = ChatMessageCondition.MatchType.CONTAINS;
                        break;
                    case "starts_with":
                        type = ChatMessageCondition.MatchType.STARTS_WITH;
                        break;
                    case "ends_with":
                        type = ChatMessageCondition.MatchType.ENDS_WITH;
                        break;
                    default:
                        break;
                }
                return new ChatMessageCondition(messageData.value, type, false);
            default:
                return null;
        }
    }

    private Action buildActionFromForm(FormData formData) {
        return switch (formData.type) {
            case "teleport" -> {
                FormData.TeleportData teleportData = (FormData.TeleportData) formData.data;
                yield new TeleportAction(teleportData.x, teleportData.y, teleportData.z, teleportData.isRelative);
            }
            case "spawn_mob" -> {
                FormData.SpawnMobData spawnMobData = (FormData.SpawnMobData) formData.data;
                yield new SpawnMobAction(spawnMobData.mobID, spawnMobData.count, spawnMobData.radius);
            }
            case "give_effect" -> {
                FormData.GiveEffectData giveEffectData = (FormData.GiveEffectData) formData.data;
                yield new GiveEffectAction(giveEffectData.effectID, giveEffectData.duration);
            }
            case "run_command" -> {
                FormData.RunCommandData runCommandData = (FormData.RunCommandData) formData.data;
                yield new RunCommandAction(runCommandData.buffer);
            }
            default -> null;
        };
    }

    private void initializeRulesList() {
        String username = playerRef.getUsername();
        List<ActionRule> rules = ActionManager.getInstance().getRules(username);

        ui.group("#EmptyState").visible(rules.isEmpty()).update();
        ui.label("#RuleCount").text(rules.size() + " rules").update();
        ui.clear("#RulesList");

        for (int i = 0; i < rules.size(); i++) {
            ActionRule rule = rules.get(i);
            String selector = "#RulesList[" + i + "] ";

            ui.append("#RulesList", "Pages/Actions/RuleItem.ui");
            ui.label(selector + "#RuleName").text(rule.getName()).update();

            String stats = String.format("%d action%s",
                    rule.getActions().size(),
                    rule.getActions().size() == 1 ? "" : "s"
            );
            ui.label(selector + "#RuleStats").text(stats).update();

            String platform = rule.getEnabledPlatform();
            if (platform.equals("twitch")) ui.group(selector + "#TwitchIcon").visible(true).update();
            if (platform.equals("youtube")) ui.group(selector + "#YouTubeIcon").visible(true).update();
            if (platform.equals("kick")) ui.group(selector + "#KickIcon").visible(true).update();

            String toggleText = rule.isEnabled() ? "ON" : "OFF";
            String toggleColor = rule.isEnabled() ? "#00D166" : "#E74C3C";
            ui.textButton(selector + "#EnableToggle").onClick(() -> {
                boolean success = ActionManager.getInstance().toggleRuleEnabled(username, rule.getId());
                if (!success) return;
                RuleBuilder.saveRulesForPlayer(username, actionData);
                initializeRulesList();
            }).text(toggleText).defaultBackground(toggleColor).update();
            ui.textButton(selector + "#DeleteButton").onClick(() -> {
                boolean success = ActionManager.getInstance().removeRule(username, rule.getId());
                if (!success) return;
                RuleBuilder.saveRulesForPlayer(username, actionData);
                initializeRulesList();
            }).update();
            ui.textButton(selector + "#EditButton").onClick(() -> {
                playerRef.sendMessage(SLMessage.formatMessageWithDebug("Edit button coming soon!"));
            }).update();
        }
        ui.applyNew();
        dispatcher.refresh();
    }

    private void removeAllRuleHandler() {
        String username = playerRef.getUsername();

        if (ActionManager.getInstance().getRules(username).isEmpty()) return;

        ActionManager.getInstance().clearRules(username);
        RuleBuilder.saveRulesForPlayer(username, actionData);
        initializeRulesList();
    }

    private void toggleAllRuleHandler(boolean enabled) {
        String username = playerRef.getUsername();

        if (ActionManager.getInstance().getRules(username).isEmpty()) return;

        ActionManager.getInstance().toggleAllRule(username, enabled);
        RuleBuilder.saveRulesForPlayer(username, actionData);
        initializeRulesList();
    }

    @Override
    public void handleDataEvent(
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl Store<EntityStore> store,
            InteractiveData data
    ) {
        if (dispatcher.dispatch(data.eventId, data.value)) return;
        dispatcher.dispatch(data.eventId, data.valueInt);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private int extractIndexFromSelector(String selector) {
        int start = selector.indexOf('[') + 1;
        int end = selector.indexOf(']');
        return Integer.parseInt(selector.substring(start, end));
    }
}
