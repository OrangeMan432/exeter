package me.earth.earthhack.impl.modules.misc.pearlspoof;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Places a block then phases a pearl through it.
 * Ported from Mio 0.6.9 (PearlSpoof).
 */
public class PearlSpoof extends Module
{
    protected final Setting<Boolean> packetPlace =
        register(new BooleanSetting("PacketPlace", false));

    public PearlSpoof()
    {
        super("PearlSpoof", Category.Misc);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Block plus pearl for instant phasing."));
    }
}
