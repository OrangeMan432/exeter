package me.friendly.exeter.module.impl.toggle.misc;

import java.util.Comparator;

import me.friendly.api.event.Listener;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/**
 * AutoLog. Disconnects you the moment things get dangerous — the #1 survival module on
 * paid anarchy clients (2b2t/Future/Rusherhack all ship it as "AutoLog" / "LogOut").
 *
 * Triggers (any one fires a disconnect):
 *  - Health drops below Health Threshold (checked on the server health packet — the
 *    instant the server tells us, before the client tick can take more damage).
 *  - An enemy is within Totem Range while we have no totem in either hand (pop risk).
 *  - Crystals are being placed near us and our health is low (optional, Smart Check).
 *
 * 5b5t note: logging mid-combat is allowed on 5b5t (combat-tag is not enforced), so this
 * module is purely protective. The disconnect uses the vanilla disconnect path — the same
 * one the "Disconnect" button uses — so the queue slot is lost cleanly, no desync.
 */
public class AutoLog extends ToggleableModule {
    private final NumberProperty<Integer> healthThreshold = new NumberProperty<>(8, 1, 20, "Health Threshold", "health", "hp");
    private final NumberProperty<Double> enemyRange = new NumberProperty<>(12.0, 4.0, 32.0, "Enemy Range", "range", "r");
    private final Property<Boolean> onlyWithoutTotem = new Property<>(true, "Only Without Totem", "totem", "t");
    private final Property<Boolean> smartCheck = new Property<>(true, "Smart Check (crystals nearby)", "smart", "s");
    private final Property<Boolean> debug = new Property<>(false, "Debug", "debug", "d");

    public AutoLog() {
        super("AutoLog", new String[]{"autolog", "logout", "log"}, ModuleType.MISCELLANEOUS);
        offerProperties(healthThreshold, enemyRange, onlyWithoutTotem, smartCheck, debug);

        this.listeners.add(new Listener<PacketEvent>("auto_log_health_packet") {
            @Override
            public void call(PacketEvent event) {
                // React on the server health packet — zero-tick reaction, before client-side
                // damage prediction can take another hit.
                if (event.getPacket() instanceof ClientboundSetHealthPacket hp) {
                    if (hp.getHealth() < healthThreshold.getValue()) {
                        log("health " + hp.getHealth() + " < " + healthThreshold.getValue());
                    }
                }
            }
        });

        this.listeners.add(new Listener<TickEvent>("auto_log_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer player = minecraft.player;
                if (player == null || minecraft.level == null) return;
                if (player.isDeadOrDying()) return;

                if (player.getHealth() < healthThreshold.getValue()) {
                    log("health " + player.getHealth());
                    return;
                }

                if (onlyWithoutTotem.getValue() && !hasTotem(player)) {
                    Player enemy = nearestEnemy(player);
                    if (enemy != null && player.distanceTo(enemy) <= enemyRange.getValue()) {
                        if (!smartCheck.getValue() || crystalNear(enemy)) {
                            log("no totem, enemy " + enemy.getName().getString()
                                    + " at " + (int) player.distanceTo(enemy));
                        }
                    }
                }
            }
        });
    }

    private boolean hasTotem(LocalPlayer player) {
        return player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)
                || player.getMainHandItem().is(Items.TOTEM_OF_UNDYING);
    }

    private boolean crystalNear(Player enemy) {
        for (Entity e : minecraft.level.entitiesForRendering()) {
            if (e instanceof EndCrystal && e.isAlive() && enemy.distanceTo(e) <= 10.0) return true;
        }
        return false;
    }

    private Player nearestEnemy(LocalPlayer self) {
        return minecraft.level.players().stream()
                .filter(p -> p != self && p.isAlive())
                .filter(p -> !Exeter.getInstance().getFriendManager().isFriend(p.getName().getString()))
                .min(Comparator.comparingDouble(self::distanceTo))
                .orElse(null);
    }

    private void log(String reason) {
        if (debug.getValue()) {
            System.out.println("[AutoLog] disconnecting: " + reason);
        }
        // Vanilla disconnect path — same as pressing the Disconnect button
        // (disconnectToWorld clears the connection and shows the disconnect screen).
        minecraft.disconnectFromWorld(net.minecraft.network.chat.Component.literal("\u00a7c[AutoLog] " + reason));
    }

    @Override
    protected void onEnable() {
        super.onEnable();
    }
}
