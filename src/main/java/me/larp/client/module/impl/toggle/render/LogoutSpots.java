package me.larp.client.module.impl.toggle.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.RenderWorldEvent;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import me.larp.client.util.Render3D;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Remembers where players logged out and marks the spot. */
public class LogoutSpots extends ToggleableModule {

  private final NumberProperty<Double> lineWidth =
      new NumberProperty<Double>(2.0, 1.0, 5.0, "Line Width");
  private final NumberProperty<Double> scale =
      new NumberProperty<Double>(1.0, 0.5, 3.0, "Scale");
  private final Property<Boolean> clearOnReturn =
      new Property<Boolean>(true, "Clear On Return");

  private final Map<String, BlockPos> spots = new HashMap<>();
  private final List<String> seen = new ArrayList<>();

  public LogoutSpots() {
    super("LogoutSpots", new String[] {"logoutspots", "logout"}, 0xFF00FF, ModuleType.RENDER);
    setDescription("Marks where players disconnect.");
    offerProperties(lineWidth, scale, clearOnReturn);
    this.listeners.add(
        new Listener<RenderWorldEvent>("logoutspots_render") {
          @Override
          public void call(RenderWorldEvent event) {
            LogoutSpots.this.onRender(event);
          }
        });
    this.listeners.add(
        new Listener<TickEvent>("logoutspots_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            LogoutSpots.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    seen.clear();
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    List<String> now = new ArrayList<>();
    for (Entity entity : minecraft.level.players()) {
      if (!(entity instanceof Player player) || entity == minecraft.player) continue;
      String name = player.getGameProfile().name();
      now.add(name);
      if (clearOnReturn.getValue() && spots.containsKey(name)) {
        spots.remove(name);
      }
    }
    for (String name : seen) {
      if (!now.contains(name) && !spots.containsKey(name)) {
        // Player vanished: record last server position is unavailable post-hoc,
        // so mark only if we tracked them — positions refresh while visible.
        BlockPos last = lastKnown.get(name);
        if (last != null) {
          spots.put(name, last);
        }
      }
    }
    seen.clear();
    seen.addAll(now);
  }

  private final Map<String, BlockPos> lastKnown = new HashMap<>();

  private void onRender(RenderWorldEvent event) {
    if (minecraft.level == null || minecraft.player == null) return;
    // Refresh known positions of visible players every frame (cheap map puts).
    for (Entity entity : minecraft.level.players()) {
      if (!(entity instanceof Player player) || entity == minecraft.player) continue;
      lastKnown.put(player.getGameProfile().name(), player.blockPosition());
    }
    for (Map.Entry<String, BlockPos> entry : spots.entrySet()) {
      BlockPos pos = entry.getValue();
      Render3D.drawBoxOutline(
          event.getSubmitNodeStorage(),
          event.getCamera(),
          event.getMatrixStack(),
          new AABB(pos).expandTowards(0, 1, 0),
          0xFFFF5555,
          lineWidth.getValue().floatValue());
      Render3D.drawText(
          event.getSubmitNodeStorage(),
          event.getMatrixStack(),
          event.getCamera(),
          minecraft,
          new Vec3(pos.getX() + 0.5, pos.getY() + 2.5, pos.getZ() + 0.5),
          entry.getKey() + " logged out",
          0xFFFF5555,
          scale.getValue().floatValue());
    }
  }
}
