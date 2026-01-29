package com.kenta.actions.types;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kenta.actions.Action;
import com.kenta.actions.context.ActionContext;

public class GiveEffectAction implements Action {
    private final String effectType;
    private final int durationSeconds;

    public GiveEffectAction(String effectType, int durationSeconds) {
        this.effectType = effectType;
        this.durationSeconds = durationSeconds;
    }

    @Override
    public void execute(ActionContext context, Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        EffectControllerComponent effectControllerComponent = store.getComponent(ref, EffectControllerComponent.getComponentType());

        if (player == null) return;

        EntityEffect effect = new EntityEffect(effectType);
        effectControllerComponent.addEffect(ref, effect, durationSeconds, OverlapBehavior.OVERWRITE, store);

        System.out.println(String.format(
                "[GiveEffectAction] Would apply %s effect (duration: %ds) to %s",
                effectType, durationSeconds, context.getPlayerRef().getUsername()
        ));
    }

    public String getEffectType() {
        return effectType;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    @Override
    public String getType() {
        return "give_effect";
    }

    @Override
    public String getDescription() {
        return String.format("Give effect: %s (%ds)",
                effectType, durationSeconds);
    }
}
