package me.larp.client.module.impl.toggle.misc;

import java.util.HashSet;
import java.util.Set;
import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.logging.Logger;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Client-side alerts when players enter visual range. */
public class VisualRange extends ToggleableModule {

  private final NumberProperty<Double> range =
      new NumberProperty<Double>(64.0, 8.0, 256.0, "Range");
  private final Property<Boolean> leaving =
      new Property<Boolean>(true, "Leaving");

  private final Set<String> known = new HashSet<>();

  public VisualRange() {
    super("VisualRange", new String[] {"visualrange", "vr"}, 0x00FFFF,
        ModuleType.MISCELLANEOUS);
    setDescription("Alerts when players enter render distance.");
    offerProperties(range, leaving);
    this.listeners.add(
        new Listener<TickEvent>("visualrange_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            VisualRange.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    known.clear();
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    Set<String> now = new HashSet<>();
    for (Entity entity : minecraft.level.players()) {
      if (!(entity instanceof Player player) || entity == minecraft.player) continue;
      if (minecraft.player.distanceTo(entity) > range.getValue()) continue;
      String name = player.getGameProfile().name();
      now.add(name);
      if (!known.contains(name)) {
        Logger.getLogger().printToChat("&a" + name + " &7entered visual range.");
      }
    }
    if (leaving.getValue()) {
      for (String name : known) {
        if (!now.contains(name)) {
          Logger.getLogger().printToChat("&c" + name + " &7left visual range.");
        }
      }
    }
    known.clear();
    known.addAll(now);
  }
}
