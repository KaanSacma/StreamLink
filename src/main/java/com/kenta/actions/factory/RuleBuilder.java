package com.kenta.actions.factory;

import com.kenta.actions.Action;
import com.kenta.actions.ActionManager;
import com.kenta.actions.conditions.Condition;
import com.kenta.actions.rules.ActionRule;
import com.kenta.data.ActionData;

import java.util.ArrayList;
import java.util.List;

public class RuleBuilder {

    public static ActionRule fromData(ActionData.ActionRuleData data) {
        if (data == null) throw new IllegalArgumentException("Rule data cannot be null");

        ActionRule.Builder builder = ActionRule.builder()
                .id(data.id)
                .name(data.name)
                .enabled(data.enabled)
                .cooldown(data.cooldownSeconds);

        // Set platform
        if (data.platform != null) builder.platform(data.platform);

        // Set condition
        if (data.condition != null) {
            try {
                Condition condition = ConditionFactory.createCondition(data.condition);
                builder.condition(condition);
            } catch (Exception e) {
                System.err.println("[RuleBuilder] Error creating condition: " + e.getMessage());
            }
        }

        // Add actions
        if (data.actions != null) {
            for (ActionData.ActionConfigData actionData : data.actions) {
                try {
                    Action action = ActionFactory.createAction(actionData);
                    builder.action(action);
                } catch (Exception e) {
                    System.err.println("[RuleBuilder] Error creating action: " + e.getMessage());
                }
            }
        }

        return builder.build();
    }

    public static ActionData.ActionRuleData toData(ActionRule rule) {
        ActionData.ActionRuleData data = new ActionData.ActionRuleData();

        data.id = rule.getId();
        data.name = rule.getName();
        data.enabled = rule.isEnabled();
        data.platform = rule.getEnabledPlatform();
        data.cooldownSeconds = rule.getCooldownSeconds();

        try {
            data.condition = ConditionFactory.createConfig(rule.getCondition());
        } catch (Exception e) {
            System.err.println("[RuleBuilder] Error serializing condition: " + e.getMessage());
        }

        data.actions = new ArrayList<>();
        for (Action action : rule.getActions()) {
            try {
                data.actions.add(ActionFactory.createConfig(action));
            } catch (Exception e) {
                System.err.println("[RuleBuilder] Error serializing action: " + e.getMessage());
            }
        }

        return data;
    }

    public static void loadRulesForPlayer(String username, ActionData actionData) {
        List<ActionData.ActionRuleData> rulesData = actionData.getRules();

        for (ActionData.ActionRuleData ruleData : rulesData) {
            try {
                ActionRule rule = fromData(ruleData);
                ActionManager.getInstance().addRule(username, rule);
            } catch (Exception e) {
                System.err.println("[RuleBuilder] Error loading rule for " + username + ": " + e.getMessage());
            }
        }

        System.out.println("[RuleBuilder] Loaded " + rulesData.size() + " rules for player: " + username);
    }

    public static void saveRulesForPlayer(String username, ActionData actionData) {
        List<ActionRule> rules = ActionManager.getInstance().getRules(username);
        List<ActionData.ActionRuleData> rulesData = new ArrayList<>();

        for (ActionRule rule : rules) {
            try {
                rulesData.add(toData(rule));
            } catch (Exception e) {
                System.err.println("[RuleBuilder] Error saving rule: " + e.getMessage());
            }
        }

        actionData.setRules(rulesData);
        System.out.println("[RuleBuilder] Saved " + rulesData.size() + " rules for player: " + username);
    }
}
