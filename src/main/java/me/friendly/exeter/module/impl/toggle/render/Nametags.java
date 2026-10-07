package me.friendly.exeter.module.impl.toggle.render;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.WorldRenderEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.clickgui.SearchSelectPopup;
import me.friendly.exeter.module.impl.toggle.render.clickgui.SelectionPopup;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.PopupProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.TotemPopTracker;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Billboard nametags through walls for selected entity types: name plus health everywhere, and the
 * full TextRadar info (health, totem pops, distance) on players.
 */
public class Nametags extends ToggleableModule {

  private final NumberProperty<Double> range =
      new NumberProperty<Double>(64.0, 8.0, 256.0, "Range");
  private final NumberProperty<Float> textScale =
      new NumberProperty<Float>(0.4f, 0.25f, 4.0f, "Text Scale");
  private final Property<Boolean> playersAlways = new Property<Boolean>(true, "Players", "players");
  private final PopupProperty selectEntities;
  private final SelectionPopup.Ids entitySelections = new SelectionPopup.Ids("Selected Entities");

  public Nametags() {
    super("Nametags", new String[] {"nametags", "nametag"}, 0xFFFF55, ModuleType.RENDER);
    setDescription("Nametags through walls, with health and radar info on players.");
    this.selectEntities = new PopupProperty("Select Entities", this::openEntityPopup);
    offerProperties(
        range, textScale, playersAlways, entitySelections.getProperty(), selectEntities);
    this.listeners.add(
        new Listener<WorldRenderEvent>("nametags_render") {
          @Override
          public void call(WorldRenderEvent event) {
            onRender();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    entitySelections.load();
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "enabled");
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "disabled");
  }

  private void openEntityPopup() {
    List<SearchSelectPopup.ToggleItem> items = new ArrayList<>();
    BuiltInRegistries.ENTITY_TYPE.stream()
        .filter(type -> type != EntityTypes.PLAYER)
        .sorted(
            (a, b) -> {
              String aName = BuiltInRegistries.ENTITY_TYPE.getKey(a).getPath();
              String bName = BuiltInRegistries.ENTITY_TYPE.getKey(b).getPath();
              return aName.compareTo(bName);
            })
        .forEach(
            entityType -> {
              String id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString();
              String displayName = entityType.getDescription().getString();
              items.add(SelectionPopup.idItem(id, displayName, entitySelections.getSelected()));
            });
    SelectionPopup.open("Select Entities", items, () -> entitySelections.save());
  }

  private static char healthCode(float health) {
    if (health > 16.0f) return 'a';
    if (health > 12.0f) return '6';
    if (health > 8.0f) return 'e';
    return 'c';
  }

  private void onRender() {
    if (minecraft.level == null || minecraft.player == null) return;
    float scale = textScale.getValue();
    TotemPopTracker tracker = TotemPopTracker.getInstance();

    for (Entity entity : minecraft.level.entitiesForRendering()) {
      if (!shouldTag(entity)) continue;
      LivingEntity living = (LivingEntity) entity;

      float health = living.getHealth() + living.getAbsorptionAmount();
      char hpCode = healthCode(health);
      String info;
      if (entity instanceof Player player) {
        int pops = tracker.getPops(player);
        int dist = (int) Math.round(minecraft.player.distanceTo(entity));
        info =
            "§"
                + hpCode
                + String.format("%.1f", health)
                + " §f"
                + pops
                + " "
                + entity.getName().getString()
                + " §7"
                + dist
                + "m";
      } else {
        info = "§f" + entity.getName().getString() + " §" + hpCode + String.format("%.1f", health);
      }

      double tagY = entity.getY() + living.getBbHeight() + 0.5;
      me.friendly.exeter.render.TagRenderer.drawTag(
          new Vec3(entity.getX(), tagY, entity.getZ()),
          java.util.List.of(info),
          java.util.List.of(0xFFFFFFFF),
          scale);
    }
  }

  /** True when the module is running and this entity gets one of our tags. */
  public static boolean shouldTag(Entity entity) {
    if (Exeter.getInstance() == null) return false;
    var manager = Exeter.getInstance().getModuleManager();
    if (manager == null) return false;
    Nametags module = null;
    for (var mod : manager.getRegistry()) {
      if (mod instanceof Nametags nametags) {
        module = nametags;
        break;
      }
    }
    if (module == null || !module.isRunning()) return false;
    if (entity == null) return false;
    if (entity == net.minecraft.client.Minecraft.getInstance().player) return false;
    if (!entity.isAlive()) return false;
    if (!(entity instanceof LivingEntity)) return false;
    double rangeSq = module.range.getValue() * module.range.getValue();
    var self = net.minecraft.client.Minecraft.getInstance().player;
    if (self == null || self.distanceToSqr(entity) > rangeSq) return false;
    if (entity instanceof Player) {
      return module.playersAlways.getValue();
    }
    String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    return module.entitySelections.getSelected().contains(typeId);
  }
}
