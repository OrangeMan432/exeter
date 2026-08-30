package me.friendly.exeter.module.impl.toggle.world;

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
import net.minecraft.core.Holder;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Scaffold. Auto-bridges: places a block under your feet as you walk backwards off an edge.
 *
 * The 5b5t bridging workhorse. Logic:
 *  - Target position: the block below-and-behind the player's next-step footprint.
 *  - Slot: first hotbar BlockItem (Tower mode prefers the selected slot to avoid swap flicker).
 *  - Placement: vanilla {@code useItemOn} on the face pointing at the target — server-validated,
 *    so rotation spoofing is optional ({@code Silent Aim} reuses PlayerUtil's with-rotation helper
 *    to face the block for the placement packet, then restores the view).
 *  - Tower: hold jump while bridging to build straight up (place under feet each jump apex).
 *
 * Safety: never places into your own bounding box, never places in air with no adjacent face,
 * respects the AntiCrystal place-timeout if that module is running.
 */
public class Scaffold extends ToggleableModule {
    private enum Mode { NORMAL, TOWER }

    private final EnumProperty<Mode> mode = new EnumProperty<>(Mode.NORMAL, "Mode", "m");
    private final Property<Boolean> silentAim = new Property<>(true, "Silent Aim", "silentaim", "sa");
    private final Property<Boolean> tower = new Property<>(true, "Tower", "tower", "t");
    private final NumberProperty<Double> range = new NumberProperty<>(4.5, 1.0, 6.0, "Range", "range", "r");

    public Scaffold() {
        super("Scaffold", new String[]{"scaffold", "safewalkbridge", "bridge"}, ModuleType.WORLD);
        offerProperties(mode, silentAim, tower, range);

        this.listeners.add(new Listener<TickEvent>("scaffold_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer player = minecraft.player;
                if (player == null || minecraft.level == null || minecraft.gameMode == null) return;
                if (minecraft.gui.screen() != null) return;
                if (player.isUsingItem()) return;

                // Tower: keep hopping to build vertically.
                if (tower.getValue() && mode.getValue() == Mode.TOWER
                        && player.onGround()
                        && player.input != null && player.input.keyPresses != null
                        && player.input.keyPresses.forward()) {
                    player.jumpFromGround();
                }

                // Target: the block under the position we're about to move into.
                Vec3 vel = player.getDeltaMovement();
                double nx = player.getX() + vel.x * 1.2;
                double nz = player.getZ() + vel.z * 1.2;
                BlockPos target = BlockPos.containing(
                        nx, player.getY() - (tower.getValue() && mode.getValue() == Mode.TOWER && !player.onGround() ? 1.0 : 0.5) - 0.5, nz);

                if (!needsBlock(target)) return;
                if (!PlayerUtil.inRange(target, range.getValue())) return;

                int slot = findBlockSlot(player);
                if (slot == -1) return;

                int originalSlot = player.getInventory().getSelectedSlot();
                boolean swapped = slot != originalSlot;
                if (swapped) PlayerUtil.swapTo(slot);

                Direction face = bestFace(target);
                if (face == null) {
                    if (swapped) PlayerUtil.swapBack();
                    return;
                }

                if (silentAim.getValue()) {
                    double yaw = PlayerUtil.getYaw(target);
                    double pitch = PlayerUtil.getPitch(target);
                    PlayerUtil.withRotation(yaw, pitch, () -> place(target, face));
                } else {
                    place(target, face);
                }

                if (swapped) PlayerUtil.swapBack();
            }
        });
    }

    private boolean needsBlock(BlockPos pos) {
        var state = minecraft.level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    private int findBlockSlot(LocalPlayer player) {
        for (int i = 0; i <= 8; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                return i;
            }
        }
        return -1;
    }

    /** Picks a face of an adjacent solid block that points toward the target. */
    private Direction bestFace(BlockPos target) {
        for (Direction d : Direction.values()) {
            BlockPos adj = target.relative(d);
            var state = minecraft.level.getBlockState(adj);
            if (state.isAir() || state.canBeReplaced()) continue;
            if (!state.getCollisionShape(minecraft.level, adj).isEmpty()) {
                return d.getOpposite();
            }
        }
        return null;
    }

    private void place(BlockPos target, Direction face) {
        PlayerUtil.useItemOn(target, face);
        PlayerUtil.swingHand();
    }
}
