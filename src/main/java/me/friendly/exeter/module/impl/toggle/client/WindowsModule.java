package me.friendly.exeter.module.impl.toggle.client;

import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.window.WindowScreen;
import me.friendly.exeter.window.impl.AccountWindow;
import me.friendly.exeter.window.impl.ConsoleWindow;
import me.friendly.exeter.window.impl.FriendsWindow;

public final class WindowsModule extends ToggleableModule {

  private static WindowScreen screen;

  public WindowsModule() {
    super("Windows", new String[] {"Windows", "win"}, ModuleType.CLIENT);
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    screen = new WindowScreen();

    int mcWidth = minecraft.getWindow().getGuiScaledWidth();
    int mcHeight = minecraft.getWindow().getGuiScaledHeight();

    screen.addWindow(new ConsoleWindow(20, 20, 400, 250));
    screen.addWindow(new FriendsWindow(mcWidth - 300, 20, 200, 200));
    screen.addWindow(new AccountWindow(mcWidth - 400, 240, 360, 250));

    minecraft.gui.setScreen(screen);
    setRunning(false);
  }
}
