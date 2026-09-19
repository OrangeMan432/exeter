package me.earth.earthhack.impl.modules.player.autojump;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Vanilla auto-jump toggle.
 * Original Exeter addition for the 1.12.2 branch.
 */
public class AutoJump extends Module
{
    public AutoJump()
    {
        super("AutoJump", Category.Player);
        this.setData(new SimpleData(this,
            "Toggles vanilla auto jump."));
    }

    @Override
    protected void onEnable()
    {
        if (mc.gameSettings != null)
        {
            mc.gameSettings.autoJump = true;
        }
    }

    @Override
    protected void onDisable()
    {
        if (mc.gameSettings != null)
        {
            mc.gameSettings.autoJump = false;
        }
    }
}
