package me.larp.client.module.impl.toggle.render.clickgui;

import java.util.ArrayList;
import java.util.List;
import me.larp.api.minecraft.render.RenderMethods;
import me.larp.api.minecraft.render.font.FontUtil;
import me.larp.client.core.Larp;
import me.larp.client.module.Module;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.module.impl.active.render.Colors;
import me.larp.client.module.impl.toggle.render.clickgui.item.ModuleButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Modern single-window GUI: sidebar categories, two-column module cards,
 * inline settings, top search. Reuses ModuleButton for all interactions.
 */
public final class ModernGui extends Screen {

  private static ModernGui modernGui;
  private ModuleType selected = ModuleType.COMBAT;
  private String search = "";
  private final List<ModuleButton> buttons = new ArrayList<>();

  private static final int SIDEBAR_W = 120;
  private static final int PAD = 16;
  private static final int CARD_GAP = 6;

  public ModernGui() {
    super(Component.literal("LarpGui"));
    rebuild();
  }

  public static ModernGui getModernGui() {
    if (modernGui == null) {
      modernGui = new ModernGui();
    } else {
      modernGui.rebuild();
    }
    return modernGui;
  }

  private void rebuild() {
    buttons.clear();
    for (Module module : Larp.getInstance().getModuleManager().getRegistry()) {
      if (!(module instanceof ToggleableModule)) continue;
      if (module.getLabel().equalsIgnoreCase("ClickGui")) continue;
      buttons.add(new ModuleButton(module));
    }
    buttons.sort((a, b) -> a.getLabel().compareToIgnoreCase(b.getLabel()));
  }

  private List<ModuleButton> visible() {
    List<ModuleButton> out = new ArrayList<>();
    for (ModuleButton button : buttons) {
      Module module = button.getModule();
      if (!(module instanceof ToggleableModule tm)) continue;
      if (tm.getModuleType() != selected) continue;
      if (!search.isEmpty()
          && !button.getLabel().toLowerCase().contains(search)) continue;
      out.add(button);
    }
    return out;
  }

  @Override
  public void extractRenderState(
      GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
    RenderMethods.guiGraphics = guiGraphics;
    int sw = this.width;
    int sh = this.height;

    // Dim background.
    RenderMethods.drawGradientRect(0, 0, sw, sh, 0xDD000000, 0xDD0A0A12);

    int winX = 30;
    int winY = 24;
    int winW = sw - 60;
    int winH = sh - 48;

    // Window.
    RenderMethods.drawRect(winX, winY, winX + winW, winY + winH, 0xF0141418);
    RenderMethods.drawRect(winX, winY, winX + winW, winY + 1, Colors.getClientColorCustomAlpha(255));

    // Header.
    FontUtil.drawString("LARP CLIENT", winX + 10, winY + 8, -1);
    String count =
        visible().size() + " modules";
    FontUtil.drawString(
        count, winX + winW - FontUtil.getStringWidth(count) - 10, winY + 8, 0xFF888888);

    // Search bar.
    String query = search.isEmpty() ? "Search..." : search;
    int sColor = search.isEmpty() ? 0xFF555555 : 0xFFFFFFFF;
    int searchY = winY + 24;
    RenderMethods.drawRect(
        winX + SIDEBAR_W + PAD, searchY, winX + winW - PAD, searchY + 14, 0xFF0A0A0E);
    FontUtil.drawString(query, winX + SIDEBAR_W + PAD + 4, searchY + 3, sColor);

    // Sidebar categories.
    int catY = winY + 44;
    for (ModuleType type : ModuleType.values()) {
      if (type == ModuleType.CLIENT) continue;
      boolean active = type == selected;
      int cTop = catY;
      int cBottom = catY + 16;
      if (active) {
        RenderMethods.drawRect(winX + 6, cTop, winX + SIDEBAR_W - 6, cBottom, 0x3322FF66);
        RenderMethods.drawRect(winX + 6, cTop, winX + 8, cBottom, 0xFF22FF66);
      }
      int tColor = active ? 0xFFFFFFFF : 0xFF888888;
      FontUtil.drawString(type.getLabel().toUpperCase(), winX + 14, cTop + 4, tColor);
      catY += 19;
    }
    // Client category at the bottom of the sidebar.
    boolean clientActive = selected == ModuleType.CLIENT;
    if (clientActive) {
      RenderMethods.drawRect(winX + 6, catY, winX + SIDEBAR_W - 6, catY + 16, 0x3322FF66);
      RenderMethods.drawRect(winX + 6, catY, winX + 8, catY + 16, 0xFF22FF66);
    }
    FontUtil.drawString(
        "CLIENT", winX + 14, catY + 4, clientActive ? 0xFFFFFFFF : 0xFF888888);

    // Two-column module cards.
    int gridX = winX + SIDEBAR_W + PAD;
    int gridY = searchY + 20;
    int gridW = winX + winW - PAD - gridX;
    int cardW = (gridW - CARD_GAP) / 2;
    List<ModuleButton> shown = visible();
    int[] colY = {gridY, gridY};
    for (int i = 0; i < shown.size(); i++) {
      ModuleButton button = shown.get(i);
      int col = i % 2;
      int bx = gridX + col * (cardW + CARD_GAP);
      int by = colY[col];
      button.setLocation(bx, by);
      button.setWidth(cardW);
      drawCard(button, mouseX, mouseY, partialTicks);
      colY[col] += button.getHeight() + CARD_GAP;
    }
  }

  private void drawCard(ModuleButton button, int mouseX, int mouseY, float partialTicks) {
    Module module = button.getModule();
    boolean on = module instanceof ToggleableModule tm && tm.isRunning();
    int x = (int) button.getX();
    int y = (int) button.getY();
    int w = button.getWidth();
    int h = button.getHeight();
    int bg = on ? 0xFF101A14 : 0xFF121216;
    RenderMethods.drawRect(x, y, x + w, y + h, bg);
    RenderMethods.drawRect(x, y, x + 2, y + h, on ? 0xFF22FF66 : 0xFF333338);
    button.drawScreen(mouseX, mouseY, partialTicks);
  }

  @Override
  public boolean mouseClicked(MouseButtonEvent event, boolean bool) {
    int mouseX = (int) event.x();
    int mouseY = (int) event.y();
    int clickedButton = event.button();

    // Sidebar category hit test (must mirror render layout).
    int winX = 30;
    int winY = 24;
    int catY = winY + 44;
    for (ModuleType type : ModuleType.values()) {
      if (type == ModuleType.CLIENT) continue;
      if (mouseX >= winX + 6
          && mouseX <= winX + SIDEBAR_W - 6
          && mouseY >= catY
          && mouseY <= catY + 16) {
        selected = type;
        return true;
      }
      catY += 19;
    }
    if (mouseX >= winX + 6
        && mouseX <= winX + SIDEBAR_W - 6
        && mouseY >= catY
        && mouseY <= catY + 16) {
      selected = ModuleType.CLIENT;
      return true;
    }

    for (ModuleButton button : visible()) {
      int bx = (int) button.getX();
      int by = (int) button.getY();
      // Only route clicks inside the card bounds (headers included).
      if (mouseX >= bx
          && mouseX <= bx + button.getWidth()
          && mouseY >= by
          && mouseY <= by + button.getHeight()) {
        button.mouseClicked(mouseX, mouseY, clickedButton);
      }
    }
    return super.mouseClicked(event, bool);
  }

  @Override
  public boolean mouseReleased(MouseButtonEvent event) {
    for (ModuleButton button : visible()) {
      button.mouseReleased((int) event.x(), (int) event.y(), event.button());
    }
    return super.mouseReleased(event);
  }

  @Override
  public boolean keyPressed(KeyEvent event) {
    int key = event.key();
    if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
      search = search.substring(0, search.length() - 1);
      return true;
    }
    if (key == GLFW.GLFW_KEY_ESCAPE) {
      search = "";
      return super.keyPressed(event);
    }
    String name = GLFW.glfwGetKeyName(key, event.scancode());
    if (name != null && name.length() == 1 && search.length() < 24) {
      search += name.toLowerCase();
      return true;
    }
    if (key == GLFW.GLFW_KEY_SPACE && search.length() < 24) {
      search += " ";
      return true;
    }
    return super.keyPressed(event);
  }

  @Override
  public void init() {
    rebuild();
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }
}
