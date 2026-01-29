package com.kenta.actions.rules;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kenta.actions.Action;
import com.kenta.actions.conditions.Condition;
import com.kenta.actions.context.ActionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ActionRule {
    private final String id;
    private final String name;
    private final boolean enabled;
    private final Condition condition;
    private final List<Action> actions;
    private final String enabledPlatform;
    private final int cooldownSeconds;
    private long lastExecutionTime;

    private ActionRule(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.name = builder.name;
        this.enabled = builder.enabled;
        this.condition = builder.condition;
        this.actions = new ArrayList<>(builder.actions);
        this.enabledPlatform = builder.enabledPlatform;
        this.cooldownSeconds = builder.cooldownSeconds;
        this.lastExecutionTime = 0;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isEnabled() { return enabled; }
    public Condition getCondition() { return condition; }
    public List<Action> getActions() { return new ArrayList<>(actions); }
    public String getEnabledPlatform() { return enabledPlatform; }
    public int getCooldownSeconds() { return cooldownSeconds; }

    public boolean shouldExecute(ActionContext context) {
        if (!enabled) return false;

        if (cooldownSeconds > 0) {
            long currentTime = System.currentTimeMillis() / 1000;
            if (currentTime - lastExecutionTime < cooldownSeconds) return false;
        }

        return condition.isMet(context);
    }

    public void execute(ActionContext context, Ref<EntityStore> ref, Store<EntityStore> store) {
        if (!shouldExecute(context)) return;

        lastExecutionTime = System.currentTimeMillis() / 1000;

        for (Action action : actions) {
            try {
                if (action.canExecute(context)) action.execute(context, ref, store);
            } catch (Exception e) {
                System.err.println("[ActionRule] Error executing action: " + action.getType());
                e.printStackTrace();
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name = "Unnamed Rule";
        private boolean enabled = true;
        private Condition condition;
        private final List<Action> actions = new ArrayList<>();
        private String enabledPlatform;
        private int cooldownSeconds = 0;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder condition(Condition condition) {
            this.condition = condition;
            return this;
        }

        public Builder action(Action action) {
            this.actions.add(action);
            return this;
        }

        public Builder actions(List<Action> actions) {
            this.actions.addAll(actions);
            return this;
        }

        public Builder platform(String platform) {
            this.enabledPlatform = platform;
            return this;
        }

        public Builder cooldown(int seconds) {
            this.cooldownSeconds = seconds;
            return this;
        }

        public ActionRule build() {
            return new ActionRule(this);
        }
    }
}
