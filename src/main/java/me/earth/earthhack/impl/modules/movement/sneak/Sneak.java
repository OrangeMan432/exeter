package me.earth.earthhack.impl.modules.movement.sneak;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Sneaks at full speed.
 * Ported from SalHack (Sneak).
 */
public class Sneak extends Module
{
    protected final Setting<Mode> mode =
        register(new EnumSetting<>("Mode", Mode.NCP));

    public Sneak()
    {
        super("Sneak", Category.Movement);
        this.listeners.add(new ListenerMotion(this));
        this.listeners.add(new ListenerSend(this));
        this.setData(new SimpleData(this,
            "Sneak without slowing down."));
    }

    @Override
    protected void onDisable()
    {
        if (mc.world != null
            && mc.player != null
            && !mc.player.isSneaking())
        {
            mc.player.connection.sendPacket(
                new net.minecraft.network.play.client.CPacketEntityAction(
                    mc.player,
                    net.minecraft.network.play.client.CPacketEntityAction
                        .Action.STOP_SNEAKING));
        }
    }

    public enum Mode
    {
        Vanilla,
        NCP,
        Always
    }
}
