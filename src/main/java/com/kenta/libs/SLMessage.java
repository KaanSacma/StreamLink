package com.kenta.libs;

import com.hypixel.hytale.server.core.Message;

import java.awt.*;

public class SLMessage {
    private static final String DEFAULT_PREFIX = "[StreamLink] ";
    private static final String ERROR_PREFIX = "[ERROR] ";
    private static final String DEBUG_PREFIX = "[DEBUG] ";
    private static final Color DEFAULT_PREFIX_COLOR = ColorHelper.parseHexColor("#F5CFF9");
    private static final Color ERROR_PREFIX_COLOR = Color.RED;
    private static final Color DEBUG_PREFIX_COLOR = Color.BLUE;

    public static Message formatMessage(String message) {
        return Message.join(
                Message.translation(DEFAULT_PREFIX).color(DEFAULT_PREFIX_COLOR),
                Message.translation(message)
        );
    }

    public static Message formatMessageWithDebug(String message) {
        return Message.join(
                Message.translation(DEFAULT_PREFIX).color(DEFAULT_PREFIX_COLOR),
                Message.translation(DEBUG_PREFIX).color(DEBUG_PREFIX_COLOR),
                Message.translation(message)
        );
    }

    public static Message formatMessageWithError(String message) {
        return Message.join(
                Message.translation(DEFAULT_PREFIX).color(DEFAULT_PREFIX_COLOR),
                Message.translation(ERROR_PREFIX).color(ERROR_PREFIX_COLOR),
                Message.translation(message)
        );
    }

    public static Message formatMessageWithLink(String message, String link) {
        return Message.join(
                Message.translation(DEFAULT_PREFIX).color(DEFAULT_PREFIX_COLOR),
                Message.translation(message),
                Message.translation(link).bold(true).link(link)
        );
    }
}
