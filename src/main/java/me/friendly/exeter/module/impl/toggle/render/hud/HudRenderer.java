package me.friendly.exeter.module.impl.toggle.render.hud;

import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.RenderGameOverlayEvent;
import me.friendly.exeter.module.Module;

public final class HudRenderer extends Module {
  private boolean defaultPositionsInitialized = false;

  public HudRenderer() {
    super("HudRenderer", new String[] {"hudrenderer"});
    setDescription("Central renderer for all HUD elements.");

    Exeter.getInstance()
        .getEventManager()
        .register(
            new Listener<RenderGameOverlayEvent>("hud_renderer_listener") {
              @Override
              public void call(RenderGameOverlayEvent event) {
                if (minecraft.gui.hud.getDebugOverlay().showDebugScreen()
                    || event.getType() != RenderGameOverlayEvent.Type.IN_GAME) {
                  return;
                }
                int sw = minecraft.getWindow().getGuiScaledWidth();
                int sh = minecraft.getWindow().getGuiScaledHeight();
                List<HudModule> active = HudModule.getActive();
                if (!defaultPositionsInitialized) {
                  HudModule.defaultLayout(active, sw, sh);
                  defaultPositionsInitialized = true;
                }
                for (HudModule m : active) {
                  m.render(sw, sh);
                }
              }
            });
  }

  public void relayout() {
    defaultPositionsInitialized = false;
  }
}
