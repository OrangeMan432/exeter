package me.friendly.exeter.window;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.config.ExeterConfig;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.module.impl.toggle.client.WindowsModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class WindowScreen extends Screen {
  private final List<Window> windows = new ArrayList<>();
  private static final int TASKBAR_HEIGHT = 20;

  public WindowScreen() {
    super(Component.literal("Windows"));
  }

  public void addWindow(Window window) {
    windows.add(window);
  }

  public List<Window> getWindows() {
    return windows;
  }

  @Override
  public void extractRenderState(
      GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
    RenderMethods.guiGraphics = guiGraphics;
    RenderMethods.drawGradientRect(0, 0, this.width, this.height, 0x80000000, 0x40000000);

    for (Window window : windows) {
      if (!window.isHidden()) {
        window.render(mouseX, mouseY, partialTicks);
      }
    }

    renderTaskbar(mouseX, mouseY);
  }

  private void renderTaskbar(int mouseX, int mouseY) {
    int taskbarY = this.height - TASKBAR_HEIGHT;
    RenderMethods.drawRect(0, taskbarY, this.width, this.height, 0xDD111111);
    RenderMethods.drawRect(
        0, taskbarY, this.width, taskbarY + 1, Colors.getClientColorCustomAlpha(180));

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
            hovered ? Colors.getClientColorCustomAlpha(200) : Colors.getClientColorCustomAlpha(140);
      } else {
        bgColor = hovered ? 0xFF444444 : 0xFF2A2A2A;
      }

      RenderMethods.drawRect(
          btnX, taskbarY + 3, btnX + btnWidth, taskbarY + TASKBAR_HEIGHT - 3, bgColor);

      int textColor = isActive ? 0xFFFFFFFF : 0xFF888888;
      FontUtil.drawString(window.getTitle(), btnX + 6, taskbarY + 6, textColor);

      btnX += btnWidth + 4;
    }
  }

  @Override
  public boolean mouseClicked(MouseButtonEvent event, boolean bool) {
    int mouseX = (int) event.x();
    int mouseY = (int) event.y();
    int button = event.button();

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
          return true;
        }
        btnX += btnWidth + 4;
      }
      return true;
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
        return true;
      }
    }

    for (Window w : windows) {
      w.setFocused(false);
    }
    return super.mouseClicked(event, bool);
  }

  @Override
  public boolean mouseReleased(MouseButtonEvent event) {
    int button = event.button();
    for (Window window : windows) {
      if (!window.isHidden()) {
        window.mouseReleased(button);
      }
    }
    return super.mouseReleased(event);
  }

  @Override
  public void mouseMoved(double mouseX, double mouseY) {}

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollDelta) {
    for (int i = windows.size() - 1; i >= 0; i--) {
      Window window = windows.get(i);
      if (!window.isHidden() && window.isHovered(mouseX, mouseY)) {
        window.mouseScrolled(mouseX, mouseY, scrollDelta);
        return true;
      }
    }
    return super.mouseScrolled(mouseX, mouseY, scrollX, scrollDelta);
  }

  @Override
  public boolean keyPressed(KeyEvent event) {
    int key = event.key();
    if (key == InputConstants.KEY_GRAVE || key == InputConstants.KEY_ESCAPE) {
      minecraft.gui.setScreen(null);
      return true;
    }
    for (Window window : windows) {
      if (!window.isHidden() && window.isFocused()) {
        if (window.keyPressed(event)) {
          return true;
        }
      }
    }
    return super.keyPressed(event);
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void init() {}

  @Override
  public void removed() {
    super.removed();
    Module module = Exeter.getInstance().getModuleManager().getModuleByAlias("windows");
    if (module instanceof WindowsModule windowsModule) {
      windowsModule.capturePositions();
      ExeterConfig.getInstance().saveModule(windowsModule);
    }
  }
}
