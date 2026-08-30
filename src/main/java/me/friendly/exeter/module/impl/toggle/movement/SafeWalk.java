package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

/**
 * SafeWalk. Prevents walking off edges — like holding sneak, without the slow speed.
 *
 * Implementation for 26.2: the module checks the block under the player's NEXT position each
 * tick; if there's no ground within one block below the predicted footprint, the input's
 * forward/backward/strafe impulses are zeroed (input record rebuilt) so vanilla's own edge
 * logic never carries you off. This is client-side prediction only — the server still sees
 * normal movement, so it's undetectable as a "sneak spoof".
 *
 * {@code Only Ahead} restricts the clamp to forward movement (you can still back off edges
 * deliberately, e.g. retreating into a hole during a 5b5t chase).
 */
public class SafeWalk extends ToggleableModule {
    private final Property<Boolean> onlyAhead = new Property<>(true, "Only Ahead", "onlyahead", "ahead");
    private final Property<Boolean> notWhileSneaking = new Property<>(true, "Off While Sneaking", "sneakbypass");

    public SafeWalk() {
        super("SafeWalk", new String[]{"safewalk", "edgeguard", "sw"}, ModuleType.MOVEMENT);
        offerProperties(onlyAhead, notWhileSneaking);

        this.listeners.add(new Listener<TickEvent>("safe_walk_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer player = minecraft.player;
                if (player == null || minecraft.level == null) return;
                if (!player.onGround()) return;
                if (player.input == null || player.input.keyPresses == null) return;

                Input keys = player.input.keyPresses;
                if (notWhileSneaking.getValue() && keys.shift()) return;
                if (!keys.forward() && !keys.backward() && !keys.left() && !keys.right()) return;

                // Predict the next-step footprint from current velocity direction.
                Vec3 vel = player.getDeltaMovement();
                double stepX = vel.x * 1.4; // ~1 tick of movement with friction
                double stepZ = vel.z * 1.4;
                if (Math.abs(stepX) < 0.01 && Math.abs(stepZ) < 0.01) return;

                double px = player.getX() + (stepX > 0 ? 0.3 : -0.3) + stepX;
                double pz = player.getZ() + (stepZ > 0 ? 0.3 : -0.3) + stepZ;
                BlockPos below = BlockPos.containing(px, player.getY() - 1.0, pz);

                if (hasGroundBelow(below)) return;
                if (onlyAhead.getValue() && !keys.forward()) return;

                // No ground ahead: strip horizontal impulses for this tick.
                player.input.keyPresses = new Input(
                        false, false, false, false,
                        keys.jump(), keys.shift(), keys.sprint());
            }
        });
    }

    private boolean hasGroundBelow(BlockPos pos) {
        for (int i = 0; i < 2; i++) {
            var state = minecraft.level.getBlockState(pos.below(i));
            if (!state.getCollisionShape(minecraft.level, pos.below(i)).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
