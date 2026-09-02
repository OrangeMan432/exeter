package me.friendly.exeter.module.impl.toggle.render;

import me.friendly.api.event.Listener;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.exeter.events.RenderGameOverlayEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;

public class EatTimer extends ToggleableModule {

  private final Listener<RenderGameOverlayEvent> renderListener =
      new Listener<RenderGameOverlayEvent>("eat_timer_render") {
        @Override
        public void call(RenderGameOverlayEvent event) {
          if (event.getType() != RenderGameOverlayEvent.Type.IN_GAME) return;
          if (minecraft.player == null || minecraft.level == null) return;
          if (!minecraft.player.isUsingItem()) return;
          if (RenderMethods.guiGraphics == null) return;

          ItemStack useItem = minecraft.player.getUseItem();
          Consumable consumable = useItem.get(DataComponents.CONSUMABLE);
          if (consumable == null) return;

          int totalTicks = consumable.consumeTicks();
          if (totalTicks <= 0) return;

          int elapsed = minecraft.player.getTicksUsingItem();
          double percent = Math.min((double) elapsed / totalTicks * 100.0, 100.0);

          String text = String.format("%.1f%%", percent);

          int sw = minecraft.getWindow().getGuiScaledWidth();
          int sh = minecraft.getWindow().getGuiScaledHeight();
          int textWidth = minecraft.font.width(text);
          int x = (sw - textWidth) / 2;
          int y = sh / 2 + 14;

          RenderMethods.guiGraphics.text(minecraft.font, text, x, y, 0xFFFFFFFF, true);
        }
      };

  public EatTimer() {
    super("EatTimer", new String[] {"eattimer", "eat-timer"}, 0x00FF00, ModuleType.RENDER);
    setDescription("Displays a progress bar while eating food.");
    this.listeners.add(renderListener);
  }
}
