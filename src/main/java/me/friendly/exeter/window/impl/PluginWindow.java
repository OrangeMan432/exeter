package me.friendly.exeter.window.impl;

import com.mojang.blaze3d.platform.InputConstants;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.plugin.Plugin;
import me.friendly.exeter.plugin.PluginDownloader;
import me.friendly.exeter.plugin.PluginList;
import me.friendly.exeter.plugin.PluginManager;
import me.friendly.exeter.window.Window;
import me.friendly.exeter.window.WindowButtons;
import net.minecraft.client.input.KeyEvent;

/**
 * Plugin manager window, tabbed: Browse lists community plugins from the plugin list (needs
 * internet), Installed shows locally loaded plugins and always works offline. Refresh refetches in
 * Browse and rescans the folder in Installed; Install downloads from GitHub releases and loads
 * without a restart; Remove stops a plugin and deletes its jar.
 */
public class PluginWindow extends Window {
  private static final int ENTRY_HEIGHT = 14;
  private static final int BUTTON_HEIGHT = 12;
  private static final int BOTTOM_BAR_HEIGHT = 22;
  private static final int TAB_HEIGHT = 13;

  private enum View {
    BROWSE,
    INSTALLED
  }

  private static volatile List<PluginList.Entry> cachedEntries = null;
  private static volatile boolean fetching;
  private static volatile boolean fetchFailed;
  private View view = View.BROWSE;
  private int selectedIndex = -1;
  private int scrollOffset;

  private volatile String status;
  private volatile long statusExpiry;

  public PluginWindow(int x, int y, int width, int height) {
    super("Plugins", x, y, width, height);
  }

  private PluginManager plugins() {
    return Exeter.getInstance().getPluginManager();
  }

  private boolean installed(PluginList.Entry entry) {
    return plugins().get(entry.name()).isPresent();
  }

  private List<Plugin> installedPlugins() {
    return new ArrayList<>(plugins().getList());
  }

  // Rendering

  @Override
  protected void renderContent(int mouseX, int mouseY, float partialTicks) {
    int contentY = y + TITLE_HEIGHT;
    int tabY = contentY + 2;
    int[] tabWidths = {
      FontUtil.getStringWidth("Browse") + 10, FontUtil.getStringWidth("Installed") + 10
    };
    String[] tabLabels = {"Browse", "Installed"};
    View[] tabViews = {View.BROWSE, View.INSTALLED};
    int tabX = x + 3;
    for (int i = 0; i < 2; i++) {
      boolean hovered =
          mouseX >= tabX
              && mouseX <= tabX + tabWidths[i]
              && mouseY >= tabY
              && mouseY <= tabY + TAB_HEIGHT;
      boolean active = view == tabViews[i];
      RenderMethods.drawRect(
          tabX,
          tabY,
          tabX + tabWidths[i],
          tabY + TAB_HEIGHT,
          active ? Colors.getClientColorCustomAlpha(160) : (hovered ? 0xFF3A3A3A : 0xFF222222));
      FontUtil.drawString(tabLabels[i], tabX + 5, tabY + 2, active ? 0xFFFFFFFF : 0xFFBBBBBB);
      tabX += tabWidths[i] + 2;
    }

    int listY = tabY + TAB_HEIGHT + 3;
    int barY = y + height - BOTTOM_BAR_HEIGHT;
    int listH = barY - listY;

    if (view == View.BROWSE) {
      renderBrowse(mouseX, mouseY, listY, listH);
    } else {
      renderInstalled(mouseX, mouseY, listY, listH);
    }

    drawStatus(listY + listH - 12);
    RenderMethods.drawRect(x, barY, x + width, y + height, 0x77111111);

    int btnY = barY + 4;
    drawButton(0, btnY, mouseX, mouseY, "Refresh");
    drawButton(1, btnY, mouseX, mouseY, "Install");
    drawButton(2, btnY, mouseX, mouseY, "Reload");
    drawButton(3, btnY, mouseX, mouseY, "Remove");
  }

  private boolean inViewTab(int mouseX, int mouseY, View target) {
    int tabY = y + TITLE_HEIGHT + 2;
    int tabX = x + 3;
    String[] labels = {"Browse", "Installed"};
    View[] views = {View.BROWSE, View.INSTALLED};
    for (int i = 0; i < 2; i++) {
      int w = FontUtil.getStringWidth(labels[i]) + 10;
      if (views[i] == target
          && mouseX >= tabX
          && mouseX <= tabX + w
          && mouseY >= tabY
          && mouseY <= tabY + TAB_HEIGHT) {
        return true;
      }
      tabX += w + 2;
    }
    return false;
  }

  private void renderBrowse(int mouseX, int mouseY, int listY, int listH) {
    List<PluginList.Entry> current = cachedEntries;
    if (current == null && !fetchFailed && !fetching) {
      refresh();
      current = cachedEntries;
    }
    if (current == null) {
      FontUtil.drawString(
          "Fetching plugin list...", x + width / 2 - 60, listY + listH / 2, 0xFF888888);
      return;
    }
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

  private void renderInstalled(int mouseX, int mouseY, int listY, int listH) {
    List<Plugin> current = installedPlugins();
    int maxVisible = Math.max(1, listH / ENTRY_HEIGHT);
    scrollOffset = Math.min(scrollOffset, Math.max(0, current.size() - maxVisible));
    int rowY = listY;
    for (int i = scrollOffset; i < Math.min(current.size(), scrollOffset + maxVisible); i++) {
      Plugin plugin = current.get(i);
      boolean hovered =
          mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY <= rowY + ENTRY_HEIGHT;
      if (hovered || i == selectedIndex) {
        RenderMethods.drawRect(x, rowY, x + width, rowY + ENTRY_HEIGHT, 0x20FFFFFF);
      }
      String marker = plugin.isRunning() ? "§a[Enabled] " : "§8[Disabled] ";
      me.friendly.exeter.plugin.PluginInfo info =
          Exeter.getInstance().getPluginManager().info(plugin.getName());
      String text =
          marker + "§f" + plugin.getName() + " §8" + info.version() + " by " + info.author();
      FontUtil.drawString(text, x + 5, rowY + 2, i == selectedIndex ? 0xFFFFFFAA : 0xFFEEEEEE);
      rowY += ENTRY_HEIGHT;
    }
    if (current.isEmpty()) {
      FontUtil.drawString(
          "No plugins installed.", x + width / 2 - 60, listY + listH / 2, 0xFF888888);
    }
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

  // Buttons: four equal columns. Remove needs a selection.

  private final WindowButtons buttons =
      new WindowButtons(this, 4, BUTTON_HEIGHT, id -> id != 3 || hasSelection());

  private boolean hasSelection() {
    return selectedIndex >= 0 && selectedIndex < visibleCount();
  }

  private int visibleCount() {
    if (view == View.BROWSE) {
      List<PluginList.Entry> current = cachedEntries;
      return current == null ? 0 : current.size();
    }
    return installedPlugins().size();
  }

  private void drawButton(int id, int btnY, int mouseX, int mouseY, String label) {
    buttons.drawButton(id, btnY, mouseX, mouseY, label);
  }

  // Input

  private int listY() {
    return y + TITLE_HEIGHT + 2 + TAB_HEIGHT + 3;
  }

  private boolean inViewTab(int mouseX, int mouseY, int slot, String label) {
    int tabY = y + TITLE_HEIGHT + 2;
    int tabX = x + 3 + slot * (FontUtil.getStringWidth("Installed") + 14);
    int w = FontUtil.getStringWidth(label) + 10;
    return mouseX >= tabX && mouseX <= tabX + w && mouseY >= tabY && mouseY <= tabY + TAB_HEIGHT;
  }

  @Override
  protected boolean consumeClick(int mouseX, int mouseY, int button) {
    if (button != InputConstants.MOUSE_BUTTON_LEFT) {
      return false;
    }
    if (inViewTab(mouseX, mouseY, View.BROWSE)) {
      view = View.BROWSE;
      selectedIndex = -1;
      scrollOffset = 0;
      return true;
    }
    if (inViewTab(mouseX, mouseY, View.INSTALLED)) {
      view = View.INSTALLED;
      selectedIndex = -1;
      scrollOffset = 0;
      return true;
    }
    int listY = listY();
    int barY = y + height - BOTTOM_BAR_HEIGHT;

    int count = visibleCount();
    int listH = barY - listY;
    int maxVisible = Math.max(1, listH / ENTRY_HEIGHT);
    for (int i = scrollOffset; i < Math.min(count, scrollOffset + maxVisible); i++) {
      int rowY = listY + (i - scrollOffset) * ENTRY_HEIGHT;
      if (mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY <= rowY + ENTRY_HEIGHT) {
        selectedIndex = i;
        return true;
      }
    }

    int btnY = barY + 4;
    if (buttons.clickButton(0, btnY, mouseX, mouseY)) {
      if (view == View.BROWSE) {
        refresh();
      } else {
        List<Plugin> loaded = plugins().reload();
        setStatus(
            loaded.isEmpty()
                ? "No new plugins found."
                : "§aLoaded " + loaded.size() + " plugin(s).",
            3000);
      }
      return true;
    }
    if (buttons.clickButton(1, btnY, mouseX, mouseY)) {
      if (view == View.BROWSE) {
        installSelected();
      } else {
        setStatus("§cPick a plugin from Browse to install.", 3000);
      }
      return true;
    }
    if (buttons.clickButton(2, btnY, mouseX, mouseY)) {
      List<Plugin> loaded = plugins().reload();
      setStatus(
          loaded.isEmpty() ? "No new plugins found." : "§aLoaded " + loaded.size() + " plugin(s).",
          3000);
      refresh();
      return true;
    }
    if (buttons.clickButton(3, btnY, mouseX, mouseY)) {
      uninstallSelected();
      return true;
    }
    return true;
  }

  @Override
  protected boolean consumeScroll(double mouseX, double mouseY, double scrollDelta) {
    int count = visibleCount();
    int listH = (y + height - BOTTOM_BAR_HEIGHT) - listY();
    int maxVisible = Math.max(1, listH / ENTRY_HEIGHT);
    int maxScroll = Math.max(0, count - maxVisible);
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

  private void uninstallSelected() {
    if (view == View.INSTALLED) {
      List<Plugin> current = installedPlugins();
      if (selectedIndex < 0 || selectedIndex >= current.size()) {
        setStatus("§cSelect an installed plugin first.", 3000);
        return;
      }
      String name = current.get(selectedIndex).getName();
      if (plugins().uninstall(name)) {
        setStatus("§aUninstalled " + name + ".", 3000);
        selectedIndex = -1;
      } else {
        setStatus("§cCould not uninstall.", 3000);
      }
      return;
    }
    List<PluginList.Entry> current = cachedEntries;
    PluginList.Entry entry =
        (current == null || selectedIndex < 0 || selectedIndex >= current.size())
            ? null
            : current.get(selectedIndex);
    if (entry == null || !installed(entry)) {
      setStatus("§cSelect an installed plugin first.", 3000);
      return;
    }
    if (plugins().uninstall(entry.name())) {
      setStatus("§aUninstalled " + entry.name() + ".", 3000);
    } else {
      setStatus("§cCould not uninstall.", 3000);
    }
  }

  private void refresh() {
    if (fetching) return;
    fetching = true;
    fetchFailed = false;
    setStatus("Fetching plugin list...", 0);
    new Thread(
            () -> {
              try {
                cachedEntries = PluginList.fetch();
                setStatus("§a" + cachedEntries.size() + " plugins listed.", 3000);
              } catch (Exception e) {
                fetchFailed = true;
                setStatus("§cCould not fetch the plugin list.", 4000);
              } finally {
                fetching = false;
              }
            },
            "exeter-plugin-list")
        .start();
  }

  private void installSelected() {
    List<PluginList.Entry> current = cachedEntries;
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
                    plugins().getFile(),
                    (Optional<File> file) -> {
                      if (file.isEmpty()) {
                        setStatus("§cDownload failed.", 4000);
                        return;
                      }
                      Optional<Plugin> plugin = plugins().loadFile(file.get());
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
