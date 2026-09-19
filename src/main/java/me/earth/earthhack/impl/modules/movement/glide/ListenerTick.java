package me.earth.earthhack.impl.modules.movement.glide;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.client.settings.GameSettings;

final class ListenerTick extends ModuleListener<Glide, TickEvent>
{
    public ListenerTick(Glide module)
    {
        super(module, TickEvent.class);
    }

    @Override
    public void invoke(TickEvent event)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindJump)
            && mc.player.motionY < 0.0
            && !mc.player.onGround
            && mc.player.fallDistance > 0.0f
            && !mc.player.isInWater()
            && !mc.player.isOnLadder()
            && !mc.player.isInLava()
            && !mc.player.collidedVertically)
        {
            mc.player.motionY = -0.125;
            mc.player.jumpMovementFactor *= 1.21337f;
        }
    }
}
