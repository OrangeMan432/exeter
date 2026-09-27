package me.friendly.exeter.test;

import com.mojang.blaze3d.platform.InputConstants;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ToggleableModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
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

  /**
   * Logs a marker to stdout and appends it to {@code smoke-markers.log} in the game directory.
   * Written directly by the game so CI can poll it without relying on Gradle streaming.
   */
  private static void mark(String message) {
    String line = "[Exeter] SmokeTest: " + message;
    System.out.println(line);
    try (java.io.FileWriter writer = new java.io.FileWriter("smoke-markers.log", true)) {
      writer.write("[" + java.time.LocalTime.now() + "] " + line + "\n");
    } catch (java.io.IOException ignored) {
    }
  }

  private static void run() {
    try {
      Minecraft mc = Minecraft.getInstance();
      if (!pressDemoButton(mc)) return;
      if (!waitForWorld(mc)) return;
      Thread.sleep(5000);
      step(mc, "open clickgui", null, () -> toggle(mc, "clickgui"));
      Thread.sleep(2000);
      step(mc, "close ClickGUI", "smoke-01-clickgui", () -> mc.gui.setScreen(null));
      Thread.sleep(2000);
      step(mc, "open hudeditor", null, () -> toggle(mc, "hudeditor"));
      Thread.sleep(2000);
      step(mc, "close HUDEditor", "smoke-02-hudeditor", () -> mc.gui.setScreen(null));
      Thread.sleep(2000);
      step(mc, "open windows", null, () -> toggle(mc, "Windows"));
      Thread.sleep(2000);
      step(mc, "close windows", "smoke-03-windows", () -> mc.gui.setScreen(null));
      Thread.sleep(2000);
      mc.execute(
          () -> {
            mark("final screen=" + screenName(mc.gui.screen()) + ", sequence complete");
            mc.stop();
          });
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    }
  }

  private static void step(Minecraft mc, String label, String shotName, Runnable action) {
    mc.execute(
        () -> {
          if (shotName != null) {
            capture(mc, shotName);
          }
          mark(label + ", screen=" + screenName(mc.gui.screen()));
          action.run();
        });
  }

  private static void capture(Minecraft mc, String name) {
    try {
      java.io.File dir = new java.io.File("screenshots");
      dir.mkdirs();
      java.io.File file = new java.io.File(dir, name + ".png");
      // grab(File, ...) treats the file as a directory, so write the image ourselves.
      Screenshot.takeScreenshot(
          mc.gameRenderer.mainRenderTarget(),
          image -> {
            try {
              image.writeToFile(file);
              mark("screenshot saved: " + file.getPath());
            } catch (Exception e) {
              mark("screenshot failed: " + e);
            }
          });
    } catch (Exception e) {
      mark("screenshot failed: " + e);
    }
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
    long deadline = System.currentTimeMillis() + 600_000;
    String lastSeen = "";
    while (System.currentTimeMillis() < deadline) {
      Gui gui = mc.gui;
      Screen screen = gui == null ? null : gui.screen();
      String seen =
          screen == null
              ? (gui == null ? "<booting>" : "<no screen>")
              : screen.getClass().getName();
      if (!seen.equals(lastSeen)) {
        lastSeen = seen;
        mark("waiting for title, seen=" + seen);
      }
      if (screen instanceof TitleScreen) {
        for (GuiEventListener child : screen.children()) {
          if (child instanceof Button button
              && button.getMessage().getString().contains("Play Demo")) {
            mc.execute(
                () -> button.onPress(new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)));
            mark("pressed Play Demo World");
            return true;
          }
        }
      } else if (screen != null && screen.getClass().getSimpleName().contains("Onboarding")) {
        for (GuiEventListener child : screen.children()) {
          if (child instanceof Button button
              && button.getMessage().getString().equalsIgnoreCase("Done")) {
            mc.execute(
                () -> button.onPress(new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)));
            mark("dismissed " + screen.getClass().getSimpleName());
            break;
          }
        }
      }
      Thread.sleep(1000);
    }
    mark("timed out waiting for title screen");
    return false;
  }

  private static boolean waitForWorld(Minecraft mc) throws InterruptedException {
    long deadline = System.currentTimeMillis() + 240_000;
    while ((mc.level == null || mc.player == null) && System.currentTimeMillis() < deadline) {
      Thread.sleep(1000);
    }
    if (mc.level == null || mc.player == null) {
      mark("timed out waiting for world");
      return false;
    }
    return true;
  }
}
