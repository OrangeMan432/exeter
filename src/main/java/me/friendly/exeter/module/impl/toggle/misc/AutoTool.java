package me.friendly.exeter.module.impl.toggle.misc;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;

/**
 * AutoTool. Swaps to the fastest tool in the hotbar for the block you're looking at/mining.
 *
 * Speed metric: the item's destroy-speed for the targeted state
 * ({@code ItemStack.getDestroySpeed(BlockState)}) — the same value vanilla's break progress
 * uses, so "fastest" matches actual break time. Swaps only when the best slot differs from the
 * selected one and the speed gain is meaningful (&gt;10%).
 *
 * Switch mode: silent (server-side {@code ServerboundSetCarriedItemPacket} via PlayerUtil, view
 * untouched) or real (selected slot changes, matches what vanilla swap does).
 */
public class AutoTool extends ToggleableModule {
    private final Property<Boolean> silent = new Property<>(true, "Silent", "silent", "s");
    private final Property<Boolean> onlyWhileMining = new Property<>(false, "Only While Mining", "mining", "om");

    private int lastBest = -1;

    public AutoTool() {
        super("AutoTool", new String[]{"autotool", "at"}, ModuleType.MISCELLANEOUS);
        offerProperties(silent, onlyWhileMining);

        this.listeners.add(new Listener<TickEvent>("auto_tool_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer player = minecraft.player;
                if (player == null || minecraft.level == null) return;
                if (minecraft.gui.screen() != null) return;

                if (onlyWhileMining.getValue() && !minecraft.options.keyAttack.isDown()) {
                    lastBest = -1;
                    return;
                }

                if (!(minecraft.hitResult instanceof BlockHitResult hit)) return;
                BlockPos pos = hit.getBlockPos();
                var state = minecraft.level.getBlockState(pos);
                if (state.isAir()) return;

                int selected = player.getInventory().getSelectedSlot();
                int best = selected;
                float bestSpeed = speedFor(player, selected, state);

                for (int i = 0; i <= 8; i++) {
                    float s = speedFor(player, i, state);
                    if (s > bestSpeed * 1.1f) { // >10% gain only
                        bestSpeed = s;
                        best = i;
                    }
                }

                if (best == lastBest) return;
                lastBest = best;

                if (best != selected) {
                    if (silent.getValue()) {
                        me.friendly.exeter.util.PlayerUtil.swapTo(best);
                        // Silent swap is a server-side packet; sync the client selection too so
                        // break-progress math uses the right tool.
                        player.getInventory().setSelectedSlot(best);
                    } else {
                        player.getInventory().setSelectedSlot(best);
                    }
                }
            }
        });
    }

    private static float speedFor(LocalPlayer player, int slot, net.minecraft.world.level.block.state.BlockState state) {
        var stack = player.getInventory().getItem(slot);
        if (stack.isEmpty()) return 1.0f;
        float speed = stack.getDestroySpeed(state);
        return Math.max(speed, 1.0f);
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        lastBest = -1;
    }
}
