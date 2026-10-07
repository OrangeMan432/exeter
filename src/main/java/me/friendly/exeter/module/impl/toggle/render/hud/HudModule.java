package me.friendly.exeter.module.impl.toggle.render.hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;

public abstract class HudModule extends ToggleableModule {

  public enum Corner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP_CENTER
  }

  private int x;
  private int y;
  private Corner corner;
  private boolean positioned = false;
  private boolean free = false;

  /** Previous frame's stack order per corner, so y-ties can't reshuffle stacks. */
  private static final java.util.Map<Corner, java.util.List<String>> stackOrderMemory =
      new java.util.EnumMap<>(Corner.class);

  protected HudModule(String label, String[] aliases, int color, Corner defaultCorner) {
    super(label, aliases, color, ModuleType.HUD);
    this.corner = defaultCorner;
    this.properties.removeIf(p -> p.getAliases()[0].equals("Drawn"));
  }

  protected HudModule(String label, String[] aliases, Corner defaultCorner) {
    this(label, aliases, -2366720, defaultCorner);
  }

  public abstract int getWidth();

  public abstract int getHeight();

  public abstract void render(int scaledWidth, int scaledHeight);

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public void setX(int x) {
    this.x = x;
    this.positioned = true;
  }

  public void setY(int y) {
    this.y = y;
    this.positioned = true;
  }

  public Corner getCorner() {
    return corner;
  }

  public void setCorner(Corner corner) {
    this.corner = corner;
  }

  /** Free-placed modules keep their exact x/y; corner stacks skip them. */
  public boolean isFree() {
    return free;
  }

  public void setFree(boolean free) {
    this.free = free;
  }

  /** TOP_CENTER stacks downward like the top corners. */
  public boolean isTop() {
    return corner == Corner.TOP_LEFT || corner == Corner.TOP_RIGHT || corner == Corner.TOP_CENTER;
  }

  public boolean isRight() {
    return corner == Corner.TOP_RIGHT || corner == Corner.BOTTOM_RIGHT;
  }

  public boolean isPositioned() {
    return positioned;
  }

  public void resetPosition() {
    this.positioned = false;
  }

  public static List<HudModule> getAll() {
    List<HudModule> result = new ArrayList<>();
    for (var module : Exeter.getInstance().getModuleManager().getRegistry()) {
      if (module instanceof HudModule hm) {
        result.add(hm);
      }
    }
    return result;
  }

  public static List<HudModule> getActive() {
    List<HudModule> result = new ArrayList<>();
    for (var module : Exeter.getInstance().getModuleManager().getRegistry()) {
      if (module instanceof HudModule hm && hm.isRunning()) {
        result.add(hm);
      }
    }
    return result;
  }

  public static void layoutByCorner(List<HudModule> modules, int scaledWidth, int scaledHeight) {
    int margin = 5;
    int gap = 2;

    for (Corner corner : Corner.values()) {
      List<HudModule> cornerModules =
          modules.stream()
              .filter(m -> m.getCorner() == corner && !m.isFree())
              .collect(Collectors.toList());

      boolean top =
          corner == Corner.TOP_LEFT || corner == Corner.TOP_RIGHT || corner == Corner.TOP_CENTER;
      boolean left = corner == Corner.TOP_LEFT || corner == Corner.BOTTOM_LEFT;
      boolean center = corner == Corner.TOP_CENTER;

      // Ties (e.g. an emptied radar sharing y with the module below it) resolve to the
      // previous frame's visual order, never to registry order, so stacks can't swap.
      List<String> previous = stackOrderMemory.getOrDefault(corner, List.of());
      Comparator<HudModule> verticalOrder =
          (top
                  ? Comparator.comparingInt(HudModule::getY)
                  : Comparator.comparingInt(HudModule::getY).reversed())
              .thenComparingInt(
                  m -> {
                    int index = previous.indexOf(m.getLabel());
                    return index == -1 ? Integer.MAX_VALUE : index;
                  });

      cornerModules.sort(verticalOrder);

      int baseY = top ? margin : scaledHeight - margin;

      for (HudModule m : cornerModules) {
        int w = m.getWidth();
        int h = m.getHeight();
        int baseX;
        if (center) {
          baseX = scaledWidth / 2 - w / 2;
        } else {
          baseX = left ? margin : scaledWidth - w - margin;
        }

        m.x = baseX;
        m.y = top ? baseY : baseY - h;
        m.positioned = true;

        baseY = top ? baseY + h + (h > 0 ? gap : 0) : baseY - h - (h > 0 ? gap : 0);
      }
      java.util.List<String> order = new java.util.ArrayList<>(cornerModules.size());
      for (HudModule m : cornerModules) {
        order.add(m.getLabel());
      }
      stackOrderMemory.put(corner, order);
    }
  }

  /**
   * Pulls overflowing boxes back on screen. Dynamic content (speed digits, potion entries) can
   * outgrow its laid-out box; idempotent, so safe to run every frame.
   */
  public static void clampOnScreen(List<HudModule> modules, int scaledWidth, int scaledHeight) {
    int margin = 5;
    for (HudModule m : modules) {
      int maxX = Math.max(margin, scaledWidth - m.getWidth() - margin);
      int maxY = Math.max(margin, scaledHeight - m.getHeight() - margin);
      m.setX(Math.max(margin, Math.min(m.getX(), maxX)));
      m.setY(Math.max(margin, Math.min(m.getY(), maxY)));
    }
  }

  public static void defaultLayout(List<HudModule> modules, int scaledWidth, int scaledHeight) {
    List<HudModule> unpositioned = new ArrayList<>();
    for (HudModule m : modules) {
      if (!m.isPositioned()) {
        unpositioned.add(m);
      }
    }
    if (unpositioned.isEmpty()) return;

    int margin = 5;
    int gap = 2;

    for (Corner corner : Corner.values()) {
      List<HudModule> cornerModules =
          unpositioned.stream().filter(m -> m.getCorner() == corner).collect(Collectors.toList());
      if (cornerModules.isEmpty()) continue;

      boolean top = corner == Corner.TOP_LEFT || corner == Corner.TOP_RIGHT;
      boolean left = corner == Corner.TOP_LEFT || corner == Corner.BOTTOM_LEFT;

      int baseY = top ? margin : scaledHeight - margin;

      for (HudModule m : cornerModules) {
        int w = m.getWidth();
        int h = m.getHeight();
        int baseX = left ? margin : scaledWidth - w - margin;

        m.x = baseX;
        m.y = top ? baseY : baseY - h;
        m.positioned = true;

        baseY = top ? baseY + h + (h > 0 ? gap : 0) : baseY - h - (h > 0 ? gap : 0);
      }
    }
  }
}
