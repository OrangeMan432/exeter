package me.friendly.exeter.module.impl.active.combat;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/**
 * AntiAim. Scrambles the rotation the client reports to the server on every outgoing move packet.
 * The local view is untouched; only what the server (and other players) see changes, making you
 * harder to hit.
 *
 * Yaw modes:
 *  - Off   : leave the real yaw untouched.
 *  - Spin  : continuously rotate the reported yaw (spinSpeed degrees per packet).
 *  - Random: randomize the reported yaw every packet.
 *
 * Pitch modes:
 *  - Off    : leave the real pitch untouched.
 *  - Up     : look straight up (90).
 *  - Down   : look straight down (-90).
 *  - Zero   : look level (0).
 *  - Custom : use the Pitch Value slider.
 */
public class AntiAim extends ToggleableModule {
    private final Property<String> yawMode = new Property<>("Spin", "Yaw Mode", "yawmode");
    private final Property<String> pitchMode = new Property<>("Up", "Pitch Mode", "pitchmode");
    private final Property<Float> pitchValue = new Property<>(90.0f, "Pitch Value", "pitchvalue");
    private final Property<Float> spinSpeed = new Property<>(15.0f, "Spin Speed", "spinspeed");

    private float spinYaw = 0.0f;

    public AntiAim() {
        super("AntiAim", new String[]{"antiaim", "aa"}, ModuleType.COMBAT);
        offerProperties(yawMode, pitchMode, pitchValue, spinSpeed);

        this.listeners.add(new Listener<PacketEvent>("anti_aim_packet") {
            @Override
            public void call(PacketEvent event) {
                if (minecraft.player == null) return;
                if (!(event.getPacket() instanceof ServerboundMovePlayerPacket packet)) return;
                if (!packet.hasRotation()) return;

                float yaw = packet.getYRot(0.0f);
                float pitch = packet.getXRot(0.0f);

                switch (yawMode.getValue().toLowerCase()) {
                    case "spin":
                        spinYaw += spinSpeed.getValue();
                        if (spinYaw > 180.0f) spinYaw -= 360.0f;
                        if (spinYaw < -180.0f) spinYaw += 360.0f;
                        yaw = spinYaw;
                        break;
                    case "random":
                        yaw = (float) (Math.random() * 360.0 - 180.0);
                        break;
                    default:
                        break;
                }

                switch (pitchMode.getValue().toLowerCase()) {
                    case "up":
                        pitch = 90.0f;
                        break;
                    case "down":
                        pitch = -90.0f;
                        break;
                    case "zero":
                        pitch = 0.0f;
                        break;
                    case "custom":
                        pitch = pitchValue.getValue();
                        break;
                    default:
                        break;
                }

                boolean onGround = packet.isOnGround();
                boolean horizontalCollision = packet.horizontalCollision();

                ServerboundMovePlayerPacket replacement;
                if (packet instanceof ServerboundMovePlayerPacket.PosRot posRot) {
                    replacement = new ServerboundMovePlayerPacket.PosRot(
                            packet.getX(0.0), packet.getY(0.0), packet.getZ(0.0),
                            yaw, pitch, onGround, horizontalCollision);
                } else if (packet instanceof ServerboundMovePlayerPacket.Rot) {
                    replacement = new ServerboundMovePlayerPacket.Rot(yaw, pitch, onGround, horizontalCollision);
                } else {
                    return;
                }
                event.setPacket(replacement);
            }
        });
    }
}
