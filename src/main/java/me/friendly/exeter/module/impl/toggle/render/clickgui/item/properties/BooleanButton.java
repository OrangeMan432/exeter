package me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties;

import java.util.Arrays;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.Button;
import me.friendly.exeter.properties.Property;
// import net.minecraft.client.resources.sounds.PositionedSoundRecord;
// import net.minecraft.resources.ResourceLocation;

public class BooleanButton extends Button {
  private Property property;
  private Module module;

  public BooleanButton(Property property, Module module) {
    super(property.getAliases()[0]);
    this.property = property;
    this.module = module;
    this.width = 15;
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    //        RenderMethods.drawRect(this.x, this.y, this.x + (float)this.width + 7.4f, this.y +
    // (float)this.height, this.getState() ? (!this.isHovering(mouseX, mouseY) ? 2012955202 :
    // -1711586750) : (!this.isHovering(mouseX, mouseY) ? 0x11555555 : -2007673515));
    RenderMethods.drawRect(
        this.x,
        this.y,
        this.x + (float) this.width + 7.4f,
        this.y + (float) this.height,
        this.getState()
            ? (!this.isHovering(mouseX, mouseY)
                ? Colors.getClientColorCustomAlpha(77)
                : Colors.getClientColorCustomAlpha(77))
            : (!this.isHovering(mouseX, mouseY) ? 0x11555555 : -2007673515));
    FontUtil.drawString(
        this.getLabel(), this.x + 2.0f, this.y + 4.0f, this.getState() ? -1 : -5592406);
  }

  @Override
  public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    if (this.isHovering(mouseX, mouseY)) {
      //
      // Minecraft.getInstance().getSoundHandler().playSound(PositionedSoundRecord.createPositionedSoundRecord(new ResourceLocation("random.click"), 1.0f));
    }
  }

  @Override
  public int getHeight() {
    return 15;
  }

  @Override
  public void toggle() {
    this.property.setValue((Boolean) this.property.getValue() == false);

    if (this.module instanceof ToggleableModule
        && Arrays.asList(this.property.getAliases()).contains("Drawn")) {
      ((ToggleableModule) this.module).toggleDrawn();
    }
  }

  @Override
  public boolean getState() {
    return (Boolean) this.property.getValue();
  }
}
