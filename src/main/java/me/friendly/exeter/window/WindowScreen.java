package me.friendly.exeter.window;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.module.impl.toggle.client.WindowsModule;
import net.minecraft.client.gui.screen.Screen;

public final class WindowScreen extends Screen {
  private final List<Window> windows = new ArrayList<Window>();
  private static final int TASKBAR_HEIGHT = 20;

  public WindowScreen() {
    super();
  }

  public void addWindow(Window window) {
    windows.add(window);
  }

  public List<Window> getWindows() {
    return windows;
  }

  private static boolean isBackgroundEnabled() {
    if (Exeter.getInstance() == null) {
      return true;
    }
    Module module = Exeter.getInstance().getModuleManager().getModuleByAlias("clickgui");
    if (!(module instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui)) {
      return true;
    }
    me.friendly.exeter.module.impl.toggle.render.ClickGui gui =
        (me.friendly.exeter.module.impl.toggle.render.ClickGui) module;
    return gui.showBackground.getValue().booleanValue();
  }

  @Override
  public void render(int mouseX, int mouseY, float partialTicks) {
    super.render(mouseX, mouseY, partialTicks);
    if (isBackgroundEnabled()) {
      fillGradient(0, 0, this.width, this.height, 0x80000000, 0x40000000);
    }

    for (Window window : windows) {
      if (!window.isHidden()) {
        window.render(mouseX, mouseY, partialTicks);
      }
    }

    renderTaskbar(mouseX, mouseY);
  }

  private void renderTaskbar(int mouseX, int mouseY) {
    int taskbarY = this.height - TASKBAR_HEIGHT;
    fill(0, taskbarY, this.width, this.height, 0xDD111111);

    int btnX = 4;
    for (Window window : windows) {
      int btnWidth = FontUtil.getStringWidth(window.getTitle()) + 12;
      boolean hovered =
          mouseX >= btnX
              && mouseX <= btnX + btnWidth
              && mouseY >= taskbarY + 3
              && mouseY <= taskbarY + TASKBAR_HEIGHT - 3;
      boolean isActive = !window.isHidden();

      int bgColor;
      if (isActive) {
        bgColor =
            hovered
                ? Colors.getClientColorCustomAlpha(200)
                : Colors.getClientColorCustomAlpha(140);
      } else {
        bgColor = hovered ? 0xFF444444 : 0xFF2A2A2A;
      }

      fill(btnX, taskbarY + 3, btnX + btnWidth, taskbarY + TASKBAR_HEIGHT - 3, bgColor);

      int textColor = isActive ? 0xFFFFFFFF : 0xFF888888;
      FontUtil.drawString(window.getTitle(), btnX + 6.0f, taskbarY + 6.0f, textColor);

      btnX += btnWidth + 4;
    }
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int button) {
    int taskbarY = this.height - TASKBAR_HEIGHT;
    if (mouseY >= taskbarY) {
      int btnX = 4;
      for (Window window : windows) {
        int btnWidth = FontUtil.getStringWidth(window.getTitle()) + 12;
        if (mouseX >= btnX && mouseX <= btnX + btnWidth) {
          window.setHidden(!window.isHidden());
          if (!window.isHidden()) {
            for (Window w : windows) {
              w.setFocused(w == window);
            }
          }
          return;
        }
        btnX += btnWidth + 4;
      }
      return;
    }

    for (int i = windows.size() - 1; i >= 0; i--) {
      Window window = windows.get(i);
      if (window.isHidden()) continue;
      if (window.isHovered(mouseX, mouseY)) {
        windows.remove(i);
        windows.add(window);
        for (Window w : windows) {
          w.setFocused(w == window);
        }
        window.mouseClicked(mouseX, mouseY, button);
        return;
      }
    }

    for (Window w : windows) {
      w.setFocused(false);
    }
    super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  protected void mouseReleased(int mouseX, int mouseY, int releaseButton) {
    for (Window window : windows) {
      if (!window.isHidden()) {
        window.mouseReleased(releaseButton);
      }
    }
    super.mouseReleased(mouseX, mouseY, releaseButton);
  }

  @Override
  protected void keyPressed(char typedChar, int keyCode) {
    if (keyCode == 1) {
      if (this.minecraft != null) {
        this.minecraft.setScreen(null);
      }
      return;
    }
    for (Window window : windows) {
      if (!window.isHidden() && window.isFocused()) {
        if (window.keyTyped(typedChar, keyCode)) {
          return;
        }
      }
    }
    super.keyPressed(typedChar, keyCode);
  }

  @Override
  public boolean shouldPause() {
    return false;
  }

  public void removed() {
    Module module = Exeter.getInstance().getModuleManager().getModuleByAlias("windows");
    if (module instanceof WindowsModule) {
      WindowsModule windowsModule = (WindowsModule) module;
      windowsModule.capturePositions();
      if (Exeter.getInstance().getExeterConfig() != null) {
        Exeter.getInstance().getExeterConfig().saveModule(windowsModule);
      }
    }
  }
}
