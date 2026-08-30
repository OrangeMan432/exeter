package me.friendly.exeter.module.impl.toggle.render;

import me.friendly.api.event.Listener;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.LevelRenderEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.api.minecraft.render.RenderMethods;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.Vec3;

/**
 * Tracers. Draws a line from the camera to each valid entity.
 *
 * Ported from the Exeter 1.12.2 Tracers module (commented-out decompile in that repo), rebuilt
 * for 26.2: the level-render pass leaves GL in the world frame with the modelview already set
 * up by the vanilla renderer, so lines go from the camera origin (0,0,0) straight to the
 * entity's interpolated position — the same geometry the old
 * {@code orientCamera + glVertex3d} path produced, minus the deprecated matrix juggling.
 *
 * Filters mirror ESP: players (friends cyan), monsters, animals, items, pearls, invisibles.
 */
public class Tracers extends ToggleableModule {
    private final Property<Boolean> players = new Property<>(true, "Players", "player", "p");
    private final Property<Boolean> monsters = new Property<>(false, "Monsters", "monster", "m");
    private final Property<Boolean> animals = new Property<>(false, "Animals", "animal", "a");
    private final Property<Boolean> items = new Property<>(false, "Items", "item", "i");
    private final Property<Boolean> pearls = new Property<>(true, "Pearls", "pearl", "epearl");
    private final Property<Boolean> invisibles = new Property<>(true, "Invisibles", "invis", "inv");
    private final NumberProperty<Float> width = new NumberProperty<>(1.5f, 1.0f, 5.0f, "Width", "w");

    public Tracers() {
        super("Tracers", new String[]{"tracers", "tracelines"}, ModuleType.RENDER);
        offerProperties(players, monsters, animals, items, pearls, invisibles, width);

        this.listeners.add(new Listener<LevelRenderEvent>("tracers_level_render") {
            @Override
            public void call(LevelRenderEvent event) {
                render(event.getCameraPos());
            }
        });
    }

    private void render(Vec3 cameraPos) {
        if (minecraft.player == null || minecraft.level == null) return;

        RenderMethods.enableGL3D(width.getValue());
        try {
            for (Entity e : minecraft.level.entitiesForRendering()) {
                if (e == minecraft.player || !isValid(e)) continue;

                Vec3 target = e.position().subtract(cameraPos)
                        .add(0, e.getBbHeight() * 0.5, 0);
                int c = colorFor(e);

                org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_LINES);
                org.lwjgl.opengl.GL11.glColor4f(
                        ((c >> 16) & 0xFF) / 255.0f,
                        ((c >> 8) & 0xFF) / 255.0f,
                        (c & 0xFF) / 255.0f,
                        0.8f);
                org.lwjgl.opengl.GL11.glVertex3d(0, 0, 0);
                org.lwjgl.opengl.GL11.glVertex3d(target.x, target.y, target.z);
                org.lwjgl.opengl.GL11.glEnd();
            }
        } finally {
            RenderMethods.disableGL3D();
        }
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
            return 0x45B9FF;
        }
        float dist = minecraft.player.distanceTo(e);
        if (dist <= 32.0f) {
            int red = (int) (255 * (dist / 32.0f));
            return (red << 16) | 0xFF00;
        }
        return 0x00E600;
    }
}
