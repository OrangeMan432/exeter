package me.friendly.exeter.module.impl.toggle.render;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.LevelRenderEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.api.minecraft.render.RenderMethods;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * ItemHighlight. Highlights recently-dropped items (5b5t: spot drops from killed players,
 * dupe outputs, or tossed gapples instantly — they show through walls).
 *
 * Tracks ItemEntity first-seen times client-side; anything younger than {@code Age Ticks} gets
 * a filled + outlined box in gold. Older drops are untouched unless {@code All Items} is on.
 *
 * Uses the same world-space pass as ESP (LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES).
 */
public class ItemHighlight extends ToggleableModule {
    private final NumberProperty<Integer> ageTicks = new NumberProperty<>(100, 10, 600, "Age Ticks", "age");
    private final Property<Boolean> allItems = new Property<>(false, "All Items", "all", "a");

    private final Map<UUID, Long> firstSeen = new ConcurrentHashMap<>();

    public ItemHighlight() {
        super("ItemHighlight", new String[]{"itemhighlight", "drophighlight", "ih"}, ModuleType.RENDER);
        offerProperties(ageTicks, allItems);

        this.listeners.add(new Listener<LevelRenderEvent>("item_highlight_render") {
            @Override
            public void call(LevelRenderEvent event) {
                if (minecraft.player == null || minecraft.level == null) return;
                Vec3 cam = event.getCameraPos();

                for (Entity entity : minecraft.level.entitiesForRendering()) {
                    if (!(entity instanceof ItemEntity e)) continue;

                    long seen = firstSeen.computeIfAbsent(e.getUUID(), k -> System.currentTimeMillis());
                    boolean fresh = System.currentTimeMillis() - seen <= ageTicks.getValue() * 50L;
                    if (!fresh && !allItems.getValue()) continue;

                    AABB box = e.getBoundingBox().move(-cam.x, -cam.y, -cam.z);
                    RenderMethods.drawBox(box);
                    RenderMethods.drawOutlinedBox(box);
                }

                // Forget stale entries so the map doesn't grow forever.
                if (firstSeen.size() > 512) {
                    firstSeen.clear();
                }
            }
        });
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        firstSeen.clear();
    }
}
