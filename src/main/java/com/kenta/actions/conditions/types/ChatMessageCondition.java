package com.kenta.actions.conditions.types;

import com.kenta.actions.conditions.Condition;
import com.kenta.actions.context.ActionContext;
import com.kenta.services.AbstractChatMessage;

public class ChatMessageCondition implements Condition {
    private final String pattern;
    private final MatchType matchType;
    private final boolean caseSensitive;

    public enum MatchType {
        EQUALS,
        CONTAINS,
        STARTS_WITH,
        ENDS_WITH
    }

    public ChatMessageCondition(String pattern, MatchType matchType, boolean caseSensitive) {
        this.pattern = pattern;
        this.matchType = matchType;
        this.caseSensitive = caseSensitive;
    }

    @Override
    public boolean isMet(ActionContext context) {
        AbstractChatMessage chatMessage = context.getChatMessage();
        if (chatMessage == null || chatMessage.message == null) {
            return false;
        }

        String message = chatMessage.message;
        String comparePattern = pattern;

        if (!caseSensitive) {
            message = message.toLowerCase();
            comparePattern = comparePattern.toLowerCase();
        }

        return switch (matchType) {
            case EQUALS -> message.equals(comparePattern);
            case CONTAINS -> message.contains(comparePattern);
            case STARTS_WITH -> message.startsWith(comparePattern);
            case ENDS_WITH -> message.endsWith(comparePattern);
            default -> false;
        };
    }

    @Override
    public String getType() {
        return "chat_message";
    }

    @Override
    public String[] getSupportedPlatforms() {
        return new String[]{"twitch", "youtube", "kick"};
    }

    @Override
    public String getDescription() {
        return String.format("Chat message %s '%s'", matchType.name().toLowerCase(), pattern);
    }
}
