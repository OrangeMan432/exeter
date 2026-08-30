package me.friendly.exeter.module.impl.toggle.render;

import java.util.ArrayList;
import java.util.List;

import me.friendly.api.event.Listener;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.LevelRenderEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.api.minecraft.render.RenderMethods;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Entity ESP. Draws a world-space box around entities through walls.
 *
 * Rebuilt for 26.2's render pipeline: drawing happens in
 * {@code LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES}, camera-relative — the hook hands us the
 * camera position and GL is still in the world frame the vanilla renderer set up for the
 * translucent pass, so boxes track entities exactly as the vanilla renderer places them
 * (render states carry interpolated positions — no manual interpolation).
 *
 * Filters: players (friends cyan), monsters, animals, items, ender pearls, invisibles.
 * Color: distance fade green→red within 32 blocks, friends always cyan.
 */
public class ESP extends ToggleableModule {
    private final Property<Boolean> players = new Property<>(true, "Players", "player", "p");
    private final Property<Boolean> monsters = new Property<>(false, "Monsters", "monster", "m");
    private final Property<Boolean> animals = new Property<>(false, "Animals", "animal", "a");
    private final Property<Boolean> items = new Property<>(false, "Items", "item", "i");
    private final Property<Boolean> pearls = new Property<>(true, "Pearls", "pearl", "epearl");
    private final Property<Boolean> invisibles = new Property<>(true, "Invisibles", "invis", "inv");
    private final EnumProperty<BoxMode> mode = new EnumProperty<>(BoxMode.OUTLINE, "Mode", "m");

    public ESP() {
        super("ESP", new String[]{"esp", "entityesp", "eesp"}, ModuleType.RENDER);
        offerProperties(players, monsters, animals, items, pearls, invisibles, mode);

        this.listeners.add(new Listener<LevelRenderEvent>("esp_level_render") {
            @Override
            public void call(LevelRenderEvent event) {
                render(event.getCameraPos());
            }
        });
    }

    private void render(Vec3 cameraPos) {
        if (minecraft.player == null || minecraft.level == null) return;

        for (Entity e : collectTargets()) {
            AABB box = e.getBoundingBox().move(-cameraPos.x, -cameraPos.y, -cameraPos.z);

            // Apply the per-entity color (friend cyan / distance fade) before drawing.
            int c = colorFor(e);
            org.lwjgl.opengl.GL11.glColor4f(
                    ((c >> 16) & 0xFF) / 255.0f,
                    ((c >> 8) & 0xFF) / 255.0f,
                    (c & 0xFF) / 255.0f,
                    ((c >> 24) & 0xFF) / 255.0f);

            if (mode.getValue() == BoxMode.FILL) {
                RenderMethods.drawBox(box);
            } else if (mode.getValue() == BoxMode.CROSS) {
                RenderMethods.renderCrosses(box);
            }
            RenderMethods.drawOutlinedBox(box);
        }
    }

    private List<Entity> collectTargets() {
        List<Entity> out = new ArrayList<>();
        for (Entity e : minecraft.level.entitiesForRendering()) {
            if (e != minecraft.player && isValid(e)) out.add(e);
        }
        return out;
    }

    private boolean isValid(Entity e) {
        if (e instanceof ItemEntity) return items.getValue();
        if (e instanceof ThrownEnderpearl) return pearls.getValue();
        if (e instanceof Player p) {
            if (!p.isAlive()) return false;
            if (p.isInvisible() && !invisibles.getValue()) return false;
            return players.getValue();
        }
        if (e instanceof LivingEntity living) {
            if (!living.isAlive()) return false;
            if (e.isSpectator()) return false;
            if (living instanceof net.minecraft.world.entity.monster.Monster) return monsters.getValue();
            if (living instanceof net.minecraft.world.entity.animal.Animal) return animals.getValue();
            return false;
        }
        return false;
    }

    private int colorFor(Entity e) {
        if (e instanceof Player p
                && Exeter.getInstance().getFriendManager().isFriend(p.getName().getString())) {
            return 0x7345B9FF; // friend cyan
        }
        float dist = minecraft.player.distanceTo(e);
        if (dist <= 32.0f) {
            int red = (int) (255 * (dist / 32.0f));
            return 0x7300FF00 | (red << 16);
        }
        return 0x7300E600;
    }

    private enum BoxMode { OUTLINE, FILL, CROSS }
}
