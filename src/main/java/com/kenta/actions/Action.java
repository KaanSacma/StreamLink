package com.kenta.actions;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kenta.actions.context.ActionContext;

public interface Action {

    void execute(ActionContext context, Ref<EntityStore> ref, Store<EntityStore> store);

    String getType();

    default boolean canExecute(ActionContext context) {
        return true;
    }

    String getDescription();
}
