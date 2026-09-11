package me.friendly.exeter.module;

import java.util.ArrayList;
import me.friendly.api.registry.ListRegistry;
import me.friendly.exeter.config.Config;
import me.friendly.exeter.config.ExeterConfig;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.module.impl.active.render.Hud;
import me.friendly.exeter.module.impl.toggle.client.Debug;
import me.friendly.exeter.module.impl.toggle.client.DiscordRPC;
import me.friendly.exeter.module.impl.toggle.client.TestModule;
import me.friendly.exeter.module.impl.toggle.combat.AutoCart;
import me.friendly.exeter.module.impl.toggle.combat.AnchorAura;
import me.friendly.exeter.module.impl.toggle.combat.AutoArmor;
import me.friendly.exeter.module.impl.toggle.combat.AutoEat;
import me.friendly.exeter.module.impl.toggle.combat.AutoPot;
import me.friendly.exeter.module.impl.toggle.combat.AutoTotem;
import me.friendly.exeter.module.impl.toggle.combat.AutoTrap;
import me.friendly.exeter.module.impl.toggle.combat.AutoWeb;
import me.friendly.exeter.module.impl.toggle.combat.AutoXP;
import me.friendly.exeter.module.impl.toggle.combat.BedAura;
import me.friendly.exeter.module.impl.toggle.combat.BlockLag;
import me.friendly.exeter.module.impl.toggle.combat.CrystalAura;
import me.friendly.exeter.module.impl.toggle.combat.Criticals;
import me.friendly.exeter.module.impl.toggle.combat.HoleFill;
import me.friendly.exeter.module.impl.toggle.combat.KillAura;
import me.friendly.exeter.module.impl.toggle.combat.PistonPush;
import me.friendly.exeter.module.impl.toggle.combat.SelfBed;
import me.friendly.exeter.module.impl.toggle.combat.SelfTrap;
import me.friendly.exeter.module.impl.toggle.combat.Surround;
import me.friendly.exeter.module.impl.toggle.misc.AutoGear;
import me.friendly.exeter.module.impl.toggle.misc.AutoItemDupe;
import me.friendly.exeter.module.impl.toggle.misc.AutoLog;
import me.friendly.exeter.module.impl.toggle.misc.ChestStealer;
import me.friendly.exeter.module.impl.toggle.misc.Replenish;
import me.friendly.exeter.module.impl.toggle.movement.ElytraFly;
import me.friendly.exeter.module.impl.toggle.movement.FakeLag;
import me.friendly.exeter.module.impl.toggle.movement.FastFall;
import me.friendly.exeter.module.impl.toggle.movement.HoleSnap;
import me.friendly.exeter.module.impl.toggle.movement.Jesus;
import me.friendly.exeter.module.impl.toggle.movement.LongJump;
import me.friendly.exeter.module.impl.toggle.movement.NoBedStep;
import me.friendly.exeter.module.impl.toggle.movement.NoFall;
import me.friendly.exeter.module.impl.toggle.movement.NoSlow;
import me.friendly.exeter.module.impl.toggle.movement.Speed;
import me.friendly.exeter.module.impl.toggle.movement.Sprint;
import me.friendly.exeter.module.impl.toggle.movement.Step;
import me.friendly.exeter.module.impl.toggle.movement.TargetStrafe;
import me.friendly.exeter.module.impl.toggle.movement.Velocity;
import me.friendly.exeter.module.impl.toggle.render.ClickGui;
import me.friendly.exeter.module.impl.toggle.render.EatTimer;
import me.friendly.exeter.module.impl.toggle.render.HUDEditor;
import me.friendly.exeter.module.impl.toggle.render.TabGui;
import me.friendly.exeter.module.impl.toggle.world.AutoShulker;
import org.lwjgl.glfw.GLFW;

/** Manages {@link Module}s for Exeter. */
public final class ModuleManager extends ListRegistry<Module> {

  public ModuleManager() {
    this.registry = new ArrayList();

    register(new Hud());
    register(new ClickGui());
    register(new TabGui());
    register(new EatTimer());
    register(new Colors());
    register(new HUDEditor());
    register(new DiscordRPC());
    register(new AutoItemDupe());
    register(new AutoTotem());
    register(new AnchorAura());
    register(new AutoArmor());
    register(new AutoWeb());
    register(new AutoXP());
    register(new BlockLag());
    register(new SelfTrap());
    register(new HoleFill());
    register(new AutoTrap());
    register(new AutoEat());
    register(new Criticals());
    register(new KillAura());
    register(new CrystalAura());
    register(new Surround());
    register(new AutoGear());
    register(new AutoLog());
    register(new ChestStealer());
    register(new Replenish());
    register(new SelfBed());
    register(new BedAura());
    register(new Speed());
    register(new PistonPush());
    register(new AutoCart());
    register(new AutoPot());
    register(new AutoShulker());
    register(new Velocity());
    register(new FakeLag());
    register(new FastFall());
    register(new HoleSnap());
    register(new Jesus());
    register(new LongJump());
    register(new ElytraFly());
    register(new NoFall());
    register(new NoSlow());
    register(new NoBedStep());
    register(new Step());
    register(new Sprint());
    register(new TargetStrafe());
    register(new TestModule());
    register(new Debug());

    // Initialize per-module debug toggles after all modules are registered
    for (Module m : registry) {
      if (m instanceof Debug debug) {
        debug.initModuleToggles(registry);
        break;
      }
    }

    Exeter.getInstance()
        .getKeybindManager()
        .getKeybindByLabel("ClickGui")
        .setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);

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

  public <T extends Module> T getModule(Class<T> clazz) {
    for (Module module : registry) {
      if (clazz.isInstance(module)) {
        return clazz.cast(module);
      }
    }
    return null;
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
