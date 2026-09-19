package me.earth.earthhack.impl.modules.render.skycolor;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.ColorSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;

/**
 * Custom fog color and density.
 * Original Exeter addition for Humza Client.
 */
public class SkyColor extends Module
{
    protected final Setting<Color> color =
        register(new ColorSetting("Color", new Color(0, 0, 0, 255)));
    protected final Setting<Float> density =
        register(new NumberSetting<>("Density", 0.01f, 0.0f, 1.0f));
    protected final Setting<Boolean> fog =
        register(new BooleanSetting("Fog", true));

    public SkyColor()
    {
        super("SkyColor", Category.Render);
        this.setData(new SimpleData(this,
            "Recolors sky fog."));
    }

    @Override
    protected void onEnable()
    {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    protected void onDisable()
    {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onFogColors(EntityViewRenderEvent.FogColors event)
    {
        event.setRed(color.getValue().getRed() / 255.0f);
        event.setGreen(color.getValue().getGreen() / 255.0f);
        event.setBlue(color.getValue().getBlue() / 255.0f);
    }

    @SubscribeEvent
    public void onFogDensity(EntityViewRenderEvent.FogDensity event)
    {
        if (fog.getValue())
        {
            event.setDensity(density.getValue());
            event.setCanceled(true);
        }
    }
}
