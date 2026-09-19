package me.earth.earthhack.impl.modules.combat.webtrap;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;
import me.earth.earthhack.impl.util.math.StopWatch;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Traps enemies in webs at their feet and face.
 * Ported from Mio 0.6.9 (WebTrap).
 */
public class WebTrap extends Module
{
    protected final Setting<Integer> delay =
        register(new NumberSetting<>("TickDelay", 50, 0, 250));
    protected final Setting<Integer> blocksPerTick =
        register(new NumberSetting<>("BPT", 1, 1, 30));
    protected final Setting<Double> range =
        register(new NumberSetting<>("Range", 10.0, 1.0, 12.0));
    protected final Setting<Boolean> autoDisable =
        register(new BooleanSetting("AutoDisable", false));
    protected final Setting<Boolean> feet =
        register(new BooleanSetting("Feet", true));
    protected final Setting<Boolean> face =
        register(new BooleanSetting("Face", false));
    protected final Setting<Boolean> render =
        register(new BooleanSetting("Render", true));

    final StopWatch delayTimer = new StopWatch();
    final Map<BlockPos, Long> renderBlocks = new ConcurrentHashMap<>();
    EntityPlayer target;
    BlockPos startPos;
    int lastSlot;
    boolean isSneaking;

    public WebTrap()
    {
        super("WebTrap", Category.Combat);
        this.listeners.add(new ListenerTick(this));
        this.listeners.add(new ListenerRender(this));
        this.listeners.add(new ListenerBlockChange(this));
        this.setData(new SimpleData(this,
            "Webs enemies feet and face to hold them still."));
    }

    @Override
    protected void onEnable()
    {
        if (mc.player != null)
        {
            startPos = new BlockPos(mc.player.posX,
                                    mc.player.posY,
                                    mc.player.posZ);
            lastSlot = mc.player.inventory.currentItem;
        }

        target = null;
    }

    @Override
    protected void onDisable()
    {
        renderBlocks.clear();
        target = null;
    }
}
