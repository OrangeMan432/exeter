package me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties;

import com.mojang.blaze3d.platform.InputConstants;
import me.friendly.exeter.module.impl.toggle.render.clickgui.ClickGui;
import me.friendly.exeter.module.impl.toggle.render.clickgui.ClickPopup;

/**
 * Routes keys to a slider that is being typed into. The slider draws its own caret in place, so
 * this only handles the keyboard and the click that takes focus away.
 */
public final class NumberEditPopup implements ClickPopup {

  private final NumberSlider slider;

  public NumberEditPopup(NumberSlider slider) {
    this.slider = slider;
  }

  @Override
  public void render(int mouseX, int mouseY, float partialTicks, int screenW, int screenH) {}

  @Override
  public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
    // Clicking anywhere else commits, like the rest of the ClickGUI.
    slider.commitEditor();
    ClickGui.getClickGui().closePopup();
    return false;
  }

  @Override
  public boolean mouseReleased(int mouseX, int mouseY, int button) {
    return false;
  }

  @Override
  public boolean mouseDragged(int mouseX, int mouseY) {
    return false;
  }

  @Override
  public boolean mouseScrolled(double scrollDelta) {
    return false;
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == InputConstants.KEY_ESCAPE) {
      slider.cancelEditor();
      ClickGui.getClickGui().closePopup();
      return true;
    }
    if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
      slider.commitEditor();
      ClickGui.getClickGui().closePopup();
      return true;
    }
    return slider.keyPressedInEditor(keyCode, scanCode);
  }
}
