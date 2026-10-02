package me.friendly.exeter.module.impl.toggle.client;

import java.util.HashMap;
import java.util.Map;
import me.friendly.exeter.config.ExeterConfig;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.ActionProperty;
import me.friendly.exeter.util.NotificationManager;
import me.friendly.exeter.window.Window;
import me.friendly.exeter.window.WindowScreen;
import me.friendly.exeter.window.impl.AccountWindow;
import me.friendly.exeter.window.impl.ConsoleWindow;
import me.friendly.exeter.window.impl.FriendsWindow;
import me.friendly.exeter.window.impl.ProxyWindow;

public final class WindowsModule extends ToggleableModule {

  private static WindowScreen screen;
  private final Map<String, int[]> pendingPositions = new HashMap<>();

  public final ActionProperty resetPositions =
      new ActionProperty("Reset Positions", this::resetPositions);

  public WindowsModule() {
    super("Windows", new String[] {"Windows", "win"}, ModuleType.CLIENT);
    offerProperties(resetPositions);
  }

  public static WindowScreen getScreen() {
    return screen;
  }

  /** Positions loaded from windows.toml, applied on top of defaults when the screen opens. */
  public void setPendingPositions(Map<String, int[]> positions) {
    pendingPositions.clear();
    pendingPositions.putAll(positions);
  }

  public Map<String, int[]> getPendingPositions() {
    return pendingPositions;
  }

  /** Copies live window positions into the pending map, called when the screen closes. */
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
    minecraft.gui.setScreen(screen);
    setRunning(false);
  }

  private void openDefaultWindows() {
    screen = new WindowScreen();

    int mcWidth = minecraft.getWindow().getGuiScaledWidth();

    screen.addWindow(new ConsoleWindow(20, 20, 400, 250));
    screen.addWindow(new FriendsWindow(mcWidth - 300, 20, 280, 200));
    screen.addWindow(new AccountWindow(mcWidth - 400, 240, 360, 250));
    screen.addWindow(new ProxyWindow(20, 290, 400, 220));

    for (Window window : screen.getWindows()) {
      int[] pos = pendingPositions.get(window.getTitle());
      if (pos != null) {
        window.setPosition(pos[0], pos[1]);
      }
    }
  }

  private void resetPositions() {
    pendingPositions.clear();
    if (screen != null && minecraft.gui.screen() == screen) {
      openDefaultWindows();
      minecraft.gui.setScreen(screen);
    }
    ExeterConfig.getInstance().saveModule(this);
    NotificationManager.push("Window positions reset", "warning");
  }
}
