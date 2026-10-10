package me.friendly.exeter.window.impl;

import com.mojang.blaze3d.platform.InputConstants;
import java.io.File;
import java.util.List;
import java.util.Optional;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.plugin.Plugin;
import me.friendly.exeter.plugin.PluginDownloader;
import me.friendly.exeter.plugin.PluginList;
import me.friendly.exeter.window.Window;
import me.friendly.exeter.window.WindowButtons;
import net.minecraft.client.input.KeyEvent;

/**
 * Plugin browser window. Lists community plugins from the plugin list with installed markers;
 * Refresh refetches, Install downloads the selected entry from its GitHub release and loads it
 * without a restart, Reload picks up jars dropped into the plugins folder by hand.
 */
public class PluginWindow extends Window {
  private static final int ENTRY_HEIGHT = 14;
  private static final int BUTTON_HEIGHT = 12;
  private static final int BOTTOM_BAR_HEIGHT = 22;

  private volatile List<PluginList.Entry> entries = null;
  private volatile boolean fetching;
  private int selectedIndex = -1;
  private int scrollOffset;

  private volatile String status;
  private volatile long statusExpiry;

  public PluginWindow(int x, int y, int width, int height) {
    super("Plugins", x, y, width, height);
  }

  private boolean installed(PluginList.Entry entry) {
    return Exeter.getInstance().getPluginManager().get(entry.name()).isPresent();
  }

  // Rendering

  @Override
  protected void renderContent(int mouseX, int mouseY, float partialTicks) {
    int contentY = y + TITLE_HEIGHT;
    int listY = contentY + 2;
    int barY = y + height - BOTTOM_BAR_HEIGHT;
    int listH = barY - listY;

    List<PluginList.Entry> current = entries;
    if (current == null && !fetching) {
      refresh();
      current = entries;
    }

    if (current == null) {
      FontUtil.drawString(
          "Fetching plugin list...", x + width / 2 - 60, listY + listH / 2, 0xFF888888);
    } else {
      int maxVisible = Math.max(1, listH / ENTRY_HEIGHT);
      scrollOffset = Math.min(scrollOffset, Math.max(0, current.size() - maxVisible));
      int rowY = listY;
      for (int i = scrollOffset; i < Math.min(current.size(), scrollOffset + maxVisible); i++) {
        PluginList.Entry entry = current.get(i);
        boolean hovered =
            mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY <= rowY + ENTRY_HEIGHT;
        if (hovered || i == selectedIndex) {
          RenderMethods.drawRect(x, rowY, x + width, rowY + ENTRY_HEIGHT, 0x20FFFFFF);
        }
        String marker = installed(entry) ? "§a[+] " : "§8[ ] ";
        String text =
            marker + "§f" + entry.name() + " §8" + entry.version() + " by " + entry.author();
        FontUtil.drawString(text, x + 5, rowY + 2, i == selectedIndex ? 0xFFFFFFAA : 0xFFEEEEEE);
        rowY += ENTRY_HEIGHT;
      }
      if (current.isEmpty()) {
        FontUtil.drawString(
            "The plugin list is empty.", x + width / 2 - 60, listY + listH / 2, 0xFF888888);
      }
    }

    drawStatus(listY + listH - 12);
    RenderMethods.drawRect(x, barY, x + width, y + height, 0x77111111);

    int btnY = barY + 4;
    drawButton(0, btnY, mouseX, mouseY, "Refresh");
    drawButton(1, btnY, mouseX, mouseY, "Install");
    drawButton(2, btnY, mouseX, mouseY, "Reload");
  }

  private void drawStatus(int statusY) {
    String current = status();
    if (current != null) {
      FontUtil.drawString(
          current, x + width / 2 - FontUtil.getStringWidth(current) / 2, statusY, 0xFFFFFFFF);
    }
  }

  private String status() {
    if (status == null) {
      return null;
    }
    if (statusExpiry != 0 && System.currentTimeMillis() > statusExpiry) {
      status = null;
      return null;
    }
    return status;
  }

  private void setStatus(String message, long durationMs) {
    status = message;
    statusExpiry = durationMs <= 0 ? 0 : System.currentTimeMillis() + durationMs;
  }

  // Buttons: three equal columns.

  private final WindowButtons buttons = new WindowButtons(this, 3, BUTTON_HEIGHT, id -> true);

  private void drawButton(int id, int btnY, int mouseX, int mouseY, String label) {
    buttons.drawButton(id, btnY, mouseX, mouseY, label);
  }

  // Input

  @Override
  protected boolean consumeClick(int mouseX, int mouseY, int button) {
    if (button != InputConstants.MOUSE_BUTTON_LEFT) {
      return false;
    }
    int contentY = y + TITLE_HEIGHT;
    int listY = contentY + 2;
    int barY = y + height - BOTTOM_BAR_HEIGHT;

    List<PluginList.Entry> current = entries;
    if (current != null) {
      int listH = barY - listY;
      int maxVisible = Math.max(1, listH / ENTRY_HEIGHT);
      for (int i = scrollOffset; i < Math.min(current.size(), scrollOffset + maxVisible); i++) {
        int rowY = listY + (i - scrollOffset) * ENTRY_HEIGHT;
        if (mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY <= rowY + ENTRY_HEIGHT) {
          selectedIndex = i;
          return true;
        }
      }
    }

    int btnY = barY + 4;
    if (buttons.clickButton(0, btnY, mouseX, mouseY)) {
      refresh();
      return true;
    }
    if (buttons.clickButton(1, btnY, mouseX, mouseY)) {
      installSelected();
      return true;
    }
    if (buttons.clickButton(2, btnY, mouseX, mouseY)) {
      List<Plugin> loaded = Exeter.getInstance().getPluginManager().reload();
      setStatus(
          loaded.isEmpty() ? "No new plugins found." : "§aLoaded " + loaded.size() + " plugin(s).",
          3000);
      return true;
    }
    return true;
  }

  @Override
  protected boolean consumeScroll(double mouseX, double mouseY, double scrollDelta) {
    List<PluginList.Entry> current = entries;
    if (current == null) return true;
    int listH = (y + height - BOTTOM_BAR_HEIGHT) - (y + TITLE_HEIGHT + 2);
    int maxVisible = Math.max(1, listH / ENTRY_HEIGHT);
    int maxScroll = Math.max(0, current.size() - maxVisible);
    if (scrollDelta < 0) {
      scrollOffset = Math.min(maxScroll, scrollOffset + 1);
    } else {
      scrollOffset = Math.max(0, scrollOffset - 1);
    }
    return true;
  }

  @Override
  protected boolean consumeKeyPress(KeyEvent event) {
    if (event.key() == InputConstants.KEY_ESCAPE) {
      selectedIndex = -1;
      return true;
    }
    return false;
  }

  // Actions

  private void refresh() {
    if (fetching) return;
    fetching = true;
    setStatus("Fetching plugin list...", 0);
    new Thread(
            () -> {
              try {
                entries = PluginList.fetch();
                setStatus("§a" + entries.size() + " plugins listed.", 3000);
              } catch (Exception e) {
                setStatus("§cCould not fetch the plugin list.", 4000);
              } finally {
                fetching = false;
              }
            },
            "exeter-plugin-list")
        .start();
  }

  private void installSelected() {
    List<PluginList.Entry> current = entries;
    if (current == null || selectedIndex < 0 || selectedIndex >= current.size()) {
      setStatus("§cSelect a plugin first.", 3000);
      return;
    }
    PluginList.Entry entry = current.get(selectedIndex);
    if (installed(entry)) {
      setStatus("§cAlready installed (" + entry.name() + ")", 3000);
      return;
    }
    setStatus("Downloading " + entry.name() + " " + entry.version() + "...", 0);
    new Thread(
            () ->
                PluginDownloader.install(
                    entry,
                    Exeter.getInstance().getPluginManager().getFile(),
                    (Optional<File> file) -> {
                      if (file.isEmpty()) {
                        setStatus("§cDownload failed.", 4000);
                        return;
                      }
                      Optional<Plugin> plugin =
                          Exeter.getInstance().getPluginManager().loadFile(file.get());
                      if (plugin.isEmpty()) {
                        setStatus("§cNo plugin inside " + file.get().getName() + ".", 4000);
                        return;
                      }
                      setStatus("§aInstalled " + plugin.get().getName() + ".", 4000);
                    }),
            "exeter-plugin-install")
        .start();
  }
}
