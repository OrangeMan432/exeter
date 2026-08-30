package me.friendly.exeter.module.impl.toggle.misc;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WebBlock;

/**
 * NoSlow. Cancels movement slowdown from cobwebs / soul sand / honey / powder snow / berries.
 *
 * Implementation: each tick, if the player is inside (or touching) a slowdown block, restore
 * the horizontal delta to the last unslowed value (capped below anticheat max-delta thresholds).
 * Delta rewrite, same mechanism as SpeedPlus — server still validates position, so this is a
 * prediction override, not a packet spoof.
 */
public class NoSlow extends ToggleableModule {
    private final Property<Boolean> cobweb = new Property<>(true, "Cobweb", "web", "cobweb");
    private final Property<Boolean> soulSand = new Property<>(true, "Soul Sand", "soulsand");
    private final Property<Boolean> honey = new Property<>(true, "Honey", "honey");
    private final Property<Boolean> powderSnow = new Property<>(true, "Powder Snow", "powdersnow");
    private final Property<Boolean> berries = new Property<>(true, "Sweet Berries", "berries");

    private double lastFreeX, lastFreeZ;
    private boolean hadFreeSpeed;

    public NoSlow() {
        super("NoSlow", new String[]{"noslow", "noslowdown"}, ModuleType.MISCELLANEOUS);
        offerProperties(cobweb, soulSand, honey, powderSnow, berries);

        this.listeners.add(new Listener<TickEvent>("no_slow_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer player = minecraft.player;
                if (player == null || minecraft.level == null) return;

                if (!isSlowed(player)) {
                    var vel = player.getDeltaMovement();
                    lastFreeX = vel.x;
                    lastFreeZ = vel.z;
                    hadFreeSpeed = Math.abs(vel.x) > 0.02 || Math.abs(vel.z) > 0.02;
                    return;
                }

                if (hadFreeSpeed) {
                    // Keep restored delta conservative — 5b5t movement checks flag large
                    // per-tick deltas; 0.2873 ≈ vanilla sprint speed per tick.
                    double cap = 0.2873;
                    player.setDeltaMovement(
                            clamp(lastFreeX, cap),
                            player.getDeltaMovement().y,
                            clamp(lastFreeZ, cap));
                }
            }
        });
    }

    private static double clamp(double v, double cap) {
        return Math.max(-cap, Math.min(cap, v));
    }

    private boolean isSlowed(LocalPlayer player) {
        BlockPos base = player.blockPosition();
        BlockPos[] checks = {
                base, base.above(),
                base.relative(player.getDirection().getOpposite())
        };
        for (BlockPos pos : checks) {
            var state = minecraft.level.getBlockState(pos);
            if (state.isAir()) continue;
            var block = state.getBlock();
            if (cobweb.getValue() && block instanceof WebBlock) return true;
            if (soulSand.getValue() && block == Blocks.SOUL_SAND) return true;
            if (honey.getValue() && block == Blocks.HONEY_BLOCK) return true;
            if (powderSnow.getValue() && block == Blocks.POWDER_SNOW) return true;
            if (berries.getValue() && block == Blocks.SWEET_BERRY_BUSH) return true;
        }
        return false;
    }
}
