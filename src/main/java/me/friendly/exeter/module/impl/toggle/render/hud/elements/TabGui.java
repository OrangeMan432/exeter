package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;
import me.friendly.exeter.module.impl.toggle.render.tabgui.GuiTabHandler;
import me.friendly.exeter.module.impl.toggle.render.tabgui.item.GuiTab;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public final class TabGui extends HudModule {

  private final GuiTabHandler handler = new GuiTabHandler();
  private boolean prevUp;
  private boolean prevDown;
  private boolean prevLeft;
  private boolean prevRight;
  private boolean prevReturn;

  public TabGui() {
    super("TabGui", new String[] {"tabgui", "tg"}, Corner.TOP_LEFT);
    setDescription("Renders a category-based tab menu for quick module access.");
    offerProperties();
    this.listeners.add(
        new Listener<TickEvent>("tabgui_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            TabGui.this.onTick();
          }
        });
  }

  private boolean pressed(int key, boolean prev) {
    return Keyboard.isKeyDown(key) && !prev;
  }

  private void onTick() {
    Minecraft mc = MinecraftAccessor.getMinecraft();
    boolean up = false;
    boolean down = false;
    boolean left = false;
    boolean right = false;
    boolean ret = false;
    if (mc != null && mc.currentScreen == null) {
      up = pressed(Keyboard.KEY_UP, prevUp);
      down = pressed(Keyboard.KEY_DOWN, prevDown);
      left = pressed(Keyboard.KEY_LEFT, prevLeft);
      right = pressed(Keyboard.KEY_RIGHT, prevRight);
      ret = pressed(Keyboard.KEY_RETURN, prevReturn);
      handler.ensureTabsPopulated();
      if (handler.tabs.isEmpty()) {
        saveKeys();
        return;
      }
      if (up) moveUp();
      if (down) moveDown();
      if (left) moveLeft();
      if (right) moveRight();
      if (ret) activate();
    }
    saveKeys();
  }

  private void saveKeys() {
    prevUp = Keyboard.isKeyDown(Keyboard.KEY_UP);
    prevDown = Keyboard.isKeyDown(Keyboard.KEY_DOWN);
    prevLeft = Keyboard.isKeyDown(Keyboard.KEY_LEFT);
    prevRight = Keyboard.isKeyDown(Keyboard.KEY_RIGHT);
    prevReturn = Keyboard.isKeyDown(Keyboard.KEY_RETURN);
  }

  private GuiTab currentTab() {
    return handler.tabs.get(handler.selectedTab);
  }

  private void moveUp() {
    if (!handler.visible) return;
    if (handler.mainMenu) {
      --handler.selectedTab;
      if (handler.selectedTab < 0) {
        handler.selectedTab = handler.tabs.size() - 1;
      }
      handler.transition = 11;
      return;
    }
    --handler.selectedItem;
    if (handler.selectedItem < 0) {
      handler.selectedItem = currentTab().getMods().size() - 1;
    }
    if (currentTab().getMods().size() > 1) {
      handler.transition = 11;
    }
  }

  private void moveDown() {
    if (!handler.visible) return;
    if (handler.mainMenu) {
      ++handler.selectedTab;
      if (handler.selectedTab > handler.tabs.size() - 1) {
        handler.selectedTab = 0;
      }
      handler.transition = -11;
      return;
    }
    ++handler.selectedItem;
    if (handler.selectedItem > currentTab().getMods().size() - 1) {
      handler.selectedItem = 0;
    }
    if (currentTab().getMods().size() > 1) {
      handler.transition = -11;
    }
  }

  private void moveLeft() {
    if (handler.mainMenu) return;
    handler.mainMenu = true;
  }

  private void moveRight() {
    if (handler.mainMenu) {
      handler.mainMenu = false;
      handler.selectedItem = 0;
      return;
    }
    if (!handler.visible) {
      handler.visible = true;
      handler.mainMenu = true;
      return;
    }
    currentTab().getMods().get(handler.selectedItem).getToggleableModule().toggle();
  }

  private void activate() {
    if (handler.mainMenu || !handler.visible) return;
    currentTab().getMods().get(handler.selectedItem).getToggleableModule().toggle();
  }

  @Override
  public int getWidth() {
    return 73;
  }

  @Override
  public int getHeight() {
    handler.ensureTabsPopulated();
    return handler.tabs.size() * 12;
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    handler.drawGui(getX(), getY());
  }
}
