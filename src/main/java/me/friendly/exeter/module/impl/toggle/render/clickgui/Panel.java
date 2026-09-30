package me.friendly.exeter.module.impl.toggle.render.clickgui;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.interfaces.Labeled;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.Item;
import net.minecraft.client.gui.DrawableHelper;

public class Panel extends DrawableHelper implements Labeled {
  private final String label;
  private int x;
  private int y;
  private final int width;
  private final int headerHeight = 14;
  private boolean open;
  private boolean dragging;
  private int dragOffsetX;
  private int dragOffsetY;
  private final List<Item> items = new ArrayList<Item>();

  public Panel(String label, int x, int y, boolean open) {
    this.label = label;
    this.x = x;
    this.y = y;
    this.width = 90;
    this.open = open;
  }

  public void addButton(Item item) {
    this.items.add(item);
  }

  public List<Item> getItems() {
    return this.items;
  }

  public boolean getOpen() {
    return this.open;
  }

  @Override
  public String getLabel() {
    return this.label;
  }

  public int getX() {
    return this.x;
  }

  public int getY() {
    return this.y;
  }

  public int getWidth() {
    return this.width;
  }

  public void setX(int x) {
    this.x = x;
  }

  public void setY(int y) {
    this.y = y;
  }

  public boolean containsMouse(int mouseX, int mouseY) {
    return mouseX >= this.x
        && mouseX <= this.x + this.width
        && mouseY >= this.y
        && mouseY <= this.y + getTotalHeight();
  }

  public boolean titleHovered(int mouseX, int mouseY) {
    return mouseX >= this.x
        && mouseX <= this.x + this.width
        && mouseY >= this.y
        && mouseY <= this.y + this.headerHeight;
  }

  private int getTotalHeight() {
    int total = headerHeight;
    if (open) {
      for (Item item : items) {
        total += item.getHeight() + 1;
      }
    }
    return total;
  }

  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    if (dragging) {
      // Position follows in mouseClicked path via offsets; updated on move.
    }
    fill(this.x, this.y, this.x + this.width, this.y + this.headerHeight, 0xDD222222);
    FontUtil.drawString(this.label, this.x + 3.0f, this.y + 3.0f, -1);

    if (open) {
      int itemY = this.y + this.headerHeight + 1;
      for (Item item : items) {
        item.setLocation(this.x + 1, itemY);
        item.setWidth(this.width - 2);
        item.drawScreen(mouseX, mouseY, partialTicks);
        itemY += item.getHeight() + 1;
      }
      fill(this.x, itemY - 1, this.x + this.width, itemY, 0xFF222222);
    }
  }

  public void onMouseMove(int mouseX, int mouseY) {
    if (dragging) {
      this.x = mouseX - dragOffsetX;
      this.y = mouseY - dragOffsetY;
    }
  }

  public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    if (mouseButton == 0 && titleHovered(mouseX, mouseY)) {
      dragging = true;
      dragOffsetX = mouseX - this.x;
      dragOffsetY = mouseY - this.y;
      return;
    }
    if (mouseButton == 1 && titleHovered(mouseX, mouseY)) {
      open = !open;
      return;
    }
    if (open) {
      for (Item item : items) {
        item.mouseClicked(mouseX, mouseY, mouseButton);
      }
    }
  }

  public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
    dragging = false;
    if (open) {
      for (Item item : items) {
        item.mouseReleased(mouseX, mouseY, releaseButton);
      }
    }
  }
}
