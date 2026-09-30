package me.friendly.exeter.module.impl.toggle.render.clickgui;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.interfaces.Labeled;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.Item;
import me.friendly.exeter.util.ClientColors;
import net.minecraft.client.gui.DrawableHelper;

public class Panel extends DrawableHelper implements Labeled {
  private final String label;
  private int x;
  private int y;
  private final int width;
  private final int height = 18;
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
        && mouseY <= this.y + this.headerHeight();
  }

  private int headerHeight() {
    return 12;
  }

  private int getTotalHeight() {
    int total = headerHeight();
    if (open) {
      for (Item item : items) {
        total += item.getHeight() + 1;
      }
    }
    return total;
  }

  private int getVisibleItemHeight() {
    int total = 0;
    for (Item item : items) {
      total += item.getHeight() + 1;
    }
    return total;
  }

  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    me.friendly.exeter.module.impl.toggle.render.ClickGui guiMod = getClickGuiModule();
    boolean showArrow = guiMod == null || guiMod.showArrow.getValue();

    int headerAccent = ClientColors.getClientColorCustomAlpha(77);
    fillGradient(this.x, this.y - 1, this.x + this.width, this.y + this.headerHeight() - 6,
        headerAccent, headerAccent);
    fill(this.x, this.y + this.headerHeight(),
        this.x + this.width, this.y + this.height + (this.open ? getVisibleItemHeight() - 2 : -1),
        0x77000000);
    FontUtil.drawString(this.label, this.x + 3.0f, (float) this.y + 1.5f, -1);

    if (showArrow) {
      String arrow = this.open ? "v" : "^";
      FontUtil.drawString(
          arrow, (float) (this.x + this.width - 10), (float) this.y + 1.5f, 0xFFCCCCCC);
    }

    if (this.open) {
      int itemY = this.getY() + this.headerHeight() - 1;
      for (Item item : getItems()) {
        item.setLocation((float) this.x + 2.0f, (float) itemY);
        item.setWidth(this.getWidth() - 4);
        item.drawScreen(mouseX, mouseY, partialTicks);
        itemY += item.getHeight() + 1;
      }
    }

    if (guiMod == null || guiMod.showBorder.getValue()) {
      float top = (float) this.y - 1.0f;
      float bottom =
          (float) this.y + this.height + (this.open ? getVisibleItemHeight() - 2 : -1);
      int accent = ClientColors.getClientColorCustomAlpha(77);
      fill(this.x - 1, (int) top - 1, this.x + this.width + 1, (int) top, accent);
      fill(this.x - 1, (int) bottom, this.x + this.width + 1, (int) bottom + 1, accent);
      fill(this.x - 1, (int) top, this.x, (int) bottom, accent);
      fill(this.x + this.width, (int) top, this.x + this.width + 1, (int) bottom, accent);
    }
  }

  private static me.friendly.exeter.module.impl.toggle.render.ClickGui getClickGuiModule() {
    if (me.friendly.exeter.core.Exeter.getInstance() == null) return null;
    me.friendly.exeter.module.Module module =
        me.friendly.exeter.core.Exeter.getInstance().getModuleManager().getModuleByAlias("clickgui");
    if (module instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui) {
      return (me.friendly.exeter.module.impl.toggle.render.ClickGui) module;
    }
    return null;
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
