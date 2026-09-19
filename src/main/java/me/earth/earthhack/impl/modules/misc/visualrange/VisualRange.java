package me.earth.earthhack.impl.modules.misc.visualrange;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

import java.util.ArrayList;
import java.util.List;

/**
 * Reports players entering and leaving render distance.
 * Ported from Kami Blue (VisualRange).
 */
public class VisualRange extends Module
{
    protected final Setting<Boolean> leaving =
        register(new BooleanSetting("Leaving", true));

    final List<String> knownPlayers = new ArrayList<>();

    public VisualRange()
    {
        super("VisualRange", Category.Misc);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Reports players entering and leaving visual range."));
    }

    @Override
    protected void onEnable()
    {
        knownPlayers.clear();
    }
}
