package me.friendly.exeter.module.impl.toggle.render.hud;

import java.util.ArrayList;
import java.util.List;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;

public abstract class HudModule extends ToggleableModule {

  public enum Corner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
  }

  private int x;
  private int y;
  private Corner corner;
  private boolean positioned = false;

  protected HudModule(String label, String[] aliases, int color, Corner defaultCorner) {
    super(label, aliases, color, ModuleType.HUD);
    this.corner = defaultCorner;
    for (int i = 0; i < this.properties.size(); i++) {
      if (this.properties.get(i).getAliases()[0].equals("Drawn")) {
        this.properties.remove(i);
        break;
      }
    }
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

  public boolean isPositioned() {
    return positioned;
  }

  public void resetPosition() {
    this.positioned = false;
  }

  public static List<HudModule> getAll() {
    List<HudModule> result = new ArrayList<HudModule>();
    for (Module module : Exeter.getInstance().getModuleManager().getRegistry()) {
      if (module instanceof HudModule) {
        result.add((HudModule) module);
      }
    }
    return result;
  }

  public static List<HudModule> getActive() {
    List<HudModule> result = new ArrayList<HudModule>();
    for (HudModule module : getAll()) {
      if (module.isRunning()) {
        result.add(module);
      }
    }
    return result;
  }

  public static void layoutByCorner(List<HudModule> modules, int scaledWidth, int scaledHeight) {
    int topLeftY = 4;
    int topRightY = 4;
    int bottomLeftY = scaledHeight - 4;
    int bottomRightY = scaledHeight - 4;
    for (HudModule module : modules) {
      if (!module.isPositioned()) {
        if (module.getCorner() == Corner.TOP_LEFT) {
          module.setX(4);
          module.setY(topLeftY);
          topLeftY += module.getHeight() + 2;
        } else if (module.getCorner() == Corner.TOP_RIGHT) {
          module.setX(scaledWidth - 4 - module.getWidth());
          module.setY(topRightY);
          topRightY += module.getHeight() + 2;
        } else if (module.getCorner() == Corner.BOTTOM_LEFT) {
          bottomLeftY -= module.getHeight() + 2;
          module.setX(4);
          module.setY(bottomLeftY + 2);
        } else {
          bottomRightY -= module.getHeight() + 2;
          module.setX(scaledWidth - 4 - module.getWidth());
          module.setY(bottomRightY + 2);
        }
      }
    }
  }
}
