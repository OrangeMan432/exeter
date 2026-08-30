package me.friendly.exeter.module.impl.toggle.combat;

import java.util.Set;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;

/**
 * NoRotate. Blocks the server from turning your camera.
 *
 * 5b5t anticheat corrections and crystal knockbacks ship a
 * {@code ClientboundPlayerPositionPacket} whose {@link Relative} set includes
 * {@code Y_ROT}/{@code X_ROT} — the client snaps your view. This module rewrites the packet's
 * rotation to the player's CURRENT view, so the "requested" rotation equals what you already
 * see: no snap, no rubber-band.
 *
 * Only In Combat: restricts the override to the hurt window ({@code hurtTime > 0}), leaving
 * normal teleports/portals snapping as vanilla.
 */
public class NoRotate extends ToggleableModule {
    private final Property<Boolean> onlyInCombat = new Property<>(false, "Only In Combat", "combat", "c");

    public NoRotate() {
        super("NoRotate", new String[]{"norotate", "rotationlock"}, ModuleType.COMBAT);
        offerProperties(onlyInCombat);

        this.listeners.add(new Listener<PacketEvent>("no_rotate_receive") {
            @Override
            public void call(PacketEvent event) {
                LocalPlayer player = minecraft.player;
                if (player == null) return;
                if (!(event.getPacket() instanceof ClientboundPlayerPositionPacket packet)) return;

                Set<Relative> relatives = packet.relatives();
                boolean rotates = relatives.contains(Relative.Y_ROT) || relatives.contains(Relative.X_ROT);
                if (!rotates) return;
                if (onlyInCombat.getValue() && player.hurtTime <= 0) return;

                PositionMoveRotation change = packet.change();
                PositionMoveRotation kept = change.withRotation(player.getYRot(), player.getXRot());

                event.setPacket(new ClientboundPlayerPositionPacket(packet.id(), kept, relatives));
            }
        });
    }
}
