package me.friendly.exeter.module.impl.toggle.render.clickgui;

/** Common interface for ClickGUI popups (list selectors, color pickers). */
public interface ClickPopup {
  void render(int mouseX, int mouseY, float partialTicks, int screenW, int screenH);

  boolean mouseClicked(int mouseX, int mouseY, int mouseButton);

  boolean mouseReleased(int mouseX, int mouseY, int button);

  boolean mouseDragged(int mouseX, int mouseY);

  boolean mouseScrolled(double scrollDelta);

  boolean keyPressed(int keyCode, int scanCode, int modifiers);
}
