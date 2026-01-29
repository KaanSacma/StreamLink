package com.kenta.data;

public class FormData {
    public String type;
    public Object data;

    public FormData(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    public void setType(String type) { this.type = type; }
    public void setData(Object data) { this.data = data; }

    // Condition Form Data
    public static class TwitchConditionEventData {
        public String event;

        public TwitchConditionEventData(String event) { this.event = event; }

        public void setEvent(String event) { this.event = event; }
    }

    public static class MessageConditionData {
        public String method;
        public String value;

        public MessageConditionData(String method, String value) {
            this.method = method;
            this.value = value;
        }

        public void setMethod(String method) { this.method = method; }
        public void setValue(String value) { this.value = value; }
    }

    // Action Form Data
    public static class TeleportData {
        public int x;
        public int y;
        public int z;
        public boolean isRelative;

        public TeleportData(int x, int y, int z, boolean isRelative) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.isRelative = isRelative;
        }

        public void setX(int x) { this.x = x; }
        public void setY(int y) { this.y = y; }
        public void setZ(int z) { this.z = z; }
        public void setRelative(boolean isRelative) { this.isRelative = isRelative; }
    }

    public static class SpawnMobData {
        public String mobID;
        public int count;
        public int radius;

        public SpawnMobData(String mobID, int count, int radius) {
            this.mobID = mobID;
            this.count = count;
            this.radius = radius;
        }

        public void setMobID(String mobID) { this.mobID = mobID; }
        public void setCount(int count) { this.count = count; }
        public void setRadius(int radius) { this.radius = radius; }
    }

    public static class GiveEffectData {
        public String effectID;
        public int duration;

        public GiveEffectData(String effectID, int duration) {
            this.effectID = effectID;
            this.duration = duration;
        }

        public void setEffectID(String effectID) { this.effectID = effectID; }
        public void setDuration(int duration) { this.duration = duration; }
    }

    public static class RunCommandData {
        public String buffer;

        public RunCommandData(String buffer) {
            this.buffer = buffer;
        }

        public void setBuffer(String buffer) { this.buffer = buffer; }
    }
}
