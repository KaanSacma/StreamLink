package com.kenta.actions.conditions.types;

import com.kenta.actions.conditions.Condition;
import com.kenta.actions.context.ActionContext;

public class EventCondition implements Condition {
    private final String eventType;
    private final String platform;

    public EventCondition(String eventType, String platform) {
        this.eventType = eventType;
        this.platform = platform;
    }

    @Override
    public boolean isMet(ActionContext context) {
        if (!eventType.equals(context.getEventType())) {
            return false;
        }

        if (platform != null && !platform.isEmpty() && !platform.equals(context.getPlatform())) {
            return false;
        }

        return true;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPlatform() {
        return platform;
    }

    @Override
    public String getType() {
        return "event";
    }

    @Override
    public String[] getSupportedPlatforms() {
        return new String[]{"twitch", "youtube", "kick"};
    }

    @Override
    public String getDescription() {
        return String.format("Event: %s (platform: %s)",
                eventType, platform != null ? platform : "any");
    }
}
