package me.friendly.exeter.module.impl.toggle.render;

import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;

public class EntityEsp extends ToggleableModule {

  private final NumberProperty<Double> range =
      new NumberProperty<Double>(64.0, 8.0, 256.0, "Range", "range");
  private final NumberProperty<Float> lineWidth =
      new NumberProperty<Float>(2.0F, 0.5F, 5.0F, "Line Width", "linewidth");
  private final Property<Boolean> players = new Property<Boolean>(true, "Players", "players");
  private final Property<Boolean> passive = new Property<Boolean>(false, "Passive", "passive");
  private final Property<Boolean> hostile = new Property<Boolean>(true, "Hostile", "hostile");

  public EntityEsp() {
    super("EntityEsp", new String[] {"entityesp", "entity-esp"}, 0x00FFFF, ModuleType.RENDER);
    setDescription("Highlights entities in the world.");
    offerProperties(range, players, passive, hostile);
  }

  public static EntityEsp get() {
    if (Exeter.getInstance() == null) return null;
    Module module = Exeter.getInstance().getModuleManager().getModuleByAlias("entityesp");
    return module instanceof EntityEsp ? (EntityEsp) module : null;
  }

  public static boolean isActive() {
    EntityEsp esp = get();
    return esp != null && esp.isRunning();
  }

  public double getRange() {
    return range.getValue().doubleValue();
  }

  public float getLineWidth() {
    return lineWidth.getValue().floatValue();
  }

  public boolean showPlayers() {
    return players.getValue().booleanValue();
  }

  public boolean showPassive() {
    return passive.getValue().booleanValue();
  }

  public boolean showHostile() {
    return hostile.getValue().booleanValue();
  }
}
