package me.friendly.exeter.module.impl.toggle.render;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.events.WorldRenderEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.clickgui.ClickGui;
import me.friendly.exeter.module.impl.toggle.render.clickgui.SearchSelectPopup;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.PopupProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.render.EspRenderManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class BlockEsp extends ToggleableModule {
  private final NumberProperty<Double> range =
      new NumberProperty<Double>(64.0, 8.0, 256.0, "Range");
  private final NumberProperty<Float> lineWidth =
      new NumberProperty<Float>(2.0f, 0.5f, 5.0f, "Line Width");
  private final EnumProperty<RenderMode> renderMode =
      new EnumProperty<RenderMode>(RenderMode.BOTH, "Render", "mode");
  private final Property<Boolean> useCustomAlpha =
      new Property<Boolean>(false, "Custom Alpha", "CustomAlpha");
  private final NumberProperty<Float> fillAlpha =
      new NumberProperty<Float>(60f, 0f, 255f, "Fill Alpha", "FillAlpha");
  private final NumberProperty<Float> outlineAlpha =
      new NumberProperty<Float>(255f, 0f, 255f, "Outline Alpha", "OutlineAlpha");
  private final NumberProperty<Integer> rescanInterval =
      new NumberProperty<Integer>(10, 1, 50, "Rescan Ticks");
  private final PopupProperty selectBlocks;
  private final Property<String> selectedBlocksProp = new Property<>("", "Selected Blocks");

  private final Set<String> selectedBlocks = new HashSet<>();
  private final List<Block> blockCache = new ArrayList<>();
  private final Set<BlockPos> matchedPositions = new HashSet<>();
  private int tickCounter = 0;

  public BlockEsp() {
    super("BlockEsp", new String[] {"blockesp", "block-esp"}, 0xFF00FF, ModuleType.RENDER);
    setDescription("Highlights specific blocks in the world.");
    this.selectBlocks = new PopupProperty("Select Blocks", this::openBlockPopup);
    offerProperties(range, lineWidth, renderMode, useCustomAlpha, fillAlpha, outlineAlpha, rescanInterval, selectedBlocksProp, selectBlocks);

    this.listeners.add(
        new Listener<TickEvent>("block_esp_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            onTick();
          }
        });

    this.listeners.add(
        new Listener<WorldRenderEvent>("block_esp_render") {
          @Override
          public void call(WorldRenderEvent event) {
            onRender();
          }
        });
  }

  private void openBlockPopup() {
    List<SearchSelectPopup.ToggleItem> items = new ArrayList<>();

    net.minecraft.core.registries.BuiltInRegistries.BLOCK.stream()
        .filter(block -> block != Blocks.AIR && block != Blocks.VOID_AIR && block != Blocks.CAVE_AIR)
        .sorted((a, b) -> {
          String aName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(a).getPath();
          String bName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).getPath();
          return aName.compareTo(bName);
        })
        .forEach(block -> {
          String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();
          String displayName = block.getName().getString();
          items.add(
              new SearchSelectPopup.ToggleItem() {
                @Override
                public String getLabel() {
                  return displayName + " [" + id + "]";
                }

                @Override
                public boolean isEnabled() {
                  return selectedBlocks.contains(id);
                }

                @Override
                public void setEnabled(boolean enabled) {
                  if (enabled) {
                    selectedBlocks.add(id);
                  } else {
                    selectedBlocks.remove(id);
                  }
                }
              });
        });

    ClickGui.getClickGui()
        .openPopup(
            new SearchSelectPopup(
                "Select Blocks",
                items,
                () -> {
                  saveSelections();
                  syncBlockCache();
                  ClickGui.getClickGui().closePopup();
                },
                () -> ClickGui.getClickGui().closePopup()));
  }

  private void syncBlockCache() {
    blockCache.clear();
    for (String id : selectedBlocks) {
      var key = Identifier.tryParse(id);
      if (key != null) {
        var ref = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(key);
        if (ref.isPresent()) {
          var block = ref.get().value();
          if (block != null && block != Blocks.AIR) {
            blockCache.add(block);
          }
        }
      }
    }
    matchedPositions.clear();
    tickCounter = 0;
  }

  private void loadSelections() {
    selectedBlocks.clear();
    String raw = selectedBlocksProp.getValue();
    if (raw != null && !raw.isEmpty()) {
      for (String id : raw.split(",")) {
        String trimmed = id.trim();
        if (!trimmed.isEmpty()) {
          selectedBlocks.add(trimmed);
        }
      }
    }
  }

  private void saveSelections() {
    selectedBlocksProp.setValue(String.join(",", selectedBlocks));
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    loadSelections();
    syncBlockCache();
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    matchedPositions.clear();
    tickCounter = 0;
  }

  private void onTick() {
    tickCounter++;
    if (tickCounter < rescanInterval.getValue()) return;
    tickCounter = 0;

    if (minecraft.level == null || minecraft.player == null) return;
    if (blockCache.isEmpty()) {
      matchedPositions.clear();
      return;
    }

    int rangeBlocks = (int) Math.ceil(range.getValue());
    int minX = minecraft.player.blockPosition().getX() - rangeBlocks;
    int minZ = minecraft.player.blockPosition().getZ() - rangeBlocks;
    int maxX = minecraft.player.blockPosition().getX() + rangeBlocks;
    int maxZ = minecraft.player.blockPosition().getZ() + rangeBlocks;
    int minY = minecraft.player.blockPosition().getY() - rangeBlocks;
    int maxY = minecraft.player.blockPosition().getY() + rangeBlocks;

    int minChunkX = minX >> 4;
    int minChunkZ = minZ >> 4;
    int maxChunkX = maxX >> 4;
    int maxChunkZ = maxZ >> 4;

    Set<BlockPos> newMatches = new HashSet<>();
    for (int cx = minChunkX; cx <= maxChunkX; cx++) {
      for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
        if (!minecraft.level.hasChunk(cx, cz)) continue;
        var chunk = minecraft.level.getChunk(cx, cz);
        int sectionMinY = Math.max(minY >> 4, minecraft.level.getMinSectionY());
        int sectionMaxY = Math.min(maxY >> 4, minecraft.level.getMaxSectionY());
        int minSectionIndex = chunk.getMinSectionY();
        for (int sy = sectionMinY; sy <= sectionMaxY; sy++) {
          int sectionIndex = sy - minSectionIndex;
          if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) continue;
          var section = chunk.getSection(sectionIndex);
          int sectionY = sy << 4;
          for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
              for (int y = 0; y < 16; y++) {
                int worldY = sectionY + y;
                if (worldY < minY || worldY > maxY) continue;
                int worldX = (cx << 4) + x;
                int worldZ = (cz << 4) + z;
                if (worldX < minX || worldX > maxX || worldZ < minZ || worldZ > maxZ) continue;
                if (section.hasOnlyAir()) continue;
                BlockState state = section.getBlockState(x, y, z);
                if (blockCache.contains(state.getBlock())) {
                  newMatches.add(new BlockPos(worldX, worldY, worldZ));
                }
              }
            }
          }
        }
      }
    }
    matchedPositions.clear();
    matchedPositions.addAll(newMatches);
  }

  private void onRender() {
    if (matchedPositions.isEmpty()) return;

    float lw = lineWidth.getValue();
    boolean filled = renderMode.getValue() == RenderMode.FILLED || renderMode.getValue() == RenderMode.BOTH;
    boolean outlined = renderMode.getValue() == RenderMode.OUTLINED || renderMode.getValue() == RenderMode.BOTH;

    int fillAlphaVal = useCustomAlpha.getValue() ? Math.round(fillAlpha.getValue()) : EspRenderManager.getGlobalFillAlpha();
    int outlineAlphaVal = useCustomAlpha.getValue() ? Math.round(outlineAlpha.getValue()) : EspRenderManager.getGlobalOutlineAlpha();

    int color = EspRenderManager.getClientColor();
    int fillColor = ARGB.color(fillAlphaVal, color);
    int outlineColor = ARGB.color(outlineAlphaVal, color);

    GizmoStyle style;
    if (filled && outlined) {
      style = GizmoStyle.strokeAndFill(outlineColor, lw, fillColor);
    } else if (filled) {
      style = GizmoStyle.fill(fillColor);
    } else {
      style = GizmoStyle.stroke(outlineColor, lw);
    }

    for (BlockPos pos : matchedPositions) {
      Gizmos.cuboid(pos, style);
    }
  }

  public Set<String> getSelectedBlocks() {
    return selectedBlocks;
  }

  private static enum RenderMode {
    FILLED,
    OUTLINED,
    BOTH;
  }
}
