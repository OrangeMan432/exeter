package me.larp.client.module.impl.toggle.combat;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

/**
 * Lemon-pattern CevBlocker lite: breaks enemy crystals crowding your own
 * head before they can cev or drop you.
 */
public class CrystalGuard extends ToggleableModule {

  private final NumberProperty<Double> range =
      new NumberProperty<Double>(4.0, 1.0, 6.0, "Range");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");

  public CrystalGuard() {
    super("CrystalGuard", new String[] {"crystalguard", "cevblocker"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Breaks crystals placed on you.");
    offerProperties(range, swingHand);
    this.listeners.add(
        new Listener<TickEvent>("crystalguard_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            CrystalGuard.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;

    for (Entity entity : minecraft.level.entitiesForRendering()) {
      if (!(entity instanceof EndCrystal crystal) || !crystal.isAlive()) continue;
      // Only crystals within range of YOUR body, not the enemy's.
      if (minecraft.player.distanceTo(crystal) > range.getValue()) continue;
      if (crystal.getY() < minecraft.player.getY() - 1.0) continue;
      minecraft.gameMode.attack(minecraft.player, crystal);
      if (swingHand.getValue()) {
        minecraft.player.swing(InteractionHand.MAIN_HAND);
      }
      setTag("CrystalGuard [hit]");
      return;
    }
    setTag("CrystalGuard");
  }
}
