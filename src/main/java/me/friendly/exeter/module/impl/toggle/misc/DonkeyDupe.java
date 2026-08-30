package me.friendly.exeter.module.impl.toggle.misc;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.phys.AABB;

/**
 * Donkey / llama dupe helper (chested-horse dupe).
 *
 * Automatically rides the nearest chested horse that has a chest equipped. On servers where the
 * chested-horse dupe is live (items in the horse's chest duplicate when the rider disconnects or
 * the chunk unloads while mounted), riding via this module sets up the dupe: put items in the
 * horse's chest, enable DonkeyDupe, and disconnect. This module only performs the mount; the dupe
 * itself is a server-side bug. Range controls how close the horse must be before auto-riding.
 */
public class DonkeyDupe extends ToggleableModule {
    private final Property<Double> range = new Property<>(3.5, "Range", "range");
    private final Property<Boolean> autoRide = new Property<>(true, "Auto Ride", "autoride");

    public DonkeyDupe() {
        super("DonkeyDupe", new String[]{"donkeydupe", "horsedupe", "dd"}, ModuleType.MISCELLANEOUS);
        offerProperties(range, autoRide);

        this.listeners.add(new Listener<TickEvent>("donkey_dupe_tick") {
            @Override
            public void call(TickEvent event) {
                if (event.getStage() != Stage.PRE) return;
                if (minecraft.player == null || minecraft.level == null) return;
                if (!autoRide.getValue() || minecraft.player.getVehicle() != null) return;

                AbstractChestedHorse target = null;
                double best = range.getValue();
                double r = range.getValue();
                AABB box = AABB.ofSize(minecraft.player.position(), r * 2.0, r * 2.0, r * 2.0);
                for (Entity e : minecraft.level.getEntities(minecraft.player, box, ent -> true)) {
                    if (e instanceof AbstractChestedHorse horse && horse.hasChest()) {
                        double d = minecraft.player.distanceTo(horse);
                        if (d <= best) {
                            best = d;
                            target = horse;
                        }
                    }
                }
                if (target != null) minecraft.player.startRiding(target);
            }
        });
    }
}
