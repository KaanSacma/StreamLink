package com.kenta.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigLoader {
    private static final Gson gson = new Gson();

    public static List<ConfigItem> loadConfig(String resourcePath) {
        try {
            InputStream inputStream = ConfigLoader.class.getResourceAsStream(resourcePath);

            if (inputStream == null) {
                System.err.println("[ConfigLoader] Could not find resource: " + resourcePath);
                return new ArrayList<>();
            }

            InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            Type listType = new TypeToken<List<ConfigItem>>(){}.getType();
            List<ConfigItem> items = gson.fromJson(reader, listType);

            reader.close();

            System.out.println("[ConfigLoader] Loaded " + items.size() + " items from " + resourcePath);
            return items;

        } catch (Exception e) {
            System.err.println("[ConfigLoader] Error loading config from " + resourcePath + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static Map<String, String> listToMap(List<ConfigItem> items) {
        Map<String, String> map = new HashMap<>();
        for (ConfigItem item : items) {
            map.put(item.getValue(), item.getDisplay());
        }
        return map;
    }
}