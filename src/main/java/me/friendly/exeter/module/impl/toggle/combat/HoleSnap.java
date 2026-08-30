package me.friendly.exeter.module.impl.toggle.combat;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * HoleSnap. Detects the nearest safe hole (1x1 or 1x2, 3-block depth) and snaps you into it —
 * the core defensive positioning of the 5b5t crystal meta (a hole negates most crystal damage).
 *
 * Logic per tick:
 *  - Scan radius (default 4) for a valid hole: column of air 1x1 (or 1x2 with Wide), floor
 *    solid at depth ≥ 2 (so crystals placed at floor level can't hit your feet), headroom ≥ 2.
 *  - Snap modes:
 *      Teleport : instant setPos into the hole center (risky — server may rubber-band >8 blocks).
 *      Motion   : strong velocity pull toward the hole center (default — smooth, server-safe).
 *      Strict   : only pulls while you're within snap range of the hole rim.
 *  - Center: once inside, gently pins you to the hole center (same pull as Surround).
 *
 * Safety: skips holes occupied by other players, skips while using items/screens open,
 * never triggers while elytra-flying (ElytraFly owns that state).
 */
public class HoleSnap extends ToggleableModule {
    private enum Mode { MOTION, TELEPORT, STRICT }

    private final EnumProperty<Mode> mode = new EnumProperty<>(Mode.MOTION, "Mode", "m");
    private final NumberProperty<Double> scanRadius = new NumberProperty<>(4.0, 1.0, 8.0, "Scan Radius", "radius", "r");
    private final NumberProperty<Double> snapSpeed = new NumberProperty<>(0.6, 0.1, 2.0, "Snap Speed", "speed", "s");
    private final Property<Boolean> wideHoles = new Property<>(false, "Wide Holes (1x2)", "wide", "w");
    private final Property<Boolean> skipOccupied = new Property<>(true, "Skip Occupied", "skipoccupied", "so");
    private final Property<Boolean> centerPin = new Property<>(true, "Center Pin", "center", "c");
    private final NumberProperty<Integer> minDepth = new NumberProperty<>(2, 1, 4, "Min Depth", "depth", "d");

    public HoleSnap() {
        super("HoleSnap", new String[]{"holesnap", "hole", "hs"}, ModuleType.COMBAT);
        offerProperties(mode, scanRadius, snapSpeed, wideHoles, skipOccupied, centerPin, minDepth);

        this.listeners.add(new Listener<TickEvent>("hole_snap_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer player = minecraft.player;
                if (player == null || minecraft.level == null) return;
                if (minecraft.gui.screen() != null || player.isUsingItem() || player.isDeadOrDying()) return;
                if (player.isFallFlying()) return;

                BlockPos feet = player.blockPosition();

                // Already in a hole? Pin to center and stop.
                if (isHole(feet, player)) {
                    if (centerPin.getValue()) {
                        double cx = feet.getX() + 0.5;
                        double cz = feet.getZ() + 0.5;
                        double dx = cx - player.getX();
                        double dz = cz - player.getZ();
                        if (Math.abs(dx) > 0.02 || Math.abs(dz) > 0.02) {
                            player.setDeltaMovement(dx * 0.3, player.getDeltaMovement().y, dz * 0.3);
                        }
                    }
                    return;
                }

                BlockPos hole = findNearestHole(player, feet);
                if (hole == null) return;

                double cx = hole.getX() + 0.5;
                double cz = hole.getZ() + 0.5;
                double dx = cx - player.getX();
                double dz = cz - player.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);

                if (mode.getValue() == Mode.TELEPORT && dist <= 6.0) {
                    player.setPos(cx, hole.getY(), cz);
                    player.setDeltaMovement(0, 0, 0);
                    return;
                }

                // MOTION / STRICT: velocity pull toward hole center.
                if (mode.getValue() == Mode.STRICT && dist > scanRadius.getValue()) return;
                double s = snapSpeed.getValue();
                double nx = dx / Math.max(dist, 0.001);
                double nz = dz / Math.max(dist, 0.001);
                player.setDeltaMovement(nx * s, player.getDeltaMovement().y, nz * s);
            }
        });
    }

    /** A valid hole: air column at feet+head, solid floor, walls on all 4 sides, depth ≥ min. */
    private boolean isHole(BlockPos feet, LocalPlayer player) {
        var level = minecraft.level;
        // Floor must be solid and deep enough (crystal at floor level shouldn't reach feet).
        int depth = 0;
        BlockPos below = feet.below();
        for (int i = 0; i < minDepth.getValue(); i++) {
            if (level.getBlockState(below.below(i)).isAir()) return false;
            depth++;
        }
        if (depth < minDepth.getValue()) return false;
        // Headroom.
        if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()) return false;
        // Walls (all 4 sides solid at feet level).
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(feet.relative(d)).isAir()) {
                // Wide holes allow one open side if the neighbour cell is also a hole.
                if (wideHoles.getValue() && isHole(feet.relative(d), player)) continue;
                return false;
            }
        }
        return true;
    }

    private BlockPos findNearestHole(LocalPlayer player, BlockPos from) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        int r = (int) Math.ceil(scanRadius.getValue());
        for (BlockPos pos : BlockPos.betweenClosed(
                from.offset(-r, -2, -r), from.offset(r, 1, r))) {
            if (!isHole(pos.immutable(), player)) continue;
            if (skipOccupied.getValue() && occupied(pos, player)) continue;
            double d = distXZ(player.position(), pos);
            if (d < bestDist) {
                bestDist = d;
                best = pos.immutable();
            }
        }
        return best;
    }

    private boolean occupied(BlockPos pos, LocalPlayer self) {
        for (var p : minecraft.level.players()) {
            if (p == self || !p.isAlive()) continue;
            if (p.blockPosition().equals(pos)) return true;
        }
        return false;
    }

    private static double distXZ(Vec3 pos, BlockPos block) {
        double dx = block.getX() + 0.5 - pos.x;
        double dz = block.getZ() + 0.5 - pos.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
