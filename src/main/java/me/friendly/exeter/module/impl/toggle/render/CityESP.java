package me.friendly.exeter.module.impl.toggle.render;

import java.util.ArrayList;
import java.util.List;

import me.friendly.api.event.Listener;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.LevelRenderEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.api.minecraft.render.RenderMethods;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * CityESP. Highlights the block under each enemy's feet that AutoCity would break — the
 * targeting overlay for the "city" move (drop them into a hole, then crystal).
 *
 * Color code: orange = cityable now, dim gray = hard block (obsidian/bedrock, needs Allow
 * Obsidian in AutoCity). Friends are never highlighted.
 */
public class CityESP extends ToggleableModule {
    private final NumberProperty<Double> range = new NumberProperty<>(6.0, 2.0, 16.0, "Range", "range", "r");
    private final Property<Boolean> showHard = new Property<>(true, "Show Hard Blocks", "hard", "h");

    private static final int COLOR_CITYABLE = 0x66FFA500; // orange
    private static final int COLOR_HARD = 0x44888888;     // dim gray

    public CityESP() {
        super("CityESP", new String[]{"cityesp", "cesp"}, ModuleType.RENDER);
        offerProperties(range, showHard);

        this.listeners.add(new Listener<LevelRenderEvent>("city_esp_render") {
            @Override
            public void call(LevelRenderEvent event) {
                render(event.getCameraPos());
            }
        });
    }

    private void render(Vec3 cameraPos) {
        if (minecraft.player == null || minecraft.level == null) return;

        for (BlockPos pos : collectTargets()) {
            AABB box = new AABB(pos).move(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            int c = isHard(pos) ? COLOR_HARD : COLOR_CITYABLE;
            org.lwjgl.opengl.GL11.glColor4f(
                    ((c >> 16) & 0xFF) / 255.0f,
                    ((c >> 8) & 0xFF) / 255.0f,
                    (c & 0xFF) / 255.0f,
                    ((c >> 24) & 0xFF) / 255.0f);
            RenderMethods.drawOutlinedBox(box);
        }
    }

    private List<BlockPos> collectTargets() {
        List<BlockPos> out = new ArrayList<>();
        for (Player p : minecraft.level.players()) {
            if (p == minecraft.player || !p.isAlive()) continue;
            if (Exeter.getInstance().getFriendManager().isFriend(p.getName().getString())) continue;
            if (minecraft.player.distanceTo(p) > range.getValue()) continue;

            BlockPos under = p.blockPosition().below();
            var state = minecraft.level.getBlockState(under);
            if (state.isAir()) continue;
            if (isHard(under) && !showHard.getValue()) continue;
            out.add(under.immutable());
        }
        return out;
    }

    private boolean isHard(BlockPos pos) {
        Block block = minecraft.level.getBlockState(pos).getBlock();
        return block == Blocks.BEDROCK || block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN;
    }
}
