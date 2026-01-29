package com.kenta.actions.conditions;

import com.kenta.actions.context.ActionContext;

public interface Condition {

    boolean isMet(ActionContext context);
    String getType();
    String[] getSupportedPlatforms();
    String getDescription();

    default boolean supportsPlatform(String platform) {
        for (String supported : getSupportedPlatforms()) {
            if (supported.equalsIgnoreCase(platform)) {
                return true;
            }
        }
        return false;
    }
}
