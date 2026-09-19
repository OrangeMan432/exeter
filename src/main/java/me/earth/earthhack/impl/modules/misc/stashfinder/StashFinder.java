package me.earth.earthhack.impl.modules.misc.stashfinder;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;
import net.minecraft.util.math.ChunkPos;

import java.util.HashSet;
import java.util.Set;

/**
 * Logs chunks with dense storage blocks (stashes).
 * Original Exeter addition for the 1.12.2 branch.
 */
public class StashFinder extends Module
{
    protected final Setting<Integer> minStorage =
        register(new NumberSetting<>("MinStorage", 5, 1, 50));
    protected final Setting<Boolean> notify =
        register(new BooleanSetting("Notify", true));
    protected final Setting<Boolean> saveToFile =
        register(new BooleanSetting("SaveToFile", true));

    final Set<ChunkPos> logged = new HashSet<>();
    int ticks;

    public StashFinder()
    {
        super("StashFinder", Category.Misc);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Logs chunks packed with chests and shulkers."));
    }

    @Override
    protected void onEnable()
    {
        logged.clear();
        ticks = 0;
    }
}
