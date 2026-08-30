package me.friendly.exeter.module.impl.toggle.combat;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

/**
 * Velocity. Cancels/reduces knockback from hits and explosions.
 *
 * 5b5t caution: full knockback cancellation (100%) is a classic anticheat flag — the server
 * sees you not moving after a hit. Default is Horizontal/Vertical Reduction at 0% (vanilla
 * passthrough) with full cancel OFF; raise reductions gradually and test in-game.
 *
 * Modes per packet type:
 *  - Motion packets (ClientboundSetEntityMotionPacket for self): apply reduction % to the
 *    packet's knockback vector instead of cancelling (reduction 100% = full cancel).
 *  - Explosion packets (ClientboundExplodePacket): cancel only if Cancel Explosions is on.
 */
public class Velocity extends ToggleableModule {

    private final NumberProperty<Integer> horizontalReduce = new NumberProperty<>(0, 0, 100, "Horizontal Reduce %", "hreduce", "h");
    private final NumberProperty<Integer> verticalReduce = new NumberProperty<>(0, 0, 100, "Vertical Reduce %", "vreduce", "v");
    private final Property<Boolean> cancelExplosions = new Property<>(false, "Cancel Explosions", "cancel explosions", "ce");

    private final Listener<PacketEvent> packetListener = new Listener<PacketEvent>("velocity_packet") {
        @Override
        public void call(PacketEvent event) {
            if (minecraft.player == null) return;

            if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet
                    && packet.id() == minecraft.player.getId()) {
                int h = horizontalReduce.getValue();
                int v = verticalReduce.getValue();
                if (h >= 100 && v >= 100) {
                    event.setCanceled(true);
                    return;
                }
                if (h > 0 || v > 0) {
                    // Record is immutable — replace the packet instance in the event.
                    Vec3 vel = packet.movement();
                    Vec3 scaled = new Vec3(
                            vel.x * (1.0 - h / 100.0),
                            vel.y * (1.0 - v / 100.0),
                            vel.z * (1.0 - h / 100.0));
                    event.setPacket(new ClientboundSetEntityMotionPacket(packet.id(), scaled));
                }
            }

            if (cancelExplosions.getValue() && event.getPacket() instanceof ClientboundExplodePacket) {
                event.setCanceled(true);
            }
        }
    };

    public Velocity() {
        super("Velocity", new String[]{"velocity", "velocity-cancel"}, 0xFF0000, ModuleType.COMBAT);
        offerProperties(horizontalReduce, verticalReduce, cancelExplosions);
        this.listeners.add(packetListener);
    }
}
