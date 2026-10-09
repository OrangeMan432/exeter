package me.friendly.exeter.module.impl.toggle.render.clickgui.item;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.ClickGui;
import me.friendly.exeter.module.impl.toggle.render.clickgui.Panel;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties.ActionButton;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties.BooleanButton;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties.EnumButton;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties.NumberSlider;
import me.friendly.exeter.properties.ActionProperty;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.PopupProperty;
import me.friendly.exeter.properties.Property;
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
    if (property instanceof PopupProperty) {
      return null;
    }
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
    if (property instanceof ActionProperty action) {
      return new ActionButton(action);
    }
    if (property instanceof PopupProperty) {
      return new PopupButton((PopupProperty) property);
    }
    if (property instanceof EnumProperty) {
      return new EnumButton((EnumProperty) property, module, child);
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

  public void setSubOpen(boolean open) {
    this.subOpen = open;
  }

  public List<Item> getTopLevelItems() {
    return this.topLevelItems;
  }

  /**
   * Description of the hovered setting (or nested child) row, or null when the mouse is not over
   * one. Falls back to label plus value when the setting is undescribed.
   */
  public String getHoveredSettingDescription(int mouseX, int mouseY) {
    if (!this.subOpen) return null;
    for (Item item : topLevelItems) {
      String desc = hoveredSettingDescription(item, mouseX, mouseY);
      if (desc != null) return desc;
    }
    return null;
  }

  private static String hoveredSettingDescription(Item item, int mouseX, int mouseY) {
    if (!item.isVisible()) return null;
    if (item instanceof PropertyItem propertyItem
        && contains(item, mouseX, mouseY)
        && propertyItem.getProperty() != null) {
      Property<?> property = propertyItem.getProperty();
      String desc = property.getDescription();
      if (desc != null && !desc.isEmpty()) return desc;
      String label = property.getAliases().length > 0 ? property.getAliases()[0] : "?";
      if (property instanceof ActionProperty) return label;
      return label + ": " + String.valueOf(property.getValue());
    }
    if (item instanceof BooleanButton booleanButton && booleanButton.isChildrenOpen()) {
      for (Item child : booleanButton.getChildren()) {
        String desc = hoveredSettingDescription(child, mouseX, mouseY);
        if (desc != null) return desc;
      }
    }
    return null;
  }

  private static boolean contains(Item item, int mouseX, int mouseY) {
    return mouseX >= item.getX()
        && mouseX <= item.getX() + item.getWidth()
        && mouseY >= item.getY()
        && mouseY <= item.getY() + item.getHeight();
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    super.drawScreen(mouseX, mouseY, partialTicks);
    if (!this.topLevelItems.isEmpty()) {
      var guiMod = Exeter.getInstance().getModuleManager().getModuleByAlias("clickgui");
      boolean showGear =
          !(guiMod instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui cg)
              || cg.showGear.getValue();

      if (showGear) {
        var guiClick =
            guiMod instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui cg2
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
          if (!item.isVisible()) continue;
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
      if (mouseButton == InputConstants.MOUSE_BUTTON_RIGHT && this.isHovering(mouseX, mouseY)) {
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
          if (!item.isVisible()) continue;
          item.mouseClicked(mouseX, mouseY, mouseButton);
        }
      }
    }
  }

  @Override
  public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
    super.mouseReleased(mouseX, mouseY, releaseButton);
    if (this.subOpen) {
      for (Item item : topLevelItems) {
        if (!item.isVisible()) continue;
        item.mouseReleased(mouseX, mouseY, releaseButton);
      }
    }
  }

  @Override
  public int getHeight() {
    if (this.subOpen) {
      int height = 15;
      for (Item item : topLevelItems) {
        if (!item.isVisible()) continue;
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
