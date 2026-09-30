package me.friendly.exeter.module.impl.toggle.render.clickgui.item;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties.ActionButton;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties.BooleanButton;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties.EnumButton;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties.NumberSlider;
import me.friendly.exeter.properties.ActionProperty;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.PopupProperty;
import me.friendly.exeter.properties.Property;

public class ModuleButton extends Button {
  private final Module module;
  private final List<Item> topLevelItems = new ArrayList<Item>();
  private boolean subOpen;

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
      return null;
    }
    if (property instanceof EnumProperty) {
      return new EnumButton((EnumProperty<?>) property);
    }
    if (property instanceof NumberProperty) {
      return new NumberSlider((NumberProperty<?>) property);
    }
    if (property.getValue() instanceof Boolean) {
      return new BooleanButton((Property<Boolean>) property);
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

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    boolean running =
        module instanceof me.friendly.api.interfaces.Toggleable
            && ((me.friendly.api.interfaces.Toggleable) module).isRunning();
    boolean hovered = isHovering(mouseX, mouseY);
    int bg;
    if (running) {
      bg = hovered ? 0xFFAA3333 : 0xFF771111;
    } else {
      bg = hovered ? 0xFF444444 : 0xFF222222;
    }
    fill((int) this.x, (int) this.y, (int) (this.x + this.width), (int) (this.y + this.height), bg);
    FontUtil.drawString(this.getLabel(), this.x + 2.0f, this.y + 2.0f, running ? -1 : -5592406);

    if (!topLevelItems.isEmpty()) {
      String gear = subOpen ? "-" : "+";
      FontUtil.drawString(
          gear,
          this.x + this.width - FontUtil.getStringWidth(gear) - 3.0f,
          this.y + 2.0f,
          0xFFCCCCCC);
    }

    if (subOpen) {
      float cy = this.y + this.height + 1;
      for (Item child : topLevelItems) {
        child.setLocation(this.x + 2, cy);
        child.setWidth(this.width - 4);
        child.drawScreen(mouseX, mouseY, partialTicks);
        cy += child.getHeight() + 1;
      }
    }
  }

  @Override
  public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    if (subOpen) {
      for (Item child : topLevelItems) {
        child.mouseClicked(mouseX, mouseY, mouseButton);
      }
    }
    if (!isHovering(mouseX, mouseY)) {
      return;
    }
    if (mouseButton == 0) {
      if (module instanceof me.friendly.api.interfaces.Toggleable) {
        ((me.friendly.api.interfaces.Toggleable) module).toggle();
      }
    } else if (mouseButton == 1 && !topLevelItems.isEmpty()) {
      subOpen = !subOpen;
    }
  }

  @Override
  public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
    for (Item child : topLevelItems) {
      child.mouseReleased(mouseX, mouseY, releaseButton);
    }
  }

  @Override
  public int getHeight() {
    if (!subOpen) {
      return 12;
    }
    int h = 12;
    for (Item child : topLevelItems) {
      h += child.getHeight() + 1;
    }
    return h;
  }

  @Override
  public boolean getState() {
    return module instanceof me.friendly.api.interfaces.Toggleable
        && ((me.friendly.api.interfaces.Toggleable) module).isRunning();
  }
}
