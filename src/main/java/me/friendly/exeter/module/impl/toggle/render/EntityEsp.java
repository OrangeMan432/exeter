package me.friendly.exeter.module.impl.toggle.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import me.friendly.api.event.Listener;
import me.friendly.exeter.events.WorldRenderEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.clickgui.SearchSelectPopup;
import me.friendly.exeter.module.impl.toggle.render.clickgui.SelectionPopup;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.PopupProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.render.EspRenderManager;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class EntityEsp extends ToggleableModule {
  private final NumberProperty<Double> range =
      new NumberProperty<Double>(64.0, 8.0, 256.0, "Range");
  private final NumberProperty<Float> lineWidth =
      new NumberProperty<Float>(2.0f, 0.5f, 5.0f, "Line Width");
  private final EnumProperty<RenderMode> renderMode =
      new EnumProperty<RenderMode>(RenderMode.BOTH, "Render", "mode");
  private final Property<Boolean> playersAlways =
      new Property<Boolean>(true, "Players Always", "players");
  private final Property<Boolean> useCustomAlpha =
      new Property<Boolean>(false, "Custom Alpha", "CustomAlpha");
  private final NumberProperty<Float> fillAlpha =
      new NumberProperty<Float>(60f, 0f, 255f, "Fill Alpha", "FillAlpha");
  private final NumberProperty<Float> outlineAlpha =
      new NumberProperty<Float>(255f, 0f, 255f, "Outline Alpha", "OutlineAlpha");
  private final PopupProperty selectEntities;
  private final SelectionPopup.Ids entitySelections = new SelectionPopup.Ids("Selected Entities");

  public EntityEsp() {
    super("EntityEsp", new String[] {"entityesp", "entity-esp"}, 0x00FFFF, ModuleType.RENDER);
    setDescription("Highlights specific entities in the world.");
    this.selectEntities = new PopupProperty("Select Entities", this::openEntityPopup);
    fillAlpha.visibleWhen(() -> useCustomAlpha.getValue());
    outlineAlpha.visibleWhen(() -> useCustomAlpha.getValue());
    offerProperties(
        range,
        lineWidth,
        renderMode,
        playersAlways,
        useCustomAlpha,
        fillAlpha,
        outlineAlpha,
        entitySelections.getProperty(),
        selectEntities);

    this.listeners.add(
        new Listener<WorldRenderEvent>("entity_esp_render") {
          @Override
          public void call(WorldRenderEvent event) {
            onRender();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    entitySelections.load();
  }

  private void openEntityPopup() {
    List<SearchSelectPopup.ToggleItem> items = new ArrayList<>();

    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.stream()
        .filter(type -> type != EntityTypes.PLAYER)
        .sorted(
            (a, b) -> {
              String aName =
                  net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(a).getPath();
              String bName =
                  net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(b).getPath();
              return aName.compareTo(bName);
            })
        .forEach(
            entityType -> {
              String id =
                  net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                      .getKey(entityType)
                      .toString();
              String displayName = entityType.getDescription().getString();
              items.add(SelectionPopup.idItem(id, displayName, entitySelections.getSelected()));
            });

    SelectionPopup.open("Select Entities", items, () -> entitySelections.save());
  }

  private void onRender() {
    if (minecraft.level == null || minecraft.player == null) return;

    double rangeSq = range.getValue() * range.getValue();
    float lw = lineWidth.getValue();
    boolean filled =
        renderMode.getValue() == RenderMode.FILLED || renderMode.getValue() == RenderMode.BOTH;
    boolean outlined =
        renderMode.getValue() == RenderMode.OUTLINED || renderMode.getValue() == RenderMode.BOTH;

    int fillAlphaVal =
        useCustomAlpha.getValue()
            ? Math.round(fillAlpha.getValue())
            : EspRenderManager.getGlobalFillAlpha();
    int outlineAlphaVal =
        useCustomAlpha.getValue()
            ? Math.round(outlineAlpha.getValue())
            : EspRenderManager.getGlobalOutlineAlpha();

    for (Entity entity : minecraft.level.entitiesForRendering()) {
      if (entity == null || entity == minecraft.player) continue;
      if (!entity.isAlive()) continue;

      boolean match = false;

      if (playersAlways.getValue() && entity instanceof Player) {
        match = true;
      }

      if (!match) {
        String typeId =
            net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .getKey(entity.getType())
                .toString();
        if (entitySelections.getSelected().contains(typeId)) {
          match = true;
        }
      }

      if (!match) continue;

      double distSq = minecraft.player.distanceToSqr(entity);
      if (distSq > rangeSq) continue;

      AABB box = entity.getBoundingBox();
      int fillColor = ARGB.color(fillAlphaVal, EspRenderManager.getClientColor());
      int outlineColor = ARGB.color(outlineAlphaVal, EspRenderManager.getClientColor());

      GizmoStyle style;
      if (filled && outlined) {
        style = GizmoStyle.strokeAndFill(outlineColor, lw, fillColor);
      } else if (filled) {
        style = GizmoStyle.fill(fillColor);
      } else {
        style = GizmoStyle.stroke(outlineColor, lw);
      }
      Gizmos.cuboid(box, style).setAlwaysOnTop();
    }
  }

  public Set<String> getSelectedEntities() {
    return entitySelections.getSelected();
  }

  private static enum RenderMode {
    FILLED,
    OUTLINED,
    BOTH;
  }
}
