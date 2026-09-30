package me.friendly.exeter.module.impl.toggle.client;

import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.window.WindowScreen;
import me.friendly.exeter.window.impl.ConsoleWindow;
import me.friendly.exeter.window.impl.FriendsWindow;
import net.minecraft.client.Minecraft;

public final class WindowsModule extends ToggleableModule {

  private static WindowScreen screen;

  public WindowsModule() {
    super("Windows", new String[] {"windows", "win"}, 0x55FFFF, ModuleType.CLIENT);
    setDescription("Opens utility windows (console, friends).");
  }

  @Override
  protected void onEnable() {
    super.onEnable();
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
    screen.addWindow(new FriendsWindow(screenWidth - 220, 20, 200, 200));
    if (mc != null) {
      mc.setScreen(screen);
    }
    setRunning(false);
  }
}
