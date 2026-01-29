package com.kenta.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ActionData implements Component<EntityStore> {
    private static final Gson gson = new Gson();

    public static final BuilderCodec<ActionData> CODEC = BuilderCodec.builder(ActionData.class, ActionData::new)
            .append(
                    new KeyedCodec<>("RulesJson", Codec.STRING),
                    (state, json) -> state.rulesJson = json,
                    (state) -> state.rulesJson
            ).add()
            .build();

    private String rulesJson;

    public ActionData() {
        this("[]");
    }

    public ActionData(String rulesJson) {
        this.rulesJson = rulesJson;
    }

    public String getRulesJson() {
        return this.rulesJson;
    }

    public void setRulesJson(String rulesJson) {
        this.rulesJson = rulesJson;
    }

    public List<ActionRuleData> getRules() {
        try {
            Type listType = new TypeToken<List<ActionRuleData>>(){}.getType();
            List<ActionRuleData> rules = gson.fromJson(rulesJson, listType);
            return rules != null ? rules : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("[ActionData] Error deserializing rules: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void setRules(List<ActionRuleData> rules) {
        try {
            this.rulesJson = gson.toJson(rules);
        } catch (Exception e) {
            System.err.println("[ActionData] Error serializing rules: " + e.getMessage());
            this.rulesJson = "[]";
        }
    }

    @NullableDecl
    @Override
    public Component<EntityStore> clone() {
        return new ActionData(this.rulesJson);
    }

    public static class ActionRuleData {
        public String id;
        public String name;
        public boolean enabled;
        public String platform;
        public int cooldownSeconds;
        public ConditionData condition;
        public List<ActionConfigData> actions;

        public ActionRuleData() {
            this.platform = "twitch";
            this.actions = new ArrayList<>();
        }
    }

    public static class ConditionData {
        public String type; // "event", "chat_message"
        public String eventType;
        public String platform;
        public String chatPattern;
        public String matchType;
        public boolean caseSensitive;
        public String username;
        public List<String> requiredBadges;
        public boolean anyBadge;

        public ConditionData() {
            this.requiredBadges = new ArrayList<>();
        }
    }

    public static class ActionConfigData {
        public String type; // "teleport", "spawn_mob", "give_effect", "run_command"

        // Teleport
        public int radiusX;
        public int radiusY;
        public int radiusZ;
        public boolean relative;

        // Spawn Mob
        public String mobType;
        public int count;
        public int radius;

        // Give Effect
        public String effectType;
        public int durationSeconds;

        // Run Command
        public String buffer;
    }
}
