package me.friendly.exeter.module.impl.toggle.render;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.RenderGameOverlayEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.api.minecraft.render.RenderMethods;

/**
 * CustomCrosshair. Replaces the vanilla crosshair with a configurable one drawn on the HUD
 * layer (same pipeline as the Hud module, so it layers correctly over the world).
 *
 * Styles: Cross (classic +), Dot, Circle (stepped), CrossDot (cross + center dot).
 * Size and thickness are configurable; rainbow mode cycles hue over time.
 */
public class CustomCrosshair extends ToggleableModule {
    private enum Style { CROSS, DOT, CIRCLE, CROSSDOT }

    private final EnumProperty<Style> style = new EnumProperty<>(Style.CROSSDOT, "Style", "s");
    private final NumberProperty<Integer> size = new NumberProperty<>(6, 2, 20, "Size", "size");
    private final NumberProperty<Integer> thickness = new NumberProperty<>(1, 1, 5, "Thickness", "t");
    private final Property<Integer> color = new Property<>(0xFFFFFFFF, "Color", "c");
    private final Property<Boolean> rainbow = new Property<>(false, "Rainbow", "rainbow", "rb");

    public CustomCrosshair() {
        super("CustomCrosshair", new String[]{"crosshair", "ch"}, ModuleType.RENDER);
        offerProperties(style, size, thickness, color, rainbow);

        this.listeners.add(new Listener<RenderGameOverlayEvent>("crosshair_overlay") {
            @Override
            public void call(RenderGameOverlayEvent event) {
                if (event.getType() != RenderGameOverlayEvent.Type.IN_GAME) return;
                if (minecraft.player == null || minecraft.options == null) return;
                if (minecraft.gui.screen() != null) return;
                if (minecraft.gui.hud.getDebugOverlay().showDebugScreen()) return;

                int sw = minecraft.getWindow().getGuiScaledWidth();
                int sh = minecraft.getWindow().getGuiScaledHeight();
                int cx = sw / 2;
                int cy = sh / 2;

                int c = rainbow.getValue()
                        ? RenderMethods.rainbow(0, 1.0f).getRGB()
                        : color.getValue();
                int s = size.getValue();
                int t = thickness.getValue();

                switch (style.getValue()) {
                    case CROSS -> {
                        RenderMethods.drawRect(cx - s, cy - t / 2, cx + s + 1, cy + t / 2 + 1, c);
                        RenderMethods.drawRect(cx - t / 2, cy - s, cx + t / 2 + 1, cy + s + 1, c);
                    }
                    case DOT -> {
                        RenderMethods.drawRect(cx - t, cy - t, cx + t + 1, cy + t + 1, c);
                    }
                    case CIRCLE -> {
                        RenderMethods.drawCircle(cx * 2.0f, cy * 2.0f, s, 24, c);
                    }
                    case CROSSDOT -> {
                        RenderMethods.drawRect(cx - s, cy - t / 2, cx + s + 1, cy + t / 2 + 1, c);
                        RenderMethods.drawRect(cx - t / 2, cy - s, cx + t / 2 + 1, cy + s + 1, c);
                        RenderMethods.drawRect(cx - t, cy - t, cx + t + 1, cy + t + 1, c);
                    }
                }
            }
        });
    }
}
