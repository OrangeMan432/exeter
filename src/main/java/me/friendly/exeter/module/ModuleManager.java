package me.friendly.exeter.module;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import me.friendly.api.registry.ListRegistry;
import me.friendly.exeter.config.Config;
import me.friendly.exeter.config.ExeterConfig;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.module.impl.toggle.client.CustomFont;
import me.friendly.exeter.module.impl.toggle.client.Debug;
import me.friendly.exeter.module.impl.toggle.client.DiscordRPC;
import me.friendly.exeter.module.impl.toggle.client.HUDEditor;
import me.friendly.exeter.module.impl.toggle.client.Notifier;
import me.friendly.exeter.module.impl.toggle.client.TestModule;
import me.friendly.exeter.module.impl.toggle.client.WindowsModule;
import me.friendly.exeter.module.impl.toggle.combat.AutoArmor;
import me.friendly.exeter.module.impl.toggle.combat.AutoCart;
import me.friendly.exeter.module.impl.toggle.combat.AutoCrystal;
import me.friendly.exeter.module.impl.toggle.combat.AutoHoleMine;
import me.friendly.exeter.module.impl.toggle.combat.AutoMend;
import me.friendly.exeter.module.impl.toggle.combat.AutoPot;
import me.friendly.exeter.module.impl.toggle.combat.AutoTotem;
import me.friendly.exeter.module.impl.toggle.combat.AutoTrap;
import me.friendly.exeter.module.impl.toggle.combat.BedAura;
import me.friendly.exeter.module.impl.toggle.combat.ElytraTarget;
import me.friendly.exeter.module.impl.toggle.combat.FireworkAura;
import me.friendly.exeter.module.impl.toggle.combat.MaceDive;
import me.friendly.exeter.module.impl.toggle.combat.PacketMine;
import me.friendly.exeter.module.impl.toggle.combat.PistonCrystal;
import me.friendly.exeter.module.impl.toggle.combat.PistonPush;
import me.friendly.exeter.module.impl.toggle.combat.SelfBed;
import me.friendly.exeter.module.impl.toggle.combat.Surround;
import me.friendly.exeter.module.impl.toggle.misc.AutoFirework;
import me.friendly.exeter.module.impl.toggle.misc.AutoGear;
import me.friendly.exeter.module.impl.toggle.misc.AutoItemDupe;
import me.friendly.exeter.module.impl.toggle.misc.Replenish;
import me.friendly.exeter.module.impl.toggle.movement.AutoWalk;
import me.friendly.exeter.module.impl.toggle.movement.ElytraFly;
import me.friendly.exeter.module.impl.toggle.movement.FastFall;
import me.friendly.exeter.module.impl.toggle.movement.NoAccel;
import me.friendly.exeter.module.impl.toggle.movement.NoBedStep;
import me.friendly.exeter.module.impl.toggle.movement.NoFall;
import me.friendly.exeter.module.impl.toggle.movement.Speed;
import me.friendly.exeter.module.impl.toggle.movement.Sprint;
import me.friendly.exeter.module.impl.toggle.movement.Step;
import me.friendly.exeter.module.impl.toggle.movement.TargetStrafe;
import me.friendly.exeter.module.impl.toggle.movement.Velocity;
import me.friendly.exeter.module.impl.toggle.render.BlockEsp;
import me.friendly.exeter.module.impl.toggle.render.BlockHighlight;
import me.friendly.exeter.module.impl.toggle.render.BreakHighlight;
import me.friendly.exeter.module.impl.toggle.render.BurrowESP;
import me.friendly.exeter.module.impl.toggle.render.ClickGui;
import me.friendly.exeter.module.impl.toggle.render.EatTimer;
import me.friendly.exeter.module.impl.toggle.render.EntityEsp;
import me.friendly.exeter.module.impl.toggle.render.FreeLook;
import me.friendly.exeter.module.impl.toggle.render.FullBright;
import me.friendly.exeter.module.impl.toggle.render.HoleESP;
import me.friendly.exeter.module.impl.toggle.render.Nametags;
import me.friendly.exeter.module.impl.toggle.render.VoidESP;
import me.friendly.exeter.module.impl.toggle.render.Waypoints;
import me.friendly.exeter.module.impl.toggle.render.hud.HudRenderer;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.ArmorHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.ArrayListHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.BindListHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.CompassHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.CoordsHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.DirectionHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.FpsHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.MacroListHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.NotificationHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.PotionsHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.SpeedHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.TabGui;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.TextRadarHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.TimeHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.WatermarkHud;
import me.friendly.exeter.module.impl.toggle.world.AutoShulker;
import me.friendly.exeter.module.impl.toggle.world.fakeplayer.FakePlayerModule;

/** Manages {@link Module}s for Exeter. */
public final class ModuleManager extends ListRegistry<Module> {

  public ModuleManager() {
    this.registry = new ArrayList();

    register(new HudRenderer());
    register(new ClickGui());
    register(new TabGui());
    register(new EatTimer());
    register(new Colors());
    register(new CustomFont());
    register(new HUDEditor());
    register(new DiscordRPC());
    register(new AutoItemDupe());
    register(new AutoTotem());
    register(new AutoArmor());
    register(new AutoMend());
    register(new AutoGear());
    register(new Replenish());
    register(new AutoFirework());
    register(new ElytraTarget());
    register(new MaceDive());
    register(new SelfBed());
    register(new BedAura());
    register(new AutoTrap());
    register(new AutoCrystal());
    register(new Surround());
    register(new FireworkAura());
    register(new Speed());
    register(new NoAccel());
    register(new PistonPush());
    register(new PistonCrystal());
    register(new PacketMine());
    register(new AutoHoleMine());
    register(new AutoCart());
    register(new AutoPot());
    register(new AutoShulker());
    register(new FakePlayerModule());
    register(new Velocity());
    register(new FastFall());
    register(new ElytraFly());
    register(new NoBedStep());
    register(new NoFall());
    register(new Step());
    register(new Sprint());
    register(new TargetStrafe());
    register(new AutoWalk());
    register(new TestModule());
    register(new Debug());
    register(new Notifier());
    register(new WindowsModule());

    register(new WatermarkHud());
    register(new ArrayListHud());
    register(new MacroListHud());
    register(new BindListHud());
    register(new ArmorHud());
    register(new PotionsHud());
    register(new CoordsHud());
    register(new FpsHud());
    register(new TimeHud());
    register(new DirectionHud());
    register(new SpeedHud());
    register(new TextRadarHud());
    register(new NotificationHud());
    register(new CompassHud());
    register(new BlockEsp());
    register(new BlockHighlight());
    register(new BreakHighlight());
    register(new BurrowESP());
    register(new EntityEsp());
    register(new FreeLook());
    register(new FullBright());
    register(new HoleESP());
    register(new Nametags());
    register(new VoidESP());
    register(new Waypoints());

    for (Module m : registry) {
      if (m instanceof Debug debug) {
        debug.initModuleToggles(registry);
        break;
      }
    }

    Exeter.getInstance()
        .getKeybindManager()
        .getKeybindByLabel("ClickGui")
        .setKey(InputConstants.KEY_RSHIFT);

    Exeter.getInstance()
        .getKeybindManager()
        .getKeybindByLabel("Windows")
        .setKey(InputConstants.KEY_GRAVE);

    Exeter.getInstance()
        .getKeybindManager()
        .getKeybindByLabel("HUDEditor")
        .setKey(InputConstants.KEY_COMMA);

    new Config("module_configurations") {

      @Override
      public void load(Object... source) {
        ExeterConfig.getInstance().loadAll();
      }

      @Override
      public void save(Object... destination) {
        ExeterConfig.getInstance().saveAll();
      }
    };
  }

  public Module getModuleByAlias(String alias) {
    for (Module module : registry) {
      for (String moduleAlias : module.getAliases()) {
        if (!alias.equalsIgnoreCase(moduleAlias)) continue;
        return module;
      }
    }
    return null;
  }
}
