package me.larp.client.module.impl.toggle.render.clickgui.item;

import java.util.ArrayList;
import java.util.List;
import me.larp.api.minecraft.render.RenderMethods;
import me.larp.api.minecraft.render.font.FontUtil;
import me.larp.client.core.Larp;
import me.larp.client.module.Module;
import me.larp.client.module.ToggleableModule;
import me.larp.client.module.impl.toggle.render.ClickGui;
import me.larp.client.module.impl.toggle.render.clickgui.Panel;
import me.larp.client.module.impl.toggle.render.clickgui.item.properties.BooleanButton;
import me.larp.client.module.impl.toggle.render.clickgui.item.properties.EnumButton;
import me.larp.client.module.impl.toggle.render.clickgui.item.properties.NumberSlider;
import me.larp.client.properties.EnumProperty;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2fStack;

public class ModuleButton extends Button {
  private final Module module;
  private final List<Item> topLevelItems = new ArrayList<>();
  private boolean subOpen;
  private int progress;
  private float gearRotation;
  private float gearBrightness = 0.5f;
  private int gearAnimFrame = 2;
  private int gearAnimTimer = 0;

  public ModuleButton(Module module) {
    super(module.getLabel());
    this.module = module;
    this.progress = 0;
    if (!module.getProperties().isEmpty()) {
      BooleanButton lastParent = null;
      for (Property<?> property : module.getProperties()) {
        BooleanButton btn = createPropertyButton(property, false);
        if (btn != null) {
          topLevelItems.add(btn);
          lastParent = btn;
        } else {
          Item item = createOtherPropertyButton(property, false);
          if (item != null) {
            topLevelItems.add(item);
          }
          lastParent = null;
        }
        for (Property<?> childProp : property.getChildren()) {
          Item childItem = createOtherPropertyButton(childProp, true);
          if (childItem == null) {
            childItem = createPropertyButtonRaw(childProp, true);
          }
          if (childItem != null) {
            if (lastParent != null) {
              lastParent.addChildItem(childItem);
            } else {
              topLevelItems.add(childItem);
            }
          }
        }
      }
    }
  }

  private BooleanButton createPropertyButton(Property<?> property, boolean child) {
    if (property.getValue() instanceof Boolean) {
      return new BooleanButton(property, module, child);
    }
    return null;
  }

  private Item createPropertyButtonRaw(Property<?> property, boolean child) {
    if (property.getValue() instanceof Boolean) {
      return new BooleanButton(property, module, child);
    }
    return null;
  }

  private Item createOtherPropertyButton(Property<?> property, boolean child) {
    if (property instanceof EnumProperty) {
      return new EnumButton((EnumProperty) property, child);
    }
    if (property instanceof NumberProperty) {
      return new NumberSlider((NumberProperty) property, child);
    }
    return null;
  }

  public Module getModule() {
    return this.module;
  }

  public boolean isSubOpen() {
    return this.subOpen;
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    super.drawScreen(mouseX, mouseY, partialTicks);
    if (!this.topLevelItems.isEmpty()) {
      var guiMod = Larp.getInstance().getModuleManager().getModuleByAlias("clickgui");
      boolean showGear =
          !(guiMod instanceof me.larp.client.module.impl.toggle.render.ClickGui cg)
              || cg.showGear.getValue();

      if (showGear) {
        var guiClick =
            guiMod instanceof me.larp.client.module.impl.toggle.render.ClickGui cg2
                ? cg2
                : null;
        ClickGui.GearMode gearMode =
            guiClick != null ? guiClick.getGearMode() : ClickGui.GearMode.IMAGE;

        if (gearMode == ClickGui.GearMode.TEXT) {
          this.gearAnimTimer++;
          if (this.gearAnimTimer >= 4) {
            this.gearAnimTimer = 0;
            if (this.subOpen && this.gearAnimFrame > 0) {
              this.gearAnimFrame--;
            } else if (!this.subOpen && this.gearAnimFrame < 2) {
              this.gearAnimFrame++;
            }
          }
          String dots =
              switch (this.gearAnimFrame) {
                case 2 -> "...";
                case 1 -> "..";
                default -> ".";
              };
          int textW = FontUtil.getStringWidth(dots);
          FontUtil.drawString(dots, this.x + this.width - textW - 2.0f, this.y + 4.0f, 0xFFCCCCCC);
        } else {
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
        }
      }

      if (this.subOpen) {
        float cy = this.y + 16.0f;
        ++progress;
        for (Item item : topLevelItems) {
          item.setLocation(this.x + 1.0f, cy);
          item.setWidth(this.width - 9);
          item.drawScreen(mouseX, mouseY, partialTicks);
          cy += item.getHeight() + 1;
        }
      }
    }
  }

  @Override
  public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    if (!this.topLevelItems.isEmpty()) {
      if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
        this.subOpen = !this.subOpen;
        this.gearAnimTimer = 0;
        if (this.subOpen) {
          this.gearAnimFrame = 2;
        } else {
          this.gearAnimFrame = 0;
        }
      }
      if (this.subOpen) {
        for (Item item : topLevelItems) {
          item.mouseClicked(mouseX, mouseY, mouseButton);
        }
      }
    }
  }

  @Override
  public int getHeight() {
    if (this.subOpen) {
      int height = 15;
      for (Item item : topLevelItems) {
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
