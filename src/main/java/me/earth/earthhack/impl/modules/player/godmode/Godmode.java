package me.earth.earthhack.impl.modules.player.godmode;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.impl.util.client.SimpleData;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.CPacketEntityAction;

/**
 * Entity-ride godmode, hides the mount.
 * Ported from Phobos 1.9 (Godmode).
 */
public class Godmode extends Module
{
    protected final Setting<Boolean> remount =
        register(new BooleanSetting("Remount", false));

    Entity entity;

    public Godmode()
    {
        super("Godmode", Category.Player);
        this.listeners.add(new ListenerSend(this));
        this.setData(new SimpleData(this,
            "Ride godmode, mount something first."));
    }

    @Override
    protected void onEnable()
    {
        if (mc.world == null || mc.player == null)
        {
            disable();
            return;
        }

        if (mc.player.getRidingEntity() != null)
        {
            entity = mc.player.getRidingEntity();
            mc.renderGlobal.loadRenderers();
            mc.world.removeEntity(entity);
            mc.player.setPosition(mc.player.posX,
                                 mc.player.posY - 1.0,
                                 mc.player.posZ);
        }

        if (remount.getValue())
        {
            remount.setValue(false);
        }
    }

    @Override
    protected void onDisable()
    {
        if (remount.getValue())
        {
            remount.setValue(false);
        }

        if (mc.player != null)
        {
            mc.player.dismountRidingEntity();
            mc.player.connection.sendPacket(new CPacketEntityAction(
                mc.player, CPacketEntityAction.Action.START_SNEAKING));
            mc.player.connection.sendPacket(new CPacketEntityAction(
                mc.player, CPacketEntityAction.Action.STOP_SNEAKING));
        }

        entity = null;
    }
}
