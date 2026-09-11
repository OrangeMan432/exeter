package me.larp.client.module.impl.toggle.misc;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.PacketEvent;
import me.larp.client.events.RenderWorldEvent;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import me.larp.client.util.PlayerUtil;
import me.larp.client.util.Render3D;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Shoreline-pattern ChorusControl: holds the chorus teleport server-side,
 * shows the destination box, and releases it on disable. Eat a chorus fruit
 * to arm it.
 */
public class ChorusControl extends ToggleableModule {

  private final NumberProperty<Double> lineWidth =
      new NumberProperty<Double>(2.0, 1.0, 5.0, "Line Width");
  private final Property<Boolean> render =
      new Property<Boolean>(true, "Render");

  private boolean holding;
  private ClientboundPlayerPositionPacket held;
  private int heldId = -1;

  public ChorusControl() {
    super("ChorusControl", new String[] {"choruscontrol", "chorus"}, 0xFF8800,
        ModuleType.MISCELLANEOUS);
    setDescription("Suspends chorus teleports until released.");
    offerProperties(lineWidth, render);
    this.listeners.add(
        new Listener<TickEvent>("choruscontrol_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            ChorusControl.this.onTick();
          }
        });
    this.listeners.add(
        new Listener<PacketEvent>("choruscontrol_packet") {
          @Override
          public void call(PacketEvent event) {
            ChorusControl.this.onPacket(event);
          }
        });
    this.listeners.add(
        new Listener<RenderWorldEvent>("choruscontrol_render") {
          @Override
          public void call(RenderWorldEvent event) {
            ChorusControl.this.onRender(event);
          }
        });
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    // Release the held teleport: apply locally and confirm to the server.
    if (held != null && minecraft.player != null && minecraft.getConnection() != null) {
      Vec3 pos = held.change().position();
      minecraft.player.setPos(pos.x, pos.y, pos.z);
      minecraft.getConnection().send(new ServerboundAcceptTeleportationPacket(heldId));
    }
    held = null;
    heldId = -1;
    holding = false;
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    if (!holding && minecraft.player.isUsingItem()) {
      var held = minecraft.player.getUseItem();
      if (!held.isEmpty()
          && held.getItem() == Items.CHORUS_FRUIT
          && held.get(DataComponents.FOOD) != null) {
        holding = true;
      }
    }
    setTag(holding ? "ChorusControl [HELD]" : "ChorusControl");
  }

  private void onPacket(PacketEvent event) {
    if (!holding) return;
    if (event.getPacket() instanceof ServerboundMovePlayerPacket) {
      // Freeze client position updates while the teleport is suspended.
      event.setCanceled(true);
      return;
    }
    if (event.getPacket() instanceof ClientboundPlayerPositionPacket packet) {
      event.setCanceled(true);
      held = packet;
      heldId = packet.id();
    }
  }

  private void onRender(RenderWorldEvent event) {
    if (!render.getValue() || held == null) return;
    if (minecraft.player == null) return;
    Vec3 pos = held.change().position();
    Render3D.drawBoxOutline(
        event.getSubmitNodeStorage(),
        event.getCamera(),
        event.getMatrixStack(),
        new AABB(
            pos.x - 0.3, pos.y, pos.z - 0.3,
            pos.x + 0.3, pos.y + 1.8, pos.z + 0.3),
        0xFFFFFF00,
          lineWidth.getValue().floatValue());
  }
}
