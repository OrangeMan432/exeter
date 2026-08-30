package me.friendly.exeter.module.impl.toggle.misc;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.world.level.GameType;

/**
 * AutoRespawn. Instantly respawns after death (5b5t: get back to the fight/grind without the
 * death screen eating seconds).
 *
 * How: while {@code player.isDeadOrDying()} and a client level exists, send the vanilla
 * PERFORM_RESPAWN client command after {@code Delay Ticks} — the exact packet the death
 * screen's Respawn button sends ({@code ServerboundClientCommandPacket}).
 *
 * Delay: default 2 ticks — long enough for the death screen to appear (so kits/commands that
 * trigger on respawn see a natural flow), short enough to lose nothing.
 */
public class AutoRespawn extends ToggleableModule {
    private final NumberProperty<Integer> delay = new NumberProperty<>(2, 0, 40, "Delay Ticks", "delay", "d");

    private int deathTicks = -1;

    public AutoRespawn() {
        super("AutoRespawn", new String[]{"autorespawn", "respawn"}, ModuleType.MISCELLANEOUS);
        offerProperties(delay);

        this.listeners.add(new Listener<TickEvent>("auto_respawn_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer player = minecraft.player;
                if (player == null || minecraft.getConnection() == null) {
                    deathTicks = -1;
                    return;
                }

                if (!player.isDeadOrDying()) {
                    deathTicks = -1;
                    return;
                }

                if (deathTicks < 0) deathTicks = 0;
                if (deathTicks++ >= delay.getValue()) {
                    deathTicks = -1;
                    minecraft.getConnection().send(new ServerboundClientCommandPacket(
                            ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                }
            }
        });
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        deathTicks = -1;
    }
}
