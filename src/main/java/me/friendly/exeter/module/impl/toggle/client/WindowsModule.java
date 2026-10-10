package me.friendly.exeter.module.impl.toggle.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
import me.friendly.exeter.window.impl.MacroWindow;
import me.friendly.exeter.window.impl.PluginWindow;
import me.friendly.exeter.window.impl.ProxyWindow;
import me.friendly.exeter.window.impl.WaypointWindow;

public final class WindowsModule extends ToggleableModule {

  private static WindowScreen screen;
  private final Map<String, int[]> pendingPositions = new HashMap<>();
  private final Map<String, Boolean> pendingHidden = new HashMap<>();

  /** Windows that start hidden on a fresh config; saved visibility wins once set. */
  private static final Set<String> DEFAULT_HIDDEN =
      Set.of("Proxies", "Waypoints", "Macros", "Plugins");

  public final ActionProperty resetPositions =
      new ActionProperty("Reset Positions", this::resetPositions);

  public WindowsModule() {
    super("Windows", new String[] {"Windows", "win"}, ModuleType.CLIENT);
    offerProperties(resetPositions);
    resetPositions.setDescription("Restores all client windows to their default positions.");
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

  /** Hidden flags loaded from windows.toml, applied on top of defaults when the screen opens. */
  public void setPendingHidden(Map<String, Boolean> hidden) {
    pendingHidden.clear();
    pendingHidden.putAll(hidden);
  }

  public Map<String, Boolean> getPendingHidden() {
    return pendingHidden;
  }

  /**
   * Copies live window positions and visibility into the pending maps, called when the screen
   * closes.
   */
  public void capturePositions() {
    if (screen == null) {
      return;
    }
    for (Window window : screen.getWindows()) {
      pendingPositions.put(window.getTitle(), new int[] {window.getX(), window.getY()});
      pendingHidden.put(window.getTitle(), window.isHidden());
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
    screen.addWindow(new WaypointWindow(mcWidth - 400, 300, 300, 220));
    screen.addWindow(new MacroWindow(440, 290, 380, 280));
    screen.addWindow(new PluginWindow(440, 580, 380, 220));

    for (Window window : screen.getWindows()) {
      int[] pos = pendingPositions.get(window.getTitle());
      if (pos != null) {
        window.setPosition(pos[0], pos[1]);
      }
      window.setHidden(
          pendingHidden.getOrDefault(
              window.getTitle(), DEFAULT_HIDDEN.contains(window.getTitle())));
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
