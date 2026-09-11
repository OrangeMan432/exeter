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
import net.minecraft.world.phys.Vec3;

/** Floating health + distance tags over players. */
public class Nametags extends ToggleableModule {

  private final NumberProperty<Double> range =
      new NumberProperty<Double>(64.0, 8.0, 256.0, "Range");
  private final NumberProperty<Double> scale = new NumberProperty<Double>(1.0, 0.5, 3.0, "Scale");
  private final Property<Boolean> health = new Property<Boolean>(true, "Health");
  private final Property<Boolean> distance = new Property<Boolean>(true, "Distance");

  public Nametags() {
    super("Nametags", new String[] {"nametags", "nametag"}, 0xFF00FF, ModuleType.RENDER);
    setDescription("Player info tags through walls.");
    offerProperties(range, scale, health, distance);
    this.listeners.add(
        new Listener<RenderWorldEvent>("nametags_render") {
          @Override
          public void call(RenderWorldEvent event) {
            Nametags.this.onRender(event);
          }
        });
  }

  private void onRender(RenderWorldEvent event) {
    if (minecraft.level == null || minecraft.player == null) return;
    for (Entity entity : minecraft.level.players()) {
      if (entity == minecraft.player || !entity.isAlive()) continue;
      if (!(entity instanceof Player player)) continue;
      double dist = minecraft.player.distanceTo(entity);
      if (dist > range.getValue()) continue;
      StringBuilder tag = new StringBuilder(player.getGameProfile().name());
      if (health.getValue()) {
        tag.append(" ")
            .append(String.format("%.0f", player.getHealth() + player.getAbsorptionAmount()));
      }
      if (distance.getValue()) {
        tag.append(" [").append(String.format("%.0f", dist)).append("m]");
      }
      Vec3 pos = entity.position().add(0, entity.getBbHeight() + 0.4, 0);
      Render3D.drawText(
          event.getSubmitNodeStorage(),
          event.getMatrixStack(),
          event.getCamera(),
          minecraft,
          pos,
          tag.toString(),
          0xFFFFFFFF,
          scale.getValue().floatValue());
    }
  }
}
