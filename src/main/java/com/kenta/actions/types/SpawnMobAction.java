package com.kenta.actions.types;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector2d;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.kenta.actions.Action;
import com.kenta.actions.context.ActionContext;
import it.unimi.dsi.fastutil.Pair;

import java.util.Random;

public class SpawnMobAction implements Action {
    private final String mobType;
    private final int count;
    private final int radius;
    private final Random random = new Random();

    public SpawnMobAction(String mobType, int count, int radius) {
        this.mobType = mobType;
        this.count = count;
        this.radius = radius;
        System.out.println(String.format(
                "[SpawnMobAction] Mob Setup: mobType=%s | count=%d | radius=%d",
                mobType, count, radius
        ));
    }

    @Override
    public void execute(ActionContext context, Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        Transform playerPos = context.getPlayerRef().getTransform();
        World world = player.getWorld();

        world.execute(() -> {
            for (int i = 0; i < count; i++) {
                Vector2d offset = new Vector2d(
                        (random.nextDouble() * 2 - 1) * radius,
                        (random.nextDouble() * 2 - 1) * radius
                );
                Vector3d spawnMob = new Vector3d(
                        playerPos.getPosition().x + offset.x,
                        playerPos.getPosition().y,
                        playerPos.getPosition().z + offset.y
                );
                Vector3f rotationMob = new Vector3f(0, 0, 0);

                Pair<Ref<EntityStore>, INonPlayerCharacter> result =
                        NPCPlugin.get().spawnNPC(
                                store,
                                mobType,
                                null,
                                spawnMob,
                                rotationMob
                        );

                if (result == null) {
                    System.out.println("[SpawnMobAction] Mob Spawn failed!");
                    continue;
                };

                System.out.println(String.format(
                        "[SpawnMobAction] Would spawn %s at %.2f, %.2f, %.2f",
                        mobType, spawnMob.x, spawnMob.y, spawnMob.z
                ));
            }
        });
    }

    @Override
    public String getType() {
        return "spawn_mob";
    }

    @Override
    public String getDescription() {
        return String.format("Spawn %d %s (radius: %d)", count, mobType, radius);
    }

    @Override
    public boolean canExecute(ActionContext context) {
        // TODO: Add validation when Hytale's entity API is available
        return true;
    }
}
