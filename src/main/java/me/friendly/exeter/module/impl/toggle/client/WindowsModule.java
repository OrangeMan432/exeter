package me.friendly.exeter.module.impl.toggle.client;

import java.util.HashMap;
import java.util.Map;
import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.config.ExeterConfig;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.ActionProperty;
import me.friendly.exeter.window.Window;
import me.friendly.exeter.window.WindowScreen;
import me.friendly.exeter.window.impl.AccountWindow;
import me.friendly.exeter.window.impl.ConsoleWindow;
import me.friendly.exeter.window.impl.FriendsWindow;
import net.minecraft.client.Minecraft;

public final class WindowsModule extends ToggleableModule {

  private static WindowScreen screen;
  private final Map<String, int[]> pendingPositions = new HashMap<String, int[]>();

  public final ActionProperty resetPositions =
      new ActionProperty(
          "Reset Positions",
          new Runnable() {
            @Override
            public void run() {
              resetPositions();
            }
          });

  public WindowsModule() {
    super("Windows", new String[] {"windows", "win"}, 0x55FFFF, ModuleType.CLIENT);
    setDescription("Opens utility windows (console, friends).");
    offerProperties(resetPositions);
  }

  public static WindowScreen getScreen() {
    return screen;
  }

  public void setPendingPositions(Map<String, int[]> positions) {
    pendingPositions.clear();
    pendingPositions.putAll(positions);
  }

  public Map<String, int[]> getPendingPositions() {
    return pendingPositions;
  }

  public void capturePositions() {
    if (screen == null) {
      return;
    }
    for (Window window : screen.getWindows()) {
      pendingPositions.put(window.getTitle(), new int[] {window.getX(), window.getY()});
    }
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    openDefaultWindows();
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc != null) {
      mc.setScreen(screen);
    }
    setRunning(false);
  }

  private void openDefaultWindows() {
    Minecraft mc = MinecraftAccessor.getMinecraft();
    screen = new WindowScreen();
    screen.addWindow(new ConsoleWindow(20, 20, 400, 250));
    int screenWidth = 854;
    if (mc != null) {
      try {
        screenWidth = mc.displayWidth / 2;
      } catch (Exception ignored) {
      }
    }
    screen.addWindow(new FriendsWindow(screenWidth - 280, 20, 260, 200));
    screen.addWindow(new AccountWindow(screenWidth - 300, 240, 280, 220));

    for (Window window : screen.getWindows()) {
      int[] pos = pendingPositions.get(window.getTitle());
      if (pos != null) {
        window.setPosition(pos[0], pos[1]);
      }
    }
  }

  private void resetPositions() {
    pendingPositions.clear();
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (screen != null && mc != null && mc.currentScreen == screen) {
      openDefaultWindows();
      mc.setScreen(screen);
    }
    if (ExeterConfig.getInstance() != null) {
      ExeterConfig.getInstance().saveModule(this);
    }
  }
}
