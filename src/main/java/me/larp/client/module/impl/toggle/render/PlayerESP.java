package me.larp.client.module.impl.toggle.render;

import me.larp.api.event.Listener;
import me.larp.client.events.RenderWorldEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import me.larp.client.util.Render3D;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** Boxes players through walls with friend coloring. */
public class PlayerESP extends ToggleableModule {

  private final NumberProperty<Double> range =
      new NumberProperty<Double>(64.0, 8.0, 256.0, "Range");
  private final NumberProperty<Double> lineWidth =
      new NumberProperty<Double>(2.0, 1.0, 5.0, "Line Width");
  private final Property<Boolean> friends =
      new Property<Boolean>(true, "Show Friends");

  public PlayerESP() {
    super("PlayerESP", new String[] {"playeresp", "player-esp", "esp"}, 0xFF00FF, ModuleType.RENDER);
    setDescription("Boxes players through walls.");
    offerProperties(range, lineWidth, friends);
    this.listeners.add(
        new Listener<RenderWorldEvent>("playeresp_render") {
          @Override
          public void call(RenderWorldEvent event) {
            PlayerESP.this.onRender(event);
          }
        });
  }

  private void onRender(RenderWorldEvent event) {
    if (minecraft.level == null || minecraft.player == null) return;
    for (Entity entity : minecraft.level.players()) {
      if (!(entity instanceof Player player) || entity == minecraft.player) continue;
      if (!entity.isAlive()) continue;
      if (minecraft.player.distanceTo(entity) > range.getValue()) continue;
      boolean friend =
          me.larp.client.core.Larp.getInstance()
              .getFriendManager()
              .isFriend(player.getName().getString());
      if (friend && !friends.getValue()) continue;
      int color = friend ? 0xFF55FF55 : 0xFFFF5555;
      Render3D.drawBoxOutline(
          event.getSubmitNodeStorage(),
          event.getCamera(),
          event.getMatrixStack(),
          entity.getBoundingBox(),
          color,
          lineWidth.getValue().floatValue());
    }
  }
}
