package me.friendly.exeter.module.impl.toggle.world;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * FastPlace. Removes the 4-tick right-click hold delay when placing blocks / using items.
 *
 * Vanilla gates repeat use on a 4-tick {@code rightClickDelay}. This module re-triggers the
 * vanilla use path every tick while the use key is held, so crystal/block placement happens
 * at up to 20 CPS instead of 5 — the core of fast crystal placement on 5b5t.
 *
 * 26.2 note: {@code Minecraft.startUseItem} is private, so the module calls the same public
 * path vanilla's {@code handleKeybinds} uses: {@code gameMode.useItemOn} for the targeted
 * block and {@code gameMode.useItem} otherwise, mirroring the vanilla branch. Cooldowns are
 * checked through the ItemStack overload ({@code ItemCooldowns.isOnCooldown(ItemStack)}).
 *
 * Blocks Only: when on, only fires while a block is held (main or offhand) — bows, pearls,
 * ender eyes and food keep their vanilla single-fire behavior.
 */
public class FastPlace extends ToggleableModule {
    private final Property<Boolean> blocksOnly = new Property<>(true, "Blocks Only", "blocksonly", "bo");

    public FastPlace() {
        super("FastPlace", new String[]{"fastplace", "fastplaceblocks"}, ModuleType.WORLD);
        offerProperties(blocksOnly);

        this.listeners.add(new Listener<TickEvent>("fast_place_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer player = minecraft.player;
                if (player == null || minecraft.level == null || minecraft.gameMode == null) return;
                if (minecraft.gui.screen() != null) return;
                if (!minecraft.options.keyUse.isDown()) return;

                ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
                if (player.getCooldowns().isOnCooldown(main)) return;

                if (blocksOnly.getValue()) {
                    ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
                    boolean mainBlock = main.getItem() instanceof BlockItem;
                    boolean offBlock = off.getItem() instanceof BlockItem;
                    if (!mainBlock && !offBlock) return;
                }

                if (minecraft.hitResult instanceof BlockHitResult hit
                        && hit.getType() != HitResult.Type.MISS) {
                    minecraft.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
                } else {
                    minecraft.gameMode.useItem(player, InteractionHand.MAIN_HAND);
                }
                player.swing(InteractionHand.MAIN_HAND);
            }
        });
    }
}
