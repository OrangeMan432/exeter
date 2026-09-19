package me.earth.earthhack.impl.modules.render.potioneffects;

import me.earth.earthhack.impl.event.events.render.Render2DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.potion.PotionEffect;

final class ListenerRender2D
        extends ModuleListener<PotionEffects, Render2DEvent>
{
    public ListenerRender2D(PotionEffects module)
    {
        super(module, Render2DEvent.class);
    }

    @Override
    public void invoke(Render2DEvent event)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        int y = module.y.getValue();
        for (PotionEffect effect : mc.player.getActivePotionEffects())
        {
            String text = effect.getEffectName()
                + " " + (effect.getAmplifier() + 1)
                + " " + effect.getDuration() / 20 + "s";
            mc.fontRenderer.drawStringWithShadow(
                text, module.x.getValue(), y, 0xFFFFFF);
            y += 10;
        }
    }
}
