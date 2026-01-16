package com.kenta.libs;

import java.awt.*;

public class ColorHelper {

    public static Color parseHexColor(String hexColor) {
        try {
            String hex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;

            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);

            return new Color(r, g, b);
        } catch (Exception e) {
            return Color.WHITE;
        }
    }
}
