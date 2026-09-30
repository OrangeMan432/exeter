package me.friendly.exeter.module.impl.toggle.render.clickgui;

import java.util.ArrayList;
import me.friendly.api.interfaces.Labeled;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.Item;
import net.minecraft.client.gui.DrawableHelper;

public class Panel extends DrawableHelper implements Labeled {
  private final String label;
  private int angle;
  private int x;
  private int y;
  private int x2;
  private int y2;
  private int width;
  private int height;
  private boolean open;
  public boolean drag;
  private final ArrayList<Item> items = new ArrayList<Item>();

  public Panel(String label, int x, int y, boolean open) {
    this.label = label;
    this.x = x;
    this.y = y;
    this.angle = 180;
    this.width = 88;
    this.height = 18;
    this.open = open;
  }

  public void addButton(Item item) {
    this.items.add(item);
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

  public int getHeight() {
    return this.height;
  }

  public boolean getOpen() {
    return this.open;
  }

  public final ArrayList<Item> getItems() {
    return this.items;
  }

  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    this.drag(mouseX, mouseY);
    me.friendly.exeter.module.impl.toggle.render.ClickGui guiMod = getClickGuiModule();
    boolean showArrow = guiMod == null || guiMod.showArrow.getValue().booleanValue();

    float totalItemHeight = this.open ? (float) this.getTotalItemHeight() - 2.0f : 0.0f;
    fillGradient(
        this.x,
        (int) ((float) this.y - 1.5f),
        this.x + this.width,
        this.y + this.height - 6,
        Colors.getClientColorCustomAlpha(77),
        Colors.getClientColorCustomAlpha(77));
    fill(
        this.x,
        this.y + 12,
        this.x + this.width,
        this.y + this.height + (this.open ? (int) totalItemHeight : -1),
        0x77000000);
    FontUtil.drawString(this.getLabel(), (float) this.x + 3.0f, (float) this.y + 1.5f, -1);

    if (!open) {
      if (this.angle > 0) {
        this.angle -= 3;
      }
    } else if (this.angle < 180) {
      this.angle += 3;
    }

    if (!open) {
      if (this.angle > 0) {
        this.angle -= 3;
      }
    } else if (this.angle < 180) {
      this.angle += 3;
    }

    if (showArrow) {
      String countStr = "[" + getItems().size() + "]";
      int textW = FontUtil.getStringWidth(countStr);
      FontUtil.drawString("[", (float) (this.x + this.width) - textW - 3.0f,
          (float) this.y + 1.5f, 0xFF888888);
      String num = String.valueOf(getItems().size());
      FontUtil.drawString(num,
          (float) (this.x + this.width) - textW - 3.0f + FontUtil.getStringWidth("["),
          (float) this.y + 1.5f, 0xFFFFFFFF);
      FontUtil.drawString("]", (float) (this.x + this.width) - FontUtil.getStringWidth("]") - 3.0f,
          (float) this.y + 1.5f, 0xFF888888);
    }

    if (this.open) {
      int itemY = this.getY() + this.getHeight() - 3;
      for (Item item : getItems()) {
        if (!matchesSearch(item)) continue;
        item.setLocation((float) this.x + 2.0f, (float) itemY);
        item.setWidth(this.getWidth() - 4);
        item.drawScreen(mouseX, mouseY, partialTicks);
        itemY += item.getHeight() + 1;
      }
    }

    if (guiMod == null || guiMod.showBorder.getValue().booleanValue()) {
      float top = (float) this.y - 1.5f;
      float bottom = (float) this.y + this.height + (this.open ? getTotalItemHeight() - 2 : -1);
      int accent = Colors.getClientColorCustomAlpha(77);
      fill(this.x - 1, (int) top - 1, this.x + this.width + 1, (int) top, accent);
      fill(this.x - 1, (int) bottom, this.x + this.width + 1, (int) bottom + 1, accent);
      fill(this.x - 1, (int) top, this.x, (int) bottom, accent);
      fill(this.x + this.width, (int) top, this.x + this.width + 1, (int) bottom, accent);
    }
  }

  public boolean matchesSearch(Item item) {
    me.friendly.exeter.module.impl.toggle.render.ClickGui guiMod = getClickGuiModule();
    if (guiMod != null && !guiMod.searchEnabled.getValue().booleanValue()) return true;
    String query = ClickGuiScreen.getInstance().getSearch();
    if (query == null || query.isEmpty()) return true;
    return FuzzySearch.score(query, item.getLabel()) >= 0;
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

  private void drag(int mouseX, int mouseY) {
    if (!this.drag) {
      return;
    }
    this.x = this.x2 + mouseX;
    this.y = this.y2 + mouseY;
  }

  public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
      this.x2 = this.x - mouseX;
      this.y2 = this.y - mouseY;
      ClickGuiScreen.getInstance().getPanels();
      for (Panel panel : ClickGuiScreen.getInstance().getPanels()) {
        if (panel.drag) {
          panel.drag = false;
        }
      }
      this.drag = true;
      return;
    }
    if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
      this.open = !this.open;
      return;
    }
    if (!this.open) {
      return;
    }
    for (Item item : this.getItems()) {
      if (!matchesSearch(item)) continue;
      item.mouseClicked(mouseX, mouseY, mouseButton);
    }
  }

  public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
    if (releaseButton == 0) {
      this.drag = false;
    }
    if (!this.open) {
      return;
    }
    for (Item item : this.getItems()) {
      if (!matchesSearch(item)) continue;
      item.mouseReleased(mouseX, mouseY, releaseButton);
    }
  }

  public boolean containsMouse(int mouseX, int mouseY) {
    return mouseX >= this.getX()
        && mouseX <= this.getX() + this.getWidth()
        && mouseY >= this.getY()
        && mouseY <= this.getY() + this.getHeight() + (this.open ? this.getTotalItemHeight() : 0);
  }

  private boolean isHovering(int mouseX, int mouseY) {
    return mouseX >= this.getX()
        && mouseX <= this.getX() + this.getWidth()
        && mouseY >= this.getY()
        && mouseY <= this.getY() + this.getHeight() - (this.open ? 2 : 0);
  }

  private int getTotalItemHeight() {
    int height = 0;
    for (Item item : getItems()) {
      if (!matchesSearch(item)) continue;
      height += item.getHeight() + 1;
    }
    return height;
  }
}
