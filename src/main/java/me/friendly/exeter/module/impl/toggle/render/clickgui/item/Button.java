package me.friendly.exeter.module.impl.toggle.render.clickgui.item;

import com.mojang.blaze3d.platform.InputConstants;
import java.awt.*;
import me.friendly.api.interfaces.Labeled;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.module.impl.toggle.render.clickgui.ClickGui;
import me.friendly.exeter.module.impl.toggle.render.clickgui.Panel;

// import net.minecraft.client.resources.sounds.PositionedSoundRecord;
// import net.minecraft.resources.ResourceLocation;

public class Button extends Item implements Labeled {
  private boolean state;

  public Button(String label) {
    super(label);
    this.height = 15;
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    var guiMod = Exeter.getInstance().getModuleManager().getModuleByAlias("clickgui");
    boolean useGradient =
        !(guiMod instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui cg)
            || cg.showGradient.getValue();
    me.friendly.exeter.module.impl.toggle.render.ClickGui cg =
        guiMod instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui c ? c : null;
    boolean rolling = cg != null && cg.rollingRainbow.getValue();
    boolean horizontal =
        rolling
            && cg.rollingDirection.getValue()
                == me.friendly.exeter.module.impl.toggle.render.ClickGui.RollingDirection
                    .HORIZONTAL;
    boolean inverse = rolling && cg.rollingInverse.getValue();
    var window = net.minecraft.client.Minecraft.getInstance().getWindow();
    int screenH = window.getGuiScaledHeight();
    int screenW = window.getGuiScaledWidth();
    int bx = (int) this.x;
    int by = (int) this.y;
    int bh = (int) (this.y + this.height);

    int topColor;
    int bottomColor;
    if (rolling) {
      boolean hovered = this.isHovering(mouseX, mouseY);
      if (this.getState()) {
        topColor =
            Colors.rollingSample(horizontal, inverse, bx, by, screenW, screenH, hovered ? 44 : 88);
        bottomColor =
            Colors.rollingSample(horizontal, inverse, bx, bh, screenW, screenH, hovered ? 77 : 55);
      } else {
        topColor =
            Colors.rollingSample(horizontal, inverse, bx, by, screenW, screenH, hovered ? 55 : 33);
        bottomColor =
            Colors.rollingSample(horizontal, inverse, bx, bh, screenW, screenH, hovered ? 66 : 33);
      }
    } else {
      topColor =
          this.getState()
              ? (!this.isHovering(mouseX, mouseY)
                  ? Colors.getClientColorCustomAlpha(88)
                  : Colors.getClientColorCustomAlpha(44))
              : (!this.isHovering(mouseX, mouseY)
                  ? Colors.getDarkerClientColorCustomAlpha(77)
                  : Colors.getDarkerClientColorCustomAlpha(33));
      bottomColor =
          this.getState()
              ? (!this.isHovering(mouseX, mouseY)
                  ? Colors.getClientColorCustomAlpha(55)
                  : Colors.getClientColorCustomAlpha(77))
              : (!this.isHovering(mouseX, mouseY)
                  ? Colors.getDarkerClientColorCustomAlpha(55)
                  : Colors.getDarkerClientColorCustomAlpha(66));
    }

    if (horizontal && rolling) {
      boolean hovered = this.isHovering(mouseX, mouseY);
      int alpha = this.getState() ? (hovered ? 66 : 77) : (hovered ? 55 : 44);
      RenderMethods.drawHorizontalSpectrumRect(
          this.x,
          this.y,
          this.x + (float) this.width,
          this.y + (float) this.height,
          sx -> Colors.rollingSample(true, inverse, sx, by, screenW, screenH, alpha));
    } else if (useGradient) {
      RenderMethods.drawGradientRect(
          this.x,
          this.y,
          this.x + (float) this.width,
          this.y + (float) this.height,
          topColor,
          bottomColor);
    } else {
      RenderMethods.drawRect(
          this.x, this.y, this.x + (float) this.width, this.y + (float) this.height, topColor);
    }

    FontUtil.drawString(
        this.getLabel(), this.x + 2.0f, this.y + 4.0f, this.getState() ? -1 : -5592406);
  }

  @Override
  public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    if (mouseButton == InputConstants.MOUSE_BUTTON_LEFT && this.isHovering(mouseX, mouseY)) {
      this.state = !this.state;
      this.toggle();
      //
      // Minecraft.getInstance().getSoundHandler().playSound(PositionedSoundRecord.createPositionedSoundRecord(new ResourceLocation("random.click"), 1.0f));
    }
  }

  public void toggle() {}

  public boolean getState() {
    return this.state;
  }

  /**
   * Client accent at this row, or the rolling hue when enabled. Subclass painters that draw their
   * own backgrounds should route through here.
   */
  protected int accentColor(int alpha) {
    var guiMod = Exeter.getInstance().getModuleManager().getModuleByAlias("clickgui");
    if (guiMod instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui cg
        && cg.rollingRainbow.getValue()) {
      var window = net.minecraft.client.Minecraft.getInstance().getWindow();
      boolean horizontal =
          cg.rollingDirection.getValue()
              == me.friendly.exeter.module.impl.toggle.render.ClickGui.RollingDirection.HORIZONTAL;
      return Colors.rollingSample(
          horizontal,
          cg.rollingInverse.getValue(),
          (int) this.x,
          (int) this.y,
          window.getGuiScaledWidth(),
          window.getGuiScaledHeight(),
          alpha);
    }
    return Colors.getClientColorCustomAlpha(alpha);
  }

  /** Horizontal rolling is on (implies rolling itself is on). */
  protected boolean rollingHorizontal() {
    var guiMod = Exeter.getInstance().getModuleManager().getModuleByAlias("clickgui");
    return guiMod instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui cg
        && cg.rollingRainbow.getValue()
        && cg.rollingDirection.getValue()
            == me.friendly.exeter.module.impl.toggle.render.ClickGui.RollingDirection.HORIZONTAL;
  }

  /** Rolling hue sampled at an explicit position, for spectrum strips. */
  protected int accentColorAt(int x, int y, int screenW, int screenH, int alpha) {
    var guiMod = Exeter.getInstance().getModuleManager().getModuleByAlias("clickgui");
    boolean inverse = false;
    if (guiMod instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui cg) {
      inverse = cg.rollingInverse.getValue();
    }
    return Colors.rollingSample(true, inverse, x, y, screenW, screenH, alpha);
  }

  @Override
  public int getHeight() {
    return 15;
  }

  protected boolean isHovering(int mouseX, int mouseY) {
    for (Panel panel : ClickGui.getClickGui().getPanels()) {
      if (!panel.drag) continue;
      return false;
    }
    return (float) mouseX >= this.getX()
        && (float) mouseX <= this.getX() + (float) this.getWidth()
        && (float) mouseY >= this.getY()
        && (float) mouseY <= this.getY() + (float) this.height;
  }
}
