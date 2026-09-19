package me.earth.earthhack.impl.modules.misc.packetfilter;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Cancels chosen incoming packets, bypass toolkit.
 * Ported from Phobos 1.9 (AntiPackets, receive half).
 */
public class PacketFilter extends Module
{
    protected final Setting<Boolean> explosion =
        register(new BooleanSetting("Explosion", false));
    protected final Setting<Boolean> entityVelocity =
        register(new BooleanSetting("EntityVelocity", false));
    protected final Setting<Boolean> posLook =
        register(new BooleanSetting("PlayerPosLook", false));
    protected final Setting<Boolean> entityMetadata =
        register(new BooleanSetting("EntityMetadata", false));
    protected final Setting<Boolean> blockChange =
        register(new BooleanSetting("BlockChange", false));
    protected final Setting<Boolean> soundEffect =
        register(new BooleanSetting("SoundEffect", false));
    protected final Setting<Boolean> chat =
        register(new BooleanSetting("Chat", false));
    protected final Setting<Boolean> keepAlive =
        register(new BooleanSetting("KeepAlive", false));
    protected final Setting<Boolean> entityEffect =
        register(new BooleanSetting("EntityEffect", false));
    protected final Setting<Boolean> destroyEntities =
        register(new BooleanSetting("DestroyEntities", false));
    protected final Setting<Boolean> spawnPlayer =
        register(new BooleanSetting("SpawnPlayer", false));
    protected final Setting<Boolean> timeUpdate =
        register(new BooleanSetting("TimeUpdate", false));

    int cancelled;

    public PacketFilter()
    {
        super("PacketFilter", Category.Misc);
        this.listeners.add(new ListenerReceive(this));
        this.setData(new SimpleData(this,
            "Cancels incoming packets by type."));
    }

    @Override
    protected void onDisable()
    {
        cancelled = 0;
    }
}
