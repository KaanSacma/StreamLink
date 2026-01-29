package com.kenta.actions.factory;

import com.kenta.actions.Action;
import com.kenta.actions.types.*;
import com.kenta.data.ActionData.ActionConfigData;

public class ActionFactory {

    public static Action createAction(ActionConfigData config) {
        if (config == null || config.type == null) {
            throw new IllegalArgumentException("Invalid action configuration");
        }

        switch (config.type.toLowerCase()) {
            case "teleport":
                return new TeleportAction(
                        config.radiusX,
                        config.radiusY,
                        config.radiusZ,
                        config.relative
                );

            case "spawn_mob":
                return new SpawnMobAction(
                        config.mobType != null ? config.mobType : "Zombie",
                        config.count,
                        config.radius
                );

            case "give_effect":
                return new GiveEffectAction(
                        config.effectType != null ? config.effectType : "Potion_Health",
                        config.durationSeconds
                );

            case "run_command":
                return new RunCommandAction(config.buffer);

            default:
                throw new IllegalArgumentException("Unknown action type: " + config.type);
        }
    }

    public static ActionConfigData createConfig(Action action) {
        ActionConfigData config = new ActionConfigData();
        config.type = action.getType();

        return config;
    }
}
