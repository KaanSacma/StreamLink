package com.kenta.actions;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kenta.actions.context.ActionContext;
import com.kenta.actions.rules.ActionRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionManager {
    private static ActionManager instance;

    private final Map<String, List<ActionRule>> playerRules;

    private ActionManager() {
        this.playerRules = new HashMap<>();
    }

    public static ActionManager getInstance() {
        if (instance == null) {
            instance = new ActionManager();
        }
        return instance;
    }

    public void addRule(String username, ActionRule rule) {
        playerRules.computeIfAbsent(username, k -> new ArrayList<>()).add(rule);
        System.out.println("[ActionManager] Added rule '" + rule.getName() + "' for player: " + username);
    }

    public boolean removeRule(String username, String ruleId) {
        List<ActionRule> rules = playerRules.get(username);
        if (rules == null) {
            return false;
        }

        boolean removed = rules.removeIf(rule -> rule.getId().equals(ruleId));
        if (removed) {
            System.out.println("[ActionManager] Removed rule: " + ruleId + " for player: " + username);
        }
        return removed;
    }

    public List<ActionRule> getRules(String username) {
        return new ArrayList<>(playerRules.getOrDefault(username, new ArrayList<>()));
    }

    public void clearRules(String username) {
        playerRules.remove(username);
        System.out.println("[ActionManager] Cleared all rules for player: " + username);
    }

    public boolean toggleRuleEnabled(String username, String ruleId) {
        List<ActionRule> rules = playerRules.get(username);
        if (rules == null) return false;

        for (int i = 0; i < rules.size(); i++) {
            ActionRule rule = rules.get(i);
            if (rule.getId().equals(ruleId)) {
                ActionRule updatedRule = ActionRule.builder()
                        .id(rule.getId())
                        .name(rule.getName())
                        .enabled(!rule.isEnabled())
                        .condition(rule.getCondition())
                        .actions(rule.getActions())
                        .platform(rule.getEnabledPlatform())
                        .cooldown(rule.getCooldownSeconds())
                        .build();

                rules.set(i, updatedRule);
                return true;
            }
        }
        return false;
    }

    public boolean toggleAllRule(String username, boolean enabled) {
        List<ActionRule> rules = playerRules.get(username);
        if (rules == null) return false;
        for (int i = 0; i < rules.size(); i++) {
            ActionRule rule = rules.get(i);
            ActionRule updatedRule = ActionRule.builder()
                    .id(rule.getId())
                    .name(rule.getName())
                    .enabled(enabled)
                    .condition(rule.getCondition())
                    .actions(rule.getActions())
                    .platform(rule.getEnabledPlatform())
                    .cooldown(rule.getCooldownSeconds())
                    .build();
            rules.set(i, updatedRule);
        }
        return true;
    }

    public void processEvent(ActionContext context, Ref<EntityStore> ref, Store<EntityStore> store) {
        String username = context.getPlayerRef().getUsername();
        List<ActionRule> rules = playerRules.get(username);

        if (rules == null || rules.isEmpty()) {
            return;
        }

        int executedCount = 0;
        for (ActionRule rule : rules) {
            try {
                if (rule.shouldExecute(context)) {
                    rule.execute(context, ref, store);
                    executedCount++;
                }
            } catch (Exception e) {
                System.err.println("[ActionManager] Error executing rule: " + rule.getName());
                e.printStackTrace();
            }
        }

        if (executedCount > 0) {
            System.out.println(String.format(
                    "[ActionManager] Executed %d rules for %s (event: %s, platform: %s)",
                    executedCount, username, context.getEventType(), context.getPlatform()
            ));
        }
    }

    public Map<String, Integer> getStatistics(String username) {
        Map<String, Integer> stats = new HashMap<>();
        List<ActionRule> rules = playerRules.getOrDefault(username, new ArrayList<>());

        stats.put("total", rules.size());
        stats.put("enabled", (int) rules.stream().filter(ActionRule::isEnabled).count());
        stats.put("disabled", (int) rules.stream().filter(r -> !r.isEnabled()).count());

        return stats;
    }
}
