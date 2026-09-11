package me.larp.client.module.impl.toggle.combat;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.core.Larp;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Lemon-pattern SelfProtect: keeps Surround running while enemies are close, releases it when the
 * area is clear.
 */
public class SelfProtect extends ToggleableModule {

  private final NumberProperty<Double> enemyRange =
      new NumberProperty<Double>(8.0, 1.0, 16.0, "Enemy Range");
  private final Property<Boolean> release = new Property<Boolean>(true, "Release When Clear");

  public SelfProtect() {
    super("SelfProtect", new String[] {"selfprotect", "self-protect"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Auto-runs Surround when enemies approach.");
    offerProperties(enemyRange, release);
    this.listeners.add(
        new Listener<TickEvent>("selfprotect_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            SelfProtect.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    if (minecraft.player.isDeadOrDying()) return;

    Surround surround = Larp.getInstance().getModuleManager().getModule(Surround.class);
    if (surround == null) return;

    boolean threatened = false;
    boolean clear = true;
    for (Entity entity : minecraft.level.players()) {
      if (entity == minecraft.player || !entity.isAlive()) continue;
      if (!(entity instanceof Player)) continue;
      double dist = minecraft.player.distanceTo(entity);
      if (dist <= enemyRange.getValue()) {
        threatened = true;
        break;
      }
      // Hysteresis: only release once NObODY is within range + 3.
      if (dist <= enemyRange.getValue() + 3.0) {
        clear = false;
      }
    }

    if (threatened && !surround.isRunning()) {
      surround.setRunning(true);
    } else if (!threatened && clear && release.getValue() && surround.isRunning()) {
      surround.setRunning(false);
    }
  }
}
