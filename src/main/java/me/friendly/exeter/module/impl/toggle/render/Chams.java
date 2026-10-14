package me.friendly.exeter.module.impl.toggle.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import me.friendly.api.event.Listener;
import me.friendly.exeter.events.WorldRenderEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.clickgui.SearchSelectPopup;
import me.friendly.exeter.module.impl.toggle.render.clickgui.SelectionPopup;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.PopupProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.render.EspRenderManager;
import me.friendly.exeter.render.ModelWireframe;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;

public final class Chams extends ToggleableModule {
  private final NumberProperty<Double> range =
      new NumberProperty<Double>(64.0, 8.0, 256.0, "Range");
  private final NumberProperty<Float> lineWidth =
      new NumberProperty<Float>(2.0f, 0.5f, 5.0f, "Line Width");
  private final Property<Boolean> fill = new Property<Boolean>(false, "Fill", "fill");
  private final Property<Boolean> playersAlways =
      new Property<Boolean>(true, "Players Always", "players");
  private final Property<Boolean> self = new Property<Boolean>(false, "Self", "self");
  private final Property<Boolean> thirdPerson =
      new Property<Boolean>(true, "Third Person", "thirdPerson");
  private final Property<Boolean> useCustomAlpha =
      new Property<Boolean>(false, "Custom Alpha", "CustomAlpha");
  private final NumberProperty<Float> fillAlpha =
      new NumberProperty<Float>(60f, 0f, 255f, "Fill Alpha", "FillAlpha");
  private final NumberProperty<Float> outlineAlpha =
      new NumberProperty<Float>(255f, 0f, 255f, "Outline Alpha", "OutlineAlpha");
  private final PopupProperty selectEntities;
  private final SelectionPopup.Ids entitySelections = new SelectionPopup.Ids("Chams Entities");

  public Chams() {
    super("Chams", new String[] {"chams"}, 0xFF55FF, ModuleType.RENDER);
    setDescription("Outlines every face of entity models, with optional fill.");
    this.selectEntities = new PopupProperty("Select Entities", this::openEntityPopup);
    thirdPerson.visibleWhen(() -> self.getValue());
    fillAlpha.visibleWhen(() -> useCustomAlpha.getValue());
    outlineAlpha.visibleWhen(() -> useCustomAlpha.getValue());
    offerProperties(
        range,
        lineWidth,
        fill,
        playersAlways,
        self,
        thirdPerson,
        useCustomAlpha,
        fillAlpha,
        outlineAlpha,
        entitySelections.getProperty(),
        selectEntities);
    range.setDescription("How far from you entities are rendered.");
    lineWidth.setDescription("Wireframe line thickness in pixels.");
    fill.setDescription("Also fill the model faces.");
    playersAlways.setDescription("Always render players, even if not selected.");
    self.setDescription("Render a wireframe around yourself.");
    thirdPerson.setDescription("Only render yourself while in third person camera.");
    useCustomAlpha.setDescription(
        "Use the Fill/Outline Alpha below instead of the global ESP alphas.");
    fillAlpha.setDescription("Fill opacity, 0-255.");
    outlineAlpha.setDescription("Wireframe opacity, 0-255.");
    entitySelections.getProperty().setDescription("Entity types currently rendered.");
    selectEntities.setDescription("Opens the entity picker.");

    this.listeners.add(
        new Listener<WorldRenderEvent>("chams_render") {
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
    DebugLogger.get()
        .log(
            getLabel(),
            DebugLogger.Level.INFO,
            "enabled, range " + range.getValue() + ", fill " + fill.getValue());
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "disabled");
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
    boolean filled = fill.getValue();
    float partial = ModelWireframe.currentPartialTick();

    int fillAlphaVal =
        useCustomAlpha.getValue()
            ? Math.round(fillAlpha.getValue())
            : EspRenderManager.getGlobalFillAlpha();
    int outlineAlphaVal =
        useCustomAlpha.getValue()
            ? Math.round(outlineAlpha.getValue())
            : EspRenderManager.getGlobalOutlineAlpha();
    int fillColor = ARGB.color(fillAlphaVal, EspRenderManager.getClientColor());
    int outlineColor = ARGB.color(outlineAlphaVal, EspRenderManager.getClientColor());

    for (Entity entity : minecraft.level.entitiesForRendering()) {
      if (entity == null || !entity.isAlive()) continue;

      boolean match = false;
      if (playersAlways.getValue() && entity instanceof Player && entity != minecraft.player) {
        match = true;
      }
      if (entity == minecraft.player && self.getValue()) {
        if (!thirdPerson.getValue() || !minecraft.options.getCameraType().isFirstPerson()) {
          match = true;
        }
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
      if (minecraft.player.distanceToSqr(entity) > rangeSq) continue;

      drawModel(entity, partial, filled, fillColor, outlineColor, lw);
    }
  }

  private void drawModel(
      Entity entity, float partial, boolean filled, int fillColor, int outlineColor, float lw) {
    List<ModelWireframe.Quad> quads = ModelWireframe.capture(entity, partial);
    for (ModelWireframe.Quad quad : quads) {
      net.minecraft.world.phys.Vec3 a = quad.a();
      net.minecraft.world.phys.Vec3 b = quad.b();
      net.minecraft.world.phys.Vec3 c = quad.c();
      net.minecraft.world.phys.Vec3 d = quad.d();
      if (filled) {
        Gizmos.rect(a, b, c, d, GizmoStyle.fill(fillColor)).setAlwaysOnTop();
      }
      Gizmos.line(a, b, outlineColor, lw).setAlwaysOnTop();
      Gizmos.line(b, c, outlineColor, lw).setAlwaysOnTop();
      Gizmos.line(c, d, outlineColor, lw).setAlwaysOnTop();
      Gizmos.line(d, a, outlineColor, lw).setAlwaysOnTop();
    }
  }

  public Set<String> getSelectedEntities() {
    return entitySelections.getSelected();
  }
}
