package com.kenta.actions.types;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kenta.actions.Action;
import com.kenta.actions.context.ActionContext;
import com.kenta.libs.SLMessage;

import java.util.Random;

public class TeleportAction implements Action {
    private final int radiusX;
    private final int radiusY;
    private final int radiusZ;
    private final boolean relative;
    private final Random random = new Random();

    public TeleportAction(int radiusX, int radiusY, int radiusZ, boolean relative) {
        this.radiusX = radiusX;
        this.radiusY = radiusY;
        this.radiusZ = radiusZ;
        this.relative = relative;
    }

    @Override
    public void execute(ActionContext context, Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        Transform currentPos = context.getPlayerRef().getTransform();
        Vector3d offset = new Vector3d(
                (random.nextDouble() * 2 - 1) * radiusX,
                relative ? (random.nextDouble() * 2 - 1) * radiusY : radiusY,
                (random.nextDouble() * 2 - 1) * radiusZ
        );
        Vector3d newPos = new Vector3d(
                relative ? currentPos.getPosition().x + offset.x : offset.x,
                relative ? currentPos.getPosition().y + offset.y : offset.y,
                relative ? currentPos.getPosition().z + offset.z : offset.z
        );
        World world = player.getWorld();

        world.execute(() -> {
            if (player.getReference() == null) return;
            Teleport teleport = Teleport.createForPlayer(world, newPos, currentPos.getRotation());
            store.addComponent(player.getReference(), Teleport.getComponentType(), teleport);
        });
    }

    public int getRadiusX() {
        return radiusX;
    }

    public int getRadiusY() {
        return radiusY;
    }

    public int getRadiusZ() {
        return radiusZ;
    }

    public boolean getRelative() {
        return relative;
    }

    @Override
    public String getType() {
        return "teleport";
    }

    @Override
    public String getDescription() {
        return String.format("Teleport player (radius: %d,%d,%d, relative: %s)",
                radiusX, radiusY, radiusZ, relative);
    }
}
