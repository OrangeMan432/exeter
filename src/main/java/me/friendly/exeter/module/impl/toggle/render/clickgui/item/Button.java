package me.friendly.exeter.module.impl.toggle.render.clickgui.item;

import com.mojang.blaze3d.platform.InputConstants;
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

    int topColor =
        this.getState()
            ? (!this.isHovering(mouseX, mouseY)
                ? Colors.getClientColorCustomAlpha(88)
                : Colors.getClientColorCustomAlpha(44))
            : (!this.isHovering(mouseX, mouseY)
                ? Colors.getDarkerClientColorCustomAlpha(77)
                : Colors.getDarkerClientColorCustomAlpha(33));
    int bottomColor =
        this.getState()
            ? (!this.isHovering(mouseX, mouseY)
                ? Colors.getClientColorCustomAlpha(55)
                : Colors.getClientColorCustomAlpha(77))
            : (!this.isHovering(mouseX, mouseY)
                ? Colors.getDarkerClientColorCustomAlpha(55)
                : Colors.getDarkerClientColorCustomAlpha(66));

    if (useGradient) {
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
