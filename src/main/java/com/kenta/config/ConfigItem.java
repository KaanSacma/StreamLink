package com.kenta.config;

public class ConfigItem {
    private String display;
    private String value;

    public ConfigItem() {}

    public ConfigItem(String display, String value) {
        this.display = display;
        this.value = value;
    }

    public String getDisplay() { return display; }
    public void setDisplay(String display) { this.display = display; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
