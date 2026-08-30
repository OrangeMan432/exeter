package me.friendly.exeter.module.impl.toggle.render;

import java.util.List;

import me.friendly.api.event.Listener;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.LevelRenderEvent;
import me.friendly.exeter.events.RenderGameOverlayEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import me.friendly.api.minecraft.render.RenderMethods;
import net.minecraft.client.gui.Font;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

/**
 * NameTags. Draws a text plate above every player with name + health, visible through walls.
 *
 * Ported from the Exeter 1.12.2 NameTags module (decompiled, commented out there), rebuilt for
 * 26.2: the level-render hook captures the camera position + projection/view-rotation matrices
 * each frame (via the raw context on {@link LevelRenderEvent}); plates are projected to screen
 * space and drawn with {@code GuiGraphics} text/fill inside the HUD overlay event — same visual
 * result as the old GL-scaled plates, without the legacy matrix stack.
 *
 * Shows: name (friend alias in cyan), health with color coding (green→red as it drops),
 * sneaking marker. Invisible players are skipped unless allowed.
 */
public class NameTags extends ToggleableModule {
    private final Property<Boolean> health = new Property<>(true, "Health", "h", "hp");
    private final Property<Boolean> heart = new Property<>(true, "Heart", "heart");
    private final Property<Boolean> invisibles = new Property<>(false, "Invisibles", "invis", "inv");

    // Captured from the last level-render pass (ExeterLevelRenderHook fires each frame).
    private Vec3 lastCameraPos;
    private org.joml.Matrix4f lastProjection;
    private org.joml.Matrix4f lastViewRotation;

    public NameTags() {
        super("NameTags", new String[]{"nametags", "tags", "nameplates"}, ModuleType.RENDER);
        offerProperties(health, heart, invisibles);

        Exeter.getInstance().getEventManager().register(new Listener<LevelRenderEvent>("nametags_capture") {
            @Override
            public void call(LevelRenderEvent event) {
                Object raw = event.getContextRaw();
                if (!(raw instanceof net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext ctx)) return;
                var state = ctx.levelState();
                if (state == null || state.cameraRenderState == null) return;
                var cam = state.cameraRenderState;
                if (cam.pos != null && cam.projectionMatrix != null && cam.viewRotationMatrix != null) {
                    lastCameraPos = cam.pos;
                    lastProjection = new org.joml.Matrix4f(cam.projectionMatrix);
                    lastViewRotation = new org.joml.Matrix4f(cam.viewRotationMatrix);
                }
            }
        });

        this.listeners.add(new Listener<RenderGameOverlayEvent>("nametags_overlay") {
            @Override
            public void call(RenderGameOverlayEvent event) {
                if (event.getType() != RenderGameOverlayEvent.Type.IN_GAME) return;
                renderPlates();
            }
        });
    }

    private void renderPlates() {
        if (minecraft.player == null || minecraft.level == null) return;
        if (lastCameraPos == null || lastProjection == null || lastViewRotation == null) return;

        Font font = minecraft.font;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();

        List<? extends Player> players = minecraft.level.players();
        for (Player p : players) {
            if (p == minecraft.player || !p.isAlive()) continue;
            if (p.isInvisible() && !invisibles.getValue()) continue;

            Vec3 rel = p.position().subtract(lastCameraPos).add(0, p.getBbHeight() + 0.35, 0);
            Vector4f clip = new Vector4f((float) rel.x, (float) rel.y, (float) rel.z, 1.0f);
            lastViewRotation.transform(clip);
            lastProjection.transform(clip);
            if (clip.w() <= 0.0f) continue;

            float sx = (sw / 2.0f) + (clip.x() / clip.w()) * (sw / 2.0f);
            float sy = (sh / 2.0f) - (clip.y() / clip.w()) * (sh / 2.0f);

            String text = buildText(p);
            int tw = font.width(text);
            float x = sx - tw / 2.0f;
            float y = sy - 10;

            RenderMethods.drawRect(x - 2, y - 1, x + tw + 2, y + 9, 0x90000000);
            RenderMethods.guiGraphics.text(font, text, (int) x, (int) y, colorFor(p), true);
        }
    }

    private String buildText(Player p) {
        String name = p.getName().getString();
        if (Exeter.getInstance().getFriendManager().isFriend(name)) {
            var f = Exeter.getInstance().getFriendManager().getFriendByAliasOrLabel(name);
            if (f != null) name = f.getAlias();
        }
        if (p.isShiftKeyDown()) name = name + " *";

        if (!health.getValue()) return name;

        float hp = p.getHealth();
        String color;
        if (hp > 18.0f) color = "\u00a7a";
        else if (hp > 16.0f) color = "\u00a72";
        else if (hp > 12.0f) color = "\u00a7e";
        else if (hp > 8.0f) color = "\u00a76";
        else if (hp > 5.0f) color = "\u00a7c";
        else color = "\u00a74";

        int shown = (int) Math.ceil(hp);
        String suffix = color + (hp > 0 ? String.valueOf(shown) : "dead");
        if (heart.getValue()) suffix = suffix + " \u2764";
        return name + " " + suffix;
    }

    private int colorFor(Player p) {
        if (Exeter.getInstance().getFriendManager().isFriend(p.getName().getString())) {
            return 0xFF45B9FF;
        }
        if (p.isInvisible()) return 0xFF888888;
        if (p.isShiftKeyDown()) return 0xFFAAAAAA;
        return 0xFFEEEEEE;
    }
}
