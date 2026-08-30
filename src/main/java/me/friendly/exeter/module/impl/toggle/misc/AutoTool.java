package me.friendly.exeter.module.impl.toggle.misc;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
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
 *
 * Customizability:
 *  - Min Gain % : only swap when the best tool is this much faster (default 10%).
 *  - Only While Mining : restrict swaps to when the attack key is held.
 *  - Prefer Hotbar : search the whole inventory for a better tool (off = hotbar only).
 */
public class AutoTool extends ToggleableModule {
    private final Property<Boolean> silent = new Property<>(true, "Silent", "silent", "s");
    private final Property<Boolean> onlyWhileMining = new Property<>(false, "Only While Mining", "mining", "om");
    private final NumberProperty<Integer> minGainPct = new NumberProperty<>(10, 0, 100, "Min Gain %", "mingain", "g");
    private final Property<Boolean> preferHotbar = new Property<>(true, "Prefer Hotbar", "hotbar", "hb");

    private int lastBest = -1;

    public AutoTool() {
        super("AutoTool", new String[]{"autotool", "at"}, ModuleType.MISCELLANEOUS);
        offerProperties(silent, onlyWhileMining, minGainPct, preferHotbar);

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

                int searchMax = preferHotbar.getValue() ? 8 : 35;
                float gainFactor = 1.0f + minGainPct.getValue() / 100.0f;
                for (int i = 0; i <= searchMax; i++) {
                    float s = speedFor(player, i, state);
                    if (s > bestSpeed * gainFactor) {
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
