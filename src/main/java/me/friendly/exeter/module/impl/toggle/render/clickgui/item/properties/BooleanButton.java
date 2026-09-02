package me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.module.impl.toggle.client.Debug;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.Button;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.Item;
import me.friendly.exeter.properties.Property;

public class BooleanButton extends Button {
  private Property property;
  private Module module;
  private final boolean child;
  private boolean childrenOpen;
  private final List<Item> children = new ArrayList<>();
  private int animFrame = 2;
  private int animTimer = 0;

  private static final float CHILD_OFFSET = 1.0f;

  public BooleanButton(Property property, Module module, boolean child) {
    super(property.getAliases()[0]);
    this.property = property;
    this.module = module;
    this.child = child;
    this.width = 15;
  }

  public void addChildItem(Item item) {
    this.children.add(item);
  }

  public boolean hasChildren() {
    return !this.children.isEmpty();
  }

  public boolean isChildrenOpen() {
    return this.childrenOpen;
  }

  public List<Item> getChildren() {
    return this.children;
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    float offsetX = child ? CHILD_OFFSET : 0.0f;
    RenderMethods.drawRect(
        this.x + offsetX,
        this.y,
        this.x + offsetX + (float) this.width + 7.4f,
        this.y + (float) this.height,
        this.getState()
            ? (!this.isHovering(mouseX, mouseY)
                ? Colors.getClientColorCustomAlpha(77)
                : Colors.getClientColorCustomAlpha(77))
            : (!this.isHovering(mouseX, mouseY) ? 0x11555555 : -2007673515));
    FontUtil.drawString(
        this.getLabel(), this.x + offsetX + 2.0f, this.y + 4.0f, this.getState() ? -1 : -5592406);

    if (hasChildren()) {
      this.animTimer++;
      if (this.animTimer >= 4) {
        this.animTimer = 0;
        if (this.childrenOpen && this.animFrame > 0) {
          this.animFrame--;
        } else if (!this.childrenOpen && this.animFrame < 2) {
          this.animFrame++;
        }
      }
      String dots =
          switch (this.animFrame) {
            case 2 -> "...";
            case 1 -> "..";
            default -> ".";
          };
      int textW = FontUtil.getStringWidth(dots);
      FontUtil.drawString(
          dots,
          this.x + offsetX + (float) this.width + 7.4f - textW - 2.0f,
          this.y + 4.0f,
          0xFFCCCCCC);
    }

    if (this.childrenOpen) {
      float cy = this.y + this.height + 1;
      for (Item childItem : this.children) {
        childItem.setLocation(this.x + offsetX + CHILD_OFFSET, cy);
        childItem.setWidth(this.width - (int) CHILD_OFFSET * 2);
        childItem.drawScreen(mouseX, mouseY, partialTicks);
        cy += childItem.getHeight() + 1;
      }
    }
  }

  @Override
  public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    if (hasChildren() && mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
      this.childrenOpen = !this.childrenOpen;
      this.animTimer = 0;
      if (this.childrenOpen) {
        this.animFrame = 2;
      } else {
        this.animFrame = 0;
      }
      return;
    }
    super.mouseClicked(mouseX, mouseY, mouseButton);
    if (this.childrenOpen) {
      for (Item childItem : this.children) {
        childItem.mouseClicked(mouseX, mouseY, mouseButton);
      }
    }
  }

  @Override
  public int getHeight() {
    if (this.childrenOpen) {
      int h = 15;
      for (Item childItem : this.children) {
        h += childItem.getHeight() + 1;
      }
      return h;
    }
    return 15;
  }

  @Override
  public void toggle() {
    this.property.setValue((Boolean) this.property.getValue() == false);

    if (this.module instanceof Debug debug) {
      debug.syncSettings();
    }

    if (this.module instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui guiMod) {
      if (Arrays.asList(this.property.getAliases()).contains("Arrow")) {
        guiMod.onArrowToggled();
      } else if (Arrays.asList(this.property.getAliases()).contains("Module Count")) {
        guiMod.onModuleCountToggled();
      }
    }
  }

  @Override
  public boolean getState() {
    return (Boolean) this.property.getValue();
  }
}
