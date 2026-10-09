package me.friendly.exeter.module.impl.toggle.render;

import me.friendly.api.event.Listener;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.WorldRenderEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.render.EspRenderManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Burrow ESP ported from Lemon's BurrowESP: boxes any player standing inside a solid block, colored
 * by self, friend or enemy.
 */
public class BurrowESP extends ToggleableModule {

  private final Property<Boolean> showSelf = new Property<Boolean>(true, "Self");
  private final Property<Boolean> showFriends = new Property<Boolean>(true, "Friends", "friend");
  private final Property<Boolean> showEnemies = new Property<Boolean>(true, "Enemies", "enemy");
  private final NumberProperty<Float> lineWidth =
      new NumberProperty<Float>(2.0f, 0.5f, 5.0f, "Line Width");
  private final Property<Boolean> useCustomAlpha =
      new Property<Boolean>(false, "Custom Alpha", "CustomAlpha");
  private final NumberProperty<Float> fillAlpha =
      new NumberProperty<Float>(120f, 0f, 255f, "Fill Alpha", "FillAlpha");
  private final NumberProperty<Float> outlineAlpha =
      new NumberProperty<Float>(255f, 0f, 255f, "Outline Alpha", "OutlineAlpha");

  public BurrowESP() {
    super("BurrowESP", new String[] {"burrowesp", "burrow"}, 0x55FF55, ModuleType.RENDER);
    setDescription("Boxes players standing inside solid blocks.");
    fillAlpha.visibleWhen(() -> useCustomAlpha.getValue());
    outlineAlpha.visibleWhen(() -> useCustomAlpha.getValue());
    offerProperties(
        showSelf, showFriends, showEnemies, lineWidth, useCustomAlpha, fillAlpha, outlineAlpha);
    showSelf.setDescription("Highlights you when you are inside a block.");
    showFriends.setDescription("Highlights friends inside blocks.");
    showEnemies.setDescription("Highlights other players inside blocks.");
    lineWidth.setDescription("Outline thickness in pixels.");
    useCustomAlpha.setDescription(
        "Use the Fill/Outline Alpha below instead of the global ESP alphas.");
    fillAlpha.setDescription("Box fill opacity, 0-255.");
    outlineAlpha.setDescription("Box outline opacity, 0-255.");
    this.listeners.add(
        new Listener<WorldRenderEvent>("burrow_esp_render") {
          @Override
          public void call(WorldRenderEvent event) {
            onRender();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "enabled");
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "disabled");
  }

  private void onRender() {
    if (minecraft.level == null || minecraft.player == null) return;

    for (Player player : minecraft.level.players()) {
      if (player == null || !player.isAlive()) continue;
      BlockPos feet = player.blockPosition();
      if (minecraft.level.getBlockState(feet).isAir()) continue;

      int color;
      if (player == minecraft.player) {
        if (!showSelf.getValue()) continue;
        color = 0xFF00FF00;
      } else if (Exeter.getInstance().getFriendManager().isFriend(player.getName().getString())) {
        if (!showFriends.getValue()) continue;
        color = 0xFF0000FF;
      } else {
        if (!showEnemies.getValue()) continue;
        color = 0xFFFF0000;
      }

      int fillColor =
          ARGB.color(
              useCustomAlpha.getValue()
                  ? Math.round(fillAlpha.getValue())
                  : EspRenderManager.getGlobalFillAlpha(),
              color);
      int outlineColor =
          ARGB.color(
              useCustomAlpha.getValue()
                  ? Math.round(outlineAlpha.getValue())
                  : EspRenderManager.getGlobalOutlineAlpha(),
              color);
      GizmoStyle style = GizmoStyle.strokeAndFill(outlineColor, lineWidth.getValue(), fillColor);
      Gizmos.cuboid(new AABB(feet), style).setAlwaysOnTop();
    }
  }
}
