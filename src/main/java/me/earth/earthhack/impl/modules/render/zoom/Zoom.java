package me.earth.earthhack.impl.modules.render.zoom;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Lowers FOV and sensitivity while active.
 * Original Exeter addition for the 1.12.2 branch.
 */
public class Zoom extends Module
{
    protected final Setting<Float> fov =
        register(new NumberSetting<>("FOV", 30.0f, 1.0f, 130.0f));
    protected final Setting<Float> sensitivity =
        register(new NumberSetting<>("Sensitivity", 0.5f, 0.1f, 2.0f));

    float oldFov;
    float oldSensitivity;

    public Zoom()
    {
        super("Zoom", Category.Render);
        this.setData(new SimpleData(this,
            "Zooms your view in while the key is held."));
    }

    @Override
    protected void onEnable()
    {
        if (mc.gameSettings == null)
        {
            return;
        }

        oldFov = mc.gameSettings.fovSetting;
        oldSensitivity = mc.gameSettings.mouseSensitivity;
        mc.gameSettings.fovSetting = fov.getValue();
        mc.gameSettings.mouseSensitivity =
            oldSensitivity * sensitivity.getValue();
    }

    @Override
    protected void onDisable()
    {
        if (mc.gameSettings == null)
        {
            return;
        }

        mc.gameSettings.fovSetting = oldFov;
        mc.gameSettings.mouseSensitivity = oldSensitivity;
    }
}
