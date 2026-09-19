package me.earth.earthhack.impl.modules.misc.packetcanceller;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Cancels chosen outgoing packets, bypass toolkit.
 * Ported from SalHack (PacketCanceller).
 */
public class PacketCanceller extends Module
{
    protected final Setting<Boolean> input =
        register(new BooleanSetting("Input", true));
    protected final Setting<Boolean> position =
        register(new BooleanSetting("Position", true));
    protected final Setting<Boolean> positionRotation =
        register(new BooleanSetting("PositionRotation", true));
    protected final Setting<Boolean> rotation =
        register(new BooleanSetting("Rotation", true));
    protected final Setting<Boolean> abilities =
        register(new BooleanSetting("PlayerAbilities", true));
    protected final Setting<Boolean> digging =
        register(new BooleanSetting("PlayerDigging", true));
    protected final Setting<Boolean> useItem =
        register(new BooleanSetting("PlayerTryUseItem", true));
    protected final Setting<Boolean> useItemOnBlock =
        register(new BooleanSetting("PlayerTryUseItemOnBlock", true));
    protected final Setting<Boolean> entityAction =
        register(new BooleanSetting("EntityAction", true));
    protected final Setting<Boolean> useEntity =
        register(new BooleanSetting("UseEntity", true));
    protected final Setting<Boolean> vehicleMove =
        register(new BooleanSetting("VehicleMove", true));

    int cancelled;

    public PacketCanceller()
    {
        super("PacketCanceller", Category.Misc);
        this.listeners.add(new ListenerSend(this));
        this.setData(new SimpleData(this,
            "Cancels outgoing packets by type."));
    }

    @Override
    protected void onDisable()
    {
        cancelled = 0;
    }
}
