package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import java.util.List;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.toggle.render.hud.HudEditorScreen;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;
import net.minecraft.client.Minecraft;

public abstract class ListHudModule extends HudModule {

  public record TextEntry(String text, int color) {}

  protected ListHudModule(String label, String[] aliases, int color, Corner defaultCorner) {
    super(label, aliases, color, defaultCorner);
  }

  protected ListHudModule(String label, String[] aliases, Corner defaultCorner) {
    super(label, aliases, defaultCorner);
  }

  protected abstract List<TextEntry> getEntries();

  protected List<TextEntry> getDummyEntries() {
    return List.of(new TextEntry(getLabel() + " dummy", 0xFFFFFFFF));
  }

  protected boolean isInEditor() {
    return Minecraft.getInstance().gui != null
        && Minecraft.getInstance().gui.screen() instanceof HudEditorScreen;
  }

  protected List<TextEntry> entriesOrDummy() {
    List<TextEntry> entries = getEntries();
    if (entries.isEmpty() && isInEditor()) return getDummyEntries();
    return entries;
  }

  @Override
  public int getWidth() {
    int max = 0;
    for (TextEntry entry : entriesOrDummy()) {
      int w = FontUtil.getStringWidth(entry.text());
      if (w > max) max = w;
    }
    return max;
  }

  @Override
  public int getHeight() {
    return entriesOrDummy().size() * 9;
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    List<TextEntry> entries = entriesOrDummy();
    if (entries.isEmpty()) return;

    boolean top = getCorner() == Corner.TOP_LEFT || getCorner() == Corner.TOP_RIGHT;
    boolean right = getCorner() == Corner.TOP_RIGHT || getCorner() == Corner.BOTTOM_RIGHT;
    int width = getWidth();

    int py = top ? getY() : getY() + getHeight() - 9;
    for (TextEntry entry : entries) {
      int lw = FontUtil.getStringWidth(entry.text());
      int px = right ? getX() + width - lw : getX();
      FontUtil.drawString(entry.text(), px, py, entry.color());
      py += top ? 9 : -9;
    }
  }
}
