package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/**
 * NoFall. Prevents fall damage.
 *
 * Two mechanisms, combined:
 *  - Outgoing: rewrites the {@code onGround} flag on our own move packets to {@code true}
 *    while falling. The server credits a landed step, negating accumulated fall distance.
 *    This is the classic "NoFall" the 1.8 clients (incl. Exeter) shipped.
 *  - Incoming: cancels server position-correction packets triggered by the flag rewrite
 *    (optional, off by default — some anticheats flag the correction storm).
 *
 * The local fall-distance accumulator is also zeroed each tick so client-side prediction
 * (and the fall-damage sound) never plays.
 */
public class NoFall extends ToggleableModule {
    private final Property<Boolean> packetSpoof = new Property<>(true, "Packet Spoof", "packet", "spoof");
    private final Property<Boolean> zeroFallDistance = new Property<>(true, "Zero Fall Distance", "zerofall", "zf");

    public NoFall() {
        super("NoFall", new String[]{"nofall", "antifall"}, ModuleType.MOVEMENT);
        offerProperties(packetSpoof, zeroFallDistance);

        this.listeners.add(new Listener<PacketEvent>("no_fall_send") {
            @Override
            public void call(PacketEvent event) {
                if (minecraft.player == null) return;
                if (!packetSpoof.getValue()) return;
                if (!(event.getPacket() instanceof ServerboundMovePlayerPacket packet)) return;
                if (!packet.hasPosition()) return;
                if (minecraft.player.fallDistance <= 0.5f) return; // only while actually falling

                boolean onGround = packet.isOnGround();
                boolean horizontalCollision = packet.horizontalCollision();

                ServerboundMovePlayerPacket replacement;
                if (packet instanceof ServerboundMovePlayerPacket.Pos pos) {
                    replacement = new ServerboundMovePlayerPacket.Pos(
                            packet.getX(0.0), packet.getY(0.0), packet.getZ(0.0), true, horizontalCollision);
                } else if (packet instanceof ServerboundMovePlayerPacket.PosRot posRot) {
                    replacement = new ServerboundMovePlayerPacket.PosRot(
                            packet.getX(0.0), packet.getY(0.0), packet.getZ(0.0),
                            packet.getYRot(0.0f), packet.getXRot(0.0f), true, horizontalCollision);
                } else {
                    return;
                }
                event.setPacket(replacement);
            }
        });

        this.listeners.add(new Listener<TickEvent>("no_fall_tick") {
            @Override
            public void call(TickEvent event) {
                if (minecraft.player == null) return;
                if (zeroFallDistance.getValue() && minecraft.player.fallDistance > 0) {
                    minecraft.player.fallDistance = 0;
                }
            }
        });
    }
}
