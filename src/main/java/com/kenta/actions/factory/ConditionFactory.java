package com.kenta.actions.factory;

import com.kenta.actions.conditions.Condition;
import com.kenta.actions.conditions.types.*;
import com.kenta.data.ActionData.ConditionData;

import java.util.ArrayList;

public class ConditionFactory {

    public static Condition createCondition(ConditionData config) {
        if (config == null || config.type == null) {
            throw new IllegalArgumentException("Invalid condition configuration");
        }

        switch (config.type.toLowerCase()) {
            case "event":
                return new EventCondition(
                        config.eventType != null ? config.eventType : "",
                        config.platform
                );

            case "chat_message":
                ChatMessageCondition.MatchType matchType;
                try {
                    matchType = ChatMessageCondition.MatchType.valueOf(
                            config.matchType != null ? config.matchType.toUpperCase() : "CONTAINS"
                    );
                } catch (IllegalArgumentException e) {
                    matchType = ChatMessageCondition.MatchType.CONTAINS;
                }

                return new ChatMessageCondition(
                        config.chatPattern != null ? config.chatPattern : "",
                        matchType,
                        config.caseSensitive
                );

            case "user":
                return new UserCondition(
                        config.username,
                        config.requiredBadges != null ? config.requiredBadges : new ArrayList<>(),
                        config.anyBadge
                );

            default:
                throw new IllegalArgumentException("Unknown condition type: " + config.type);
        }
    }

    public static ConditionData createConfig(Condition condition) {
        ConditionData config = new ConditionData();
        config.type = condition.getType();

        return config;
    }
}
