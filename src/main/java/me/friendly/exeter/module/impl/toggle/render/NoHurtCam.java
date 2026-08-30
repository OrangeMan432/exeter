package me.friendly.exeter.module.impl.toggle.render;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;

/**
 * NoHurtCam. Blocks the server-driven hurt animation/camera tilt.
 *
 * On 26.2 the hurt tilt is triggered by {@code ClientboundHurtAnimationPacket} (the server
 * sends it when you take damage; the client tilts the camera). Cancelling the packet removes
 * the tilt entirely — local damage feedback (red overlay, sounds) is untouched.
 */
public class NoHurtCam extends ToggleableModule {
    private final Property<Boolean> onlyOwnDamage = new Property<>(true, "Only Own Damage", "own", "o");

    public NoHurtCam() {
        super("NoHurtCam", new String[]{"nohurtcam", "nohurt", "anticam"}, ModuleType.RENDER);
        offerProperties(onlyOwnDamage);

        this.listeners.add(new Listener<PacketEvent>("no_hurt_cam_packet") {
            @Override
            public void call(PacketEvent event) {
                if (event.getPacket() instanceof ClientboundHurtAnimationPacket p) {
                    // Only Own Damage: let other players' hurt animations through (useful
                    // for seeing crystal pops land on enemies), block only our own tilt.
                    if (!onlyOwnDamage.getValue()
                            || minecraft.player == null
                            || p.id() == minecraft.player.getId()) {
                        event.setCanceled(true);
                    }
                }
            }
        });
    }
}
