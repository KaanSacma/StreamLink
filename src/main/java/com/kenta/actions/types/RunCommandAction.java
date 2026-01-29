package com.kenta.actions.types;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kenta.actions.Action;
import com.kenta.actions.context.ActionContext;

public class RunCommandAction implements Action {
    private final String buffer;

    public RunCommandAction(String buffer) {
        this.buffer = buffer.trim().replaceFirst("^/", "").replaceAll("\\s+", " ");
    }

    @Override
    public void execute(ActionContext context, Ref<EntityStore> ref, Store<EntityStore> store) {
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;
        CommandManager.get().handleCommand(playerRef, buffer);
    }

    public String getBuffer() {
        return buffer;
    }

    @Override
    public String getType() {
        return "run_command";
    }

    @Override
    public String getDescription() {
        return String.format("Run command: %s", buffer);
    }
}
