package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.module.impl.toggle.render.hud.HudEditorScreen;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;
import net.minecraft.client.Minecraft;

public abstract class ListHudModule extends HudModule {

  public static final class TextEntry {
    private final String text;
    private final int color;

    public TextEntry(String text, int color) {
      this.text = text;
      this.color = color;
    }

    public String text() {
      return text;
    }

    public int color() {
      return color;
    }
  }

  protected ListHudModule(String label, String[] aliases, int color, Corner defaultCorner) {
    super(label, aliases, color, defaultCorner);
  }

  protected ListHudModule(String label, String[] aliases, Corner defaultCorner) {
    super(label, aliases, defaultCorner);
  }

  protected abstract List<TextEntry> getEntries();

  protected List<TextEntry> getDummyEntries() {
    List<TextEntry> dummy = new ArrayList<TextEntry>();
    dummy.add(new TextEntry(getLabel() + " dummy", 0xFFFFFFFF));
    return dummy;
  }

  protected boolean isInEditor() {
    Minecraft mc = MinecraftAccessor.getMinecraft();
    return mc != null && mc.currentScreen instanceof HudEditorScreen;
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
    return entriesOrDummy().size() * FontUtil.getFontHeight();
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    List<TextEntry> entries = entriesOrDummy();
    if (entries.isEmpty()) return;

    boolean top = getCorner() == Corner.TOP_LEFT || getCorner() == Corner.TOP_RIGHT;
    boolean right = getCorner() == Corner.TOP_RIGHT || getCorner() == Corner.BOTTOM_RIGHT;
    int width = getWidth();
    int line = FontUtil.getFontHeight();

    int py = top ? getY() : getY() + getHeight() - line;
    for (TextEntry entry : entries) {
      int lw = FontUtil.getStringWidth(entry.text());
      int px = right ? getX() + width - lw : getX();
      FontUtil.drawString(entry.text(), (float) px, (float) py, entry.color());
      py += top ? line : -line;
    }
  }
}
