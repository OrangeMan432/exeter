package me.friendly.exeter.module.impl.toggle.misc;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * FastEat. Shortens consume time for food (gapple spam in crystal fights).
 *
 * 26.2 mechanics: the consume countdown lives in the protected
 * {@code LivingEntity.useItemRemaining} field; there is no public setter, and the field is
 * decremented in {@code LivingEntity.tick}. A Mixin accessor is the clean hook — but this base
 * already ships {@code MixinClientPlayer} on LocalPlayer, so rather than grow the mixin surface
 * the module uses the public API that exists: when the remaining count passes the threshold,
 * call {@code releaseUsingItem()} once — vanilla then treats the item as consumed early
 * (the same path a manual early-release takes), which on lenient 5b5t anticheats still grants
 * the food effects.
 *
 * Trigger threshold: when remaining ticks &lt;= threshold, release. Threshold 0 = vanilla.
 */
public class FastEat extends ToggleableModule {
    private final NumberProperty<Integer> releaseAfterTicks = new NumberProperty<>(6, 1, 30, "Release After Ticks", "ticks", "t");

    private boolean releasedThisUse = false;

    public FastEat() {
        super("FastEat", new String[]{"fasteat", "fastconsume"}, ModuleType.MISCELLANEOUS);
        offerProperties(releaseAfterTicks);

        this.listeners.add(new Listener<TickEvent>("fast_eat_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer player = minecraft.player;
                if (player == null) return;

                if (!player.isUsingItem()) {
                    releasedThisUse = false;
                    return;
                }

                ItemStack use = player.getUseItem();
                if (use.get(DataComponents.FOOD) == null) return; // food only

                if (releasedThisUse) return;
                if (player.getUseItemRemainingTicks() <= releaseAfterTicks.getValue()) {
                    player.releaseUsingItem();
                    releasedThisUse = true;
                }
            }
        });
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        releasedThisUse = false;
    }
}
