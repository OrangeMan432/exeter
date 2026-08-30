package me.friendly.exeter.module.impl.toggle.render;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;

/**
 * NoHurtCam. Blocks the server-driven hurt animation/camera tilt.
 *
 * On 26.2 the hurt tilt is triggered by {@code ClientboundHurtAnimationPacket} (the server
 * sends it when you take damage; the client tilts the camera). Cancelling the packet removes
 * the tilt entirely — local damage feedback (red overlay, sounds) is untouched.
 */
public class NoHurtCam extends ToggleableModule {

    public NoHurtCam() {
        super("NoHurtCam", new String[]{"nohurtcam", "nohurt", "anticam"}, ModuleType.RENDER);

        this.listeners.add(new Listener<PacketEvent>("no_hurt_cam_packet") {
            @Override
            public void call(PacketEvent event) {
                if (event.getPacket() instanceof ClientboundHurtAnimationPacket) {
                    event.setCanceled(true);
                }
            }
        });
    }
}
