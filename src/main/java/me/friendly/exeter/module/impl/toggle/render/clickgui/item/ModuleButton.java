package me.friendly.exeter.module.impl.toggle.render.clickgui.item;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties.ActionButton;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties.BooleanButton;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties.EnumButton;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties.NumberSlider;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties.PopupButton;
import me.friendly.exeter.properties.ActionProperty;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.PopupProperty;
import me.friendly.exeter.properties.Property;

public class ModuleButton extends Button {
  private final Module module;
  private final List<Item> topLevelItems = new ArrayList<Item>();
  private boolean subOpen;
  private int gearAnimFrame = 2;
  private int gearAnimTimer = 0;

  public ModuleButton(Module module) {
    super(module.getLabel());
    this.module = module;
    for (Property<?> property : module.getProperties()) {
      Item item = createItem(property);
      if (item != null) {
        topLevelItems.add(item);
      }
    }
  }

  private Item createItem(Property<?> property) {
    if (property instanceof ActionProperty) {
      return new ActionButton((ActionProperty) property);
    }
    if (property instanceof PopupProperty) {
      return new PopupButton((PopupProperty) property);
    }
    if (property instanceof EnumProperty) {
      return new EnumButton((EnumProperty<?>) property);
    }
    if (property instanceof NumberProperty) {
      return new NumberSlider((NumberProperty<?>) property);
    }
    if (property.getValue() instanceof Boolean) {
      return new BooleanButton((Property<Boolean>) property, module);
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

  private boolean isRunning() {
    if (module instanceof me.friendly.api.interfaces.Toggleable) {
      return ((me.friendly.api.interfaces.Toggleable) module).isRunning();
    }
    return true;
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    super.drawScreen(mouseX, mouseY, partialTicks);

    if (!topLevelItems.isEmpty()) {
      this.gearAnimTimer++;
      if (this.gearAnimTimer >= 4) {
        this.gearAnimTimer = 0;
        if (this.subOpen && this.gearAnimFrame > 0) {
          this.gearAnimFrame--;
        } else if (!this.subOpen && this.gearAnimFrame < 2) {
          this.gearAnimFrame++;
        }
      }
      String dots;
      if (this.gearAnimFrame == 2) {
        dots = "...";
      } else if (this.gearAnimFrame == 1) {
        dots = "..";
      } else {
        dots = ".";
      }
      int textW = FontUtil.getStringWidth(dots);
      FontUtil.drawString(
          dots, this.x + this.width - textW - 2.0f, this.y + 4.0f, 0xFFCCCCCC);
    }

    if (this.subOpen) {
      float cy = this.y + 16.0f;
      for (Item child : topLevelItems) {
        child.setLocation(this.x + 1.0f, cy);
        child.setWidth(this.width - 9);
        child.drawScreen(mouseX, mouseY, partialTicks);
        cy += child.getHeight() + 1;
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
        for (Item child : topLevelItems) {
          child.mouseClicked(mouseX, mouseY, mouseButton);
        }
      }
    }
  }

  @Override
  public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
    for (Item child : topLevelItems) {
      child.mouseReleased(mouseX, mouseY, releaseButton);
    }
  }

  @Override
  public void toggle() {
    if (module instanceof me.friendly.exeter.module.ToggleableModule) {
      ((me.friendly.exeter.module.ToggleableModule) module).toggle();
    }
  }

  @Override
  public int getHeight() {
    if (this.subOpen) {
      int height = 15;
      for (Item child : topLevelItems) {
        height += child.getHeight() + 1;
      }
      return height + 2;
    }
    return 15;
  }

  @Override
  public boolean getState() {
    return isRunning();
  }
}
