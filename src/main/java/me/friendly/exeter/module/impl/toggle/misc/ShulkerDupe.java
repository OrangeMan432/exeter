package me.friendly.exeter.module.impl.toggle.misc;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.ContainerInput;

/**
 * Shulker dupe, ported from Lemon Client's ShulkerDupe module.
 *
 * <p>Mechanics: when a shulker (or any container) screen is open, a single PICKUP click on slot 0
 * is sent; the follow-up click (triggered when that click packet leaves) on every shulker slot is
 * what performs the duplication. This relies on the classic container-desync dupe and only works on
 * servers running vanilla/forge/fabric <= 1.19 (per Lemon's own description). On 1.21+ it will
 * simply click slots and do nothing useful.
 */
public class ShulkerDupe extends ToggleableModule {
  private final Property<Boolean> allSlots = new Property<>(false, "All Slots", "all", "allslots");
  private final Property<Boolean> auto = new Property<>(false, "Auto", "auto");

  private boolean shouldDupe = false;
  private boolean shouldDupeAll = false;
  private boolean screenOpen = false;

  public ShulkerDupe() {
    super("ShulkerDupe", new String[] {"shulkerdupe", "sdupe"}, ModuleType.MISCELLANEOUS);
    offerProperties(allSlots, auto);

    this.listeners.add(
        new Listener<TickEvent>("shulker_dupe_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            if (minecraft.player == null) return;

            boolean open = minecraft.gui.screen() instanceof AbstractContainerScreen;
            if (open && !screenOpen) {
              screenOpen = true;
              if (auto.getValue()) {
                if (allSlots.getValue()) shouldDupeAll = true;
                else shouldDupe = true;
              }
            } else if (!open) {
              screenOpen = false;
            }

            if (shouldDupe || shouldDupeAll) {
              minecraft.player.containerMenu.clicked(0, 0, ContainerInput.PICKUP, minecraft.player);
            }
          }
        });

    this.listeners.add(
        new Listener<PacketEvent>("shulker_dupe_packet") {
          @Override
          public void call(PacketEvent event) {
            if (!(event.getPacket() instanceof ServerboundContainerClickPacket)) return;
            if (minecraft.player == null) return;

            if (shouldDupeAll) {
              for (int i = 0; i < 27; i++) {
                minecraft.player.containerMenu.clicked(
                    i, 0, ContainerInput.PICKUP, minecraft.player);
              }
              shouldDupeAll = false;
            } else if (shouldDupe) {
              minecraft.player.containerMenu.clicked(0, 0, ContainerInput.PICKUP, minecraft.player);
              shouldDupe = false;
            }
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    shouldDupe = false;
    shouldDupeAll = false;
    screenOpen = false;
    if (minecraft.player != null && minecraft.gui.screen() instanceof AbstractContainerScreen) {
      if (allSlots.getValue()) shouldDupeAll = true;
      else shouldDupe = true;
    }
  }
}
