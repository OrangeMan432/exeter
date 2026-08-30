package me.friendly.exeter.module.impl.toggle.render.clickgui.item;

import java.util.ArrayList;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.clickgui.Panel;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties.BooleanButton;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties.EnumButton;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties.NumberSlider;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2fStack;

public class ModuleButton extends Button {
  private final Module module;
  private java.util.List<Item> items = new ArrayList<Item>();
  private boolean subOpen;
  private int progress;
  private float gearRotation;
  private float gearBrightness = 0.5f;

  public ModuleButton(Module module) {
    super(module.getLabel());
    this.module = module;
    this.progress = 0;
    if (!module.getProperties().isEmpty()) {
      for (Property<?> property : module.getProperties()) {
        if (property.getValue() instanceof Boolean) {
          this.items.add(new BooleanButton(property, module));
        }
        if (property instanceof EnumProperty) {
          this.items.add(new EnumButton((EnumProperty) property));
        }
        if (property instanceof NumberProperty) {
          this.items.add(new NumberSlider((NumberProperty) property));
        }
        if (!(property.getValue() instanceof NumberProperty)) continue;
      }
    }
  }

  public static float calculateRotation(float var0) {
    if ((var0 %= 360.0F) >= 180.0F) {
      var0 -= 360.0F;
    }

    if (var0 < -180.0F) {
      var0 += 360.0F;
    }

    return var0;
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    super.drawScreen(mouseX, mouseY, partialTicks);
    if (!this.items.isEmpty()) {
      // FontUtil.drawString("...", this.x - 1.0f + (float)this.width - 8.0f, this.y - 2.0f, -1);//
      // remove this, its not in future

      int gx = (int) getX() + getWidth() - 12;
      int gy = (int) getY() + 3;

      float target = this.subOpen ? 1.0f : 0.5f;
      this.gearBrightness += (target - this.gearBrightness) * 0.05f;

      this.gearRotation += this.gearBrightness - 0.5f;

      int c = (int) (this.gearBrightness * 255.0f);
      int tintColor = 0xFF000000 | (c << 16) | (c << 8) | c;

      Matrix3x2fStack pose = RenderMethods.guiGraphics.pose();
      pose.pushMatrix();
      pose.rotateAbout(this.gearRotation * (float) Math.PI / 180.0f, gx + 5, gy + 5);
      RenderMethods.guiGraphics.blit(
          RenderPipelines.GUI_TEXTURED,
          Panel.GEAR_ID,
          gx,
          gy,
          0.0f,
          0.0f,
          10,
          10,
          128,
          128,
          128,
          128,
          tintColor);
      pose.popMatrix();

      if (this.subOpen) {
        float height = 1.0f;
        ++progress;
        for (Item item : items) {
          item.setLocation(this.x + 1.0f, this.y + (height += 15.0f));
          item.setHeight(15);
          item.setWidth(this.width - 9);
          item.drawScreen(mouseX, mouseY, partialTicks);
        }
      }
    }
  }

  @Override
  public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    if (!this.items.isEmpty()) {
      if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
        this.subOpen = !this.subOpen;
        //
        // Minecraft.getInstance().getSoundHandler().playSound(PositionedSoundRecord.createPositionedSoundRecord(new ResourceLocation("random.click"), 1.0f));
      }
      if (this.subOpen) {
        for (Item item : items) {
          item.mouseClicked(mouseX, mouseY, mouseButton);
        }
      }
    }
  }

  @Override
  public int getHeight() {
    if (this.subOpen) {
      int height = 15;
      for (Item item : items) {
        height += item.getHeight() + 1;
      }
      return height + 2;
    }
    return 15;
  }

  @Override
  public void toggle() {
    if (this.module instanceof ToggleableModule) {
      ((ToggleableModule) this.module).toggle();
    }
  }

  @Override
  public boolean getState() {
    if (this.module instanceof ToggleableModule) {
      return ((ToggleableModule) this.module).isRunning();
    }
    return true;
  }
}
