package me.friendly.exeter.module.impl.toggle.render;

import java.util.ArrayList;
import java.util.List;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.LevelRenderEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import me.friendly.api.minecraft.render.RenderMethods;
import net.minecraft.core.BlockPos;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Trajectories. Predicts and draws the flight path + landing point of thrown items
 * (ender pearl, XP bottle, snowball, bow/crossbow arrow, trident) — the aim tool for
 * pearl-clutching onto towers and XP-potting crystal fights.
 *
 * Physics: vanilla projectile drag/gravity — per tick: vel = vel * drag - gravity;
 * drag 0.99 (pearl/snowball/xp), arrow 0.95 with gravity 0.05; pearl gravity 0.03.
 * Path is simulated until the position enters a solid block (landing marker) or
 * Max Ticks elapses.
 */
public class Trajectories extends ToggleableModule {
    private final Property<Boolean> pearl = new Property<>(true, "Pearl", "pearl", "p");
    private final Property<Boolean> xp = new Property<>(true, "XP Bottle", "xp", "x");
    private final Property<Boolean> arrows = new Property<>(true, "Arrows", "arrows", "a");
    private final Property<Boolean> others = new Property<>(false, "Snowballs & Trident", "others", "o");
    private final Property<Boolean> drawPath = new Property<>(true, "Draw Path", "path", "pa");
    private final Property<Boolean> drawLanding = new Property<>(true, "Draw Landing", "landing", "l");
    private final Property<Boolean> otherPlayers = new Property<>(true, "Other Players", "otherplayers", "op");

    private static final int COLOR_PATH = 0x66FFFFFF;
    private static final int COLOR_LAND = 0x88FF5555;

    public Trajectories() {
        super("Trajectories", new String[]{"trajectories", "traj", "projectilepath"}, ModuleType.RENDER);
        offerProperties(pearl, xp, arrows, others, drawPath, drawLanding, otherPlayers);

        this.listeners.add(new Listener<LevelRenderEvent>("trajectories_render") {
            @Override
            public void call(LevelRenderEvent event) {
                render(event.getCameraPos());
            }
        });
    }

    private void render(Vec3 cameraPos) {
        if (minecraft.player == null || minecraft.level == null) return;

        List<net.minecraft.world.entity.player.Player> holders = new ArrayList<>();
        holders.add(minecraft.player);
        if (otherPlayers.getValue()) {
            for (var p : minecraft.level.players()) {
                if (p != minecraft.player && p.isAlive()) holders.add(p);
            }
        }

        for (net.minecraft.world.entity.player.Player holder : holders) {
            Item item = projectileItem(holder);
            if (item == null || !enabledFor(item)) continue;

            Vec3 pos = new Vec3(holder.getX(), holder.getEyeY(), holder.getZ());
            // Vanilla spawn offset: 0.5 forward along look.
            Vec3 look = holder.getViewVector(1.0f);
            pos = pos.add(look.scale(0.5));

            float power = launchPower(holder);
            if (power <= 0) continue;
            Vec3 vel = look.scale(power);

            simulateAndDraw(pos, vel, item, cameraPos);
        }
    }

    private Item projectileItem(net.minecraft.world.entity.player.Player holder) {
        ItemStack main = holder.getMainHandItem();
        ItemStack off = holder.getOffhandItem();
        Item m = main.isEmpty() ? null : main.getItem();
        Item o = off.isEmpty() ? null : off.getItem();
        if (isProjectile(m)) return m;
        if (isProjectile(o)) return o;
        return null;
    }

    private boolean isProjectile(Item item) {
        return item instanceof EnderpearlItem || item instanceof ExperienceBottleItem
                || item instanceof SnowballItem || item instanceof BowItem
                || item instanceof CrossbowItem || item instanceof TridentItem;
    }

    private boolean enabledFor(Item item) {
        if (item instanceof EnderpearlItem) return pearl.getValue();
        if (item instanceof ExperienceBottleItem) return xp.getValue();
        if (item instanceof BowItem || item instanceof CrossbowItem) return arrows.getValue();
        return others.getValue(); // snowball / trident
    }

    /** Charge power 0..1 for bows (draw progress), 1.0 for instant throws. */
    private float launchPower(net.minecraft.world.entity.player.Player holder) {
        ItemStack main = holder.getMainHandItem();
        if (!main.isEmpty() && main.getItem() instanceof BowItem && holder.isUsingItem()) {
            int useTicks = holder.getUseItemRemainingTicks();
            float charge = Math.max(0.0f, (main.getUseDuration(holder) - useTicks) / 20.0f);
            float power = (charge * charge + charge * 2.0f) / 3.0f;
            if (power > 1.0f) power = 1.0f;
            if (power < 0.1f) return 0; // not drawn enough to fly
            return power * 3.0f; // vanilla arrow launch velocity
        }
        if (!main.isEmpty() && main.getItem() instanceof CrossbowItem) return 3.15f;
        if (!main.isEmpty() && main.getItem() instanceof TridentItem) return 2.5f;
        // Pearl / XP / snowball: fixed vanilla velocities.
        if (main != null && !main.isEmpty()) {
            Item m = main.getItem();
            if (m instanceof ExperienceBottleItem) return 0.7f;
            if (m instanceof EnderpearlItem || m instanceof SnowballItem) return 1.5f;
        }
        Item o = holder.getOffhandItem().isEmpty() ? null : holder.getOffhandItem().getItem();
        if (o instanceof ExperienceBottleItem) return 0.7f;
        if (o instanceof EnderpearlItem || o instanceof SnowballItem) return 1.5f;
        return 0;
    }

    /** Simulate vanilla projectile physics and draw the path + landing box. */
    private void simulateAndDraw(Vec3 pos, Vec3 vel, Item item, Vec3 cameraPos) {
        boolean isArrow = item instanceof BowItem || item instanceof CrossbowItem;
        boolean isXp = item instanceof ExperienceBottleItem;
        double gravity = isArrow ? 0.05 : (isXp ? 0.07 : 0.03);
        double drag = isArrow ? 0.95 : 0.99;

        Vec3 prev = pos;
        Vec3 cur = pos;
        Vec3 curVel = vel;

        for (int tick = 0; tick < 300; tick++) {
            prev = cur;
            cur = cur.add(curVel);
            curVel = new Vec3(curVel.x * drag, curVel.y * drag - gravity, curVel.z * drag);

            if (drawPath.getValue()) {
                AABB seg = segmentBox(prev, cur).move(-cameraPos.x, -cameraPos.y, -cameraPos.z);
                org.lwjgl.opengl.GL11.glColor4f(
                        ((COLOR_PATH >> 16) & 0xFF) / 255.0f,
                        ((COLOR_PATH >> 8) & 0xFF) / 255.0f,
                        (COLOR_PATH & 0xFF) / 255.0f,
                        ((COLOR_PATH >> 24) & 0xFF) / 255.0f);
                RenderMethods.drawOutlinedBox(seg);
            }

            // Hit a solid block → landing point.
            var state = minecraft.level.getBlockState(BlockPos.containing(cur.x, cur.y, cur.z));
            if (!state.isAir() && !state.canBeReplaced()
                    && !state.getCollisionShape(minecraft.level, BlockPos.containing(cur.x, cur.y, cur.z)).isEmpty()) {
                if (drawLanding.getValue()) {
                    AABB land = new AABB(
                            BlockPos.containing(cur.x, cur.y, cur.z)).move(-cameraPos.x, -cameraPos.y, -cameraPos.z);
                    org.lwjgl.opengl.GL11.glColor4f(
                            ((COLOR_LAND >> 16) & 0xFF) / 255.0f,
                            ((COLOR_LAND >> 8) & 0xFF) / 255.0f,
                            (COLOR_LAND & 0xFF) / 255.0f,
                            ((COLOR_LAND >> 24) & 0xFF) / 255.0f);
                    RenderMethods.drawOutlinedBox(land);
                }
                return;
            }
        }
    }

    private static AABB segmentBox(Vec3 a, Vec3 b) {
        return new AABB(
                Math.min(a.x, b.x) - 0.05, Math.min(a.y, b.y) - 0.05, Math.min(a.z, b.z) - 0.05,
                Math.max(a.x, b.x) + 0.05, Math.max(a.y, b.y) + 0.05, Math.max(a.z, b.z) + 0.05);
    }
}
