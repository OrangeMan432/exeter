package me.friendly.exeter.render;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.WorldRenderEvent;
import me.friendly.exeter.module.impl.active.render.Colors;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;

public final class EspRenderManager {
  private static EspRenderManager INSTANCE;
  private final List<EspEntry> entries = new ArrayList<>();

  private EspRenderManager() {
    Exeter.getInstance()
        .getEventManager()
        .register(
            new Listener<WorldRenderEvent>("esp_render_manager_world_render") {
              @Override
              public void call(WorldRenderEvent event) {
                renderAll();
              }
            });
  }

  public static EspRenderManager getInstance() {
    if (INSTANCE == null) INSTANCE = new EspRenderManager();
    return INSTANCE;
  }

  public void addFilledBox(AABB box, int color) {
    entries.add(new EspEntry(box, color, color, 0, EntryMode.FILLED));
  }

  public void addOutlinedBox(AABB box, int color, float lineWidth) {
    entries.add(new EspEntry(box, color, color, lineWidth, EntryMode.OUTLINED));
  }

  public void addBox(AABB box, int color, float lineWidth) {
    entries.add(new EspEntry(box, color, color, lineWidth, EntryMode.BOTH));
  }

  public void addBox(AABB box, int fillColor, int outlineColor, float lineWidth) {
    entries.add(new EspEntry(box, fillColor, outlineColor, lineWidth, EntryMode.BOTH));
  }

  public void addBoxEsp(
      AABB box, float lineWidth, boolean filled, boolean outlined,
      int overrideFillAlpha, int overrideOutlineAlpha) {
    int fillAlpha = overrideFillAlpha >= 0 ? overrideFillAlpha : Colors.getEspFillAlpha();
    int outlineAlpha = overrideOutlineAlpha >= 0 ? overrideOutlineAlpha : Colors.getEspOutlineAlpha();

    int fillColor = Colors.getClientColorCustomAlpha(fillAlpha);
    int outlineColor = Colors.getClientColorCustomAlpha(outlineAlpha);

    if (filled && outlined) {
      entries.add(new EspEntry(box, fillColor, outlineColor, lineWidth, EntryMode.BOTH));
    } else if (filled) {
      entries.add(new EspEntry(box, fillColor, fillColor, 0, EntryMode.FILLED));
    } else if (outlined) {
      entries.add(new EspEntry(box, outlineColor, outlineColor, lineWidth, EntryMode.OUTLINED));
    }
  }

  private void renderAll() {
    if (entries.isEmpty()) return;

    for (EspEntry entry : entries) {
      GizmoStyle style;
      if (entry.mode == EntryMode.BOTH) {
        style = GizmoStyle.strokeAndFill(entry.outlineColor, entry.lineWidth, entry.fillColor);
      } else if (entry.mode == EntryMode.FILLED) {
        style = GizmoStyle.fill(entry.fillColor);
      } else {
        style = GizmoStyle.stroke(entry.outlineColor, entry.lineWidth);
      }
      Gizmos.cuboid(entry.box, style).setAlwaysOnTop();
    }

    entries.clear();
  }

  public boolean isRendering() {
    return !entries.isEmpty();
  }

  public static int getClientColor() {
    return Colors.getClientColor();
  }

  public static int getGlobalFillAlpha() {
    return Colors.getEspFillAlpha();
  }

  public static int getGlobalOutlineAlpha() {
    return Colors.getEspOutlineAlpha();
  }

  private enum EntryMode {
    FILLED,
    OUTLINED,
    BOTH
  }

  private record EspEntry(
      AABB box,
      int fillColor,
      int outlineColor,
      float lineWidth,
      EntryMode mode) {}
}
