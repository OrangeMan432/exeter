package me.friendly.exeter.module.impl.toggle.world;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;

/**
 * AutoMine. Keeps mining the targeted block without holding the attack key — the classic
 * "auto-tool/auto-mine" for 5b5t grinder farms.
 *
 * Behavior: while the crosshair is on a block (and the attack key is held, or {@code Auto}
 * is on), the module drives the vanilla destroy path
 * ({@code startDestroyBlock} → {@code continueDestroyBlock}), which sends the correct
 * server-bound digging packets and respects server-side break progress. It re-attacks the
 * next block automatically after one breaks, with a configurable re-start delay.
 *
 * The swing is vanilla's own (gameMode handles it), so animations/anticheat dig timing match
 * a real player.
 */
public class AutoMine extends ToggleableModule {
    private final Property<Boolean> auto = new Property<>(true, "Auto", "auto", "a");
    private final NumberProperty<Integer> delay = new NumberProperty<>(0, 0, 20, "Delay Ticks", "delay", "d");
    private final Property<Boolean> requireTool = new Property<>(false, "Require Tool", "tool");

    private int cooldown = 0;
    private BlockPos lastTarget = null;

    public AutoMine() {
        super("AutoMine", new String[]{"automine", "miner"}, ModuleType.WORLD);
        offerProperties(auto, delay, requireTool);

        this.listeners.add(new Listener<TickEvent>("auto_mine_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer player = minecraft.player;
                if (player == null || minecraft.level == null || minecraft.gameMode == null) return;
                if (minecraft.gui.screen() != null) return;
                if (player.isUsingItem()) return;

                if (cooldown > 0) {
                    cooldown--;
                    return;
                }

                boolean trigger = auto.getValue() || minecraft.options.keyAttack.isDown();
                if (!trigger) {
                    lastTarget = null;
                    return;
                }

                if (!(minecraft.hitResult instanceof BlockHitResult hit)) return;
                BlockPos pos = hit.getBlockPos();
                if (minecraft.level.getBlockState(pos).isAir()) return;

                // New block: start destroy (vanilla sends the dig-start packet + swing).
                if (!pos.equals(lastTarget)) {
                    minecraft.gameMode.startDestroyBlock(pos, hit.getDirection());
                    lastTarget = pos;
                    cooldown = Math.max(0, delay.getValue());
                    return;
                }

                // Same block: continue (vanilla tracks progress + sends dig packets).
                if (!minecraft.level.getBlockState(pos).isAir()) {
                    minecraft.gameMode.continueDestroyBlock(pos, hit.getDirection());
                }
            }
        });
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        lastTarget = null;
        if (minecraft.gameMode != null) {
            minecraft.gameMode.stopDestroyBlock();
        }
    }
}
