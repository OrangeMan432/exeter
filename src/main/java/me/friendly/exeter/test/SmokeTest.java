package me.friendly.exeter.test;

import com.mojang.blaze3d.platform.InputConstants;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ToggleableModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonInfo;

/**
 * Test-only smoke hook for CI. Active only with {@code -Dexeter.smokeTest=true}: presses Play Demo
 * World on the title screen, waits for the world, opens the ClickGUI once, and logs markers for log
 * assertions. Never runs in normal play.
 */
public final class SmokeTest {

  private SmokeTest() {}

  public static void maybeStart() {
    if (!Boolean.getBoolean("exeter.smokeTest")) return;
    Thread thread = new Thread(SmokeTest::run, "Exeter-SmokeTest");
    thread.setDaemon(true);
    thread.start();
  }

  private static void run() {
    try {
      Minecraft mc = Minecraft.getInstance();
      if (!pressDemoButton(mc)) return;
      if (!waitForWorld(mc)) return;
      Thread.sleep(5000);
      step(mc, "open clickgui", () -> toggle(mc, "clickgui"));
      Thread.sleep(2000);
      step(mc, "close ClickGUI", () -> mc.gui.setScreen(null));
      Thread.sleep(2000);
      step(mc, "open hudeditor", () -> toggle(mc, "hudeditor"));
      Thread.sleep(2000);
      step(mc, "close HUDEditor", () -> mc.gui.setScreen(null));
      Thread.sleep(2000);
      step(mc, "open windows", () -> toggle(mc, "Windows"));
      Thread.sleep(2000);
      step(mc, "close windows", () -> mc.gui.setScreen(null));
      Thread.sleep(2000);
      mc.execute(
          () ->
              System.out.println(
                  "[Exeter] SmokeTest: final screen="
                      + screenName(mc.gui.screen())
                      + ", sequence complete"));
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    }
  }

  private static void step(Minecraft mc, String label, Runnable action) {
    mc.execute(
        () -> {
          System.out.println(
              "[Exeter] SmokeTest: " + label + ", screen=" + screenName(mc.gui.screen()));
          action.run();
        });
  }

  private static void toggle(Minecraft mc, String alias) {
    Module module = Exeter.getInstance().getModuleManager().getModuleByAlias(alias);
    if (module instanceof ToggleableModule toggleable) {
      toggleable.setRunning(true);
    }
  }

  private static String screenName(Screen screen) {
    return screen == null ? "null" : screen.getClass().getName();
  }

  private static boolean pressDemoButton(Minecraft mc) throws InterruptedException {
    long deadline = System.currentTimeMillis() + 120_000;
    while (System.currentTimeMillis() < deadline) {
      Gui gui = mc.gui;
      if (gui != null) {
        Screen screen = gui.screen();
        if (screen instanceof TitleScreen) {
          for (GuiEventListener child : screen.children()) {
            if (child instanceof Button button
                && button.getMessage().getString().contains("Play Demo")) {
              mc.execute(
                  () -> button.onPress(new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)));
              System.out.println("[Exeter] SmokeTest: pressed Play Demo World");
              return true;
            }
          }
        }
      }
      Thread.sleep(1000);
    }
    System.out.println("[Exeter] SmokeTest: timed out waiting for title screen");
    return false;
  }

  private static boolean waitForWorld(Minecraft mc) throws InterruptedException {
    long deadline = System.currentTimeMillis() + 240_000;
    while ((mc.level == null || mc.player == null) && System.currentTimeMillis() < deadline) {
      Thread.sleep(1000);
    }
    if (mc.level == null || mc.player == null) {
      System.out.println("[Exeter] SmokeTest: timed out waiting for world");
      return false;
    }
    return true;
  }
}
