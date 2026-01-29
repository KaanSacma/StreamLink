package com.kenta.actions.conditions.types;

import com.kenta.actions.conditions.Condition;
import com.kenta.actions.context.ActionContext;
import com.kenta.services.AbstractChatMessage;

import java.util.List;

public class UserCondition implements Condition {
    private final String username;
    private final List<String> requiredBadges;
    private final boolean anyBadge;

    public UserCondition(String username, List<String> requiredBadges, boolean anyBadge) {
        this.username = username;
        this.requiredBadges = requiredBadges;
        this.anyBadge = anyBadge;
    }

    @Override
    public boolean isMet(ActionContext context) {
        String eventUsername = null;
        List<String> userBadges = null;

        if (context.getChatMessage() != null) {
            AbstractChatMessage chatMessage = context.getChatMessage();
            eventUsername = chatMessage.username;
            userBadges = chatMessage.badges;
        }
        else if (context.getEventData() != null && context.getEventData().has("user_name")) {
            eventUsername = context.getEventDataString("user_name");
        }

        if (username != null && !username.isEmpty()) {
            if (eventUsername == null || !eventUsername.equalsIgnoreCase(username)) {
                return false;
            }
        }

        if (requiredBadges != null && !requiredBadges.isEmpty()) {
            if (userBadges == null || userBadges.isEmpty()) {
                return false;
            }

            if (anyBadge) {
                boolean hasAnyBadge = false;
                for (String requiredBadge : requiredBadges) {
                    if (userBadges.contains(requiredBadge)) {
                        hasAnyBadge = true;
                        break;
                    }
                }
                if (!hasAnyBadge) {
                    return false;
                }
            } else {
                for (String requiredBadge : requiredBadges) {
                    if (!userBadges.contains(requiredBadge)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Override
    public String getType() {
        return "user";
    }

    @Override
    public String[] getSupportedPlatforms() {
        return new String[]{"twitch", "youtube", "kick"};
    }

    @Override
    public String getDescription() {
        StringBuilder desc = new StringBuilder("User");
        if (username != null && !username.isEmpty()) {
            desc.append(": ").append(username);
        }
        if (requiredBadges != null && !requiredBadges.isEmpty()) {
            desc.append(" (badges: ").append(String.join(", ", requiredBadges));
            desc.append(anyBadge ? " - any)" : " - all)");
        }
        return desc.toString();
    }
}
