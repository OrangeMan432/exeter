package me.larp.client.module;

import java.util.ArrayList;
import me.larp.api.registry.ListRegistry;
import me.larp.client.config.Config;
import me.larp.client.config.LarpConfig;
import me.larp.client.core.Larp;
import me.larp.client.module.impl.active.render.Colors;
import me.larp.client.module.impl.active.render.Hud;
import me.larp.client.module.impl.toggle.client.Debug;
import me.larp.client.module.impl.toggle.client.DiscordRPC;
import me.larp.client.module.impl.toggle.client.TestModule;
import me.larp.client.module.impl.toggle.combat.AnchorAura;
import me.larp.client.module.impl.toggle.combat.AntiRegear;
import me.larp.client.module.impl.toggle.combat.AutoArmor;
import me.larp.client.module.impl.toggle.combat.AutoCart;
import me.larp.client.module.impl.toggle.combat.AutoEat;
import me.larp.client.module.impl.toggle.combat.AutoPot;
import me.larp.client.module.impl.toggle.combat.AutoTotem;
import me.larp.client.module.impl.toggle.combat.AutoTrap;
import me.larp.client.module.impl.toggle.combat.AutoWeb;
import me.larp.client.module.impl.toggle.combat.AutoXP;
import me.larp.client.module.impl.toggle.combat.BedAura;
import me.larp.client.module.impl.toggle.combat.BlockLag;
import me.larp.client.module.impl.toggle.combat.CevBreaker;
import me.larp.client.module.impl.toggle.combat.Criticals;
import me.larp.client.module.impl.toggle.combat.CrystalAura;
import me.larp.client.module.impl.toggle.combat.HoleFill;
import me.larp.client.module.impl.toggle.combat.HolePush;
import me.larp.client.module.impl.toggle.combat.KillAura;
import me.larp.client.module.impl.toggle.combat.PistonCrystal;
import me.larp.client.module.impl.toggle.combat.PistonPush;
import me.larp.client.module.impl.toggle.combat.SelfBed;
import me.larp.client.module.impl.toggle.combat.SelfProtect;
import me.larp.client.module.impl.toggle.combat.SelfTrap;
import me.larp.client.module.impl.toggle.combat.Surround;
import me.larp.client.module.impl.toggle.combat.TNTAura;
import me.larp.client.module.impl.toggle.misc.AutoGear;
import me.larp.client.module.impl.toggle.misc.AutoItemDupe;
import me.larp.client.module.impl.toggle.misc.AutoLog;
import me.larp.client.module.impl.toggle.misc.AutoTool;
import me.larp.client.module.impl.toggle.misc.ChestStealer;
import me.larp.client.module.impl.toggle.misc.PingSpoof;
import me.larp.client.module.impl.toggle.misc.Replenish;
import me.larp.client.module.impl.toggle.misc.ShulkerDupe;
import me.larp.client.module.impl.toggle.movement.ElytraFly;
import me.larp.client.module.impl.toggle.movement.FakeLag;
import me.larp.client.module.impl.toggle.movement.FastFall;
import me.larp.client.module.impl.toggle.movement.HoleSnap;
import me.larp.client.module.impl.toggle.movement.Jesus;
import me.larp.client.module.impl.toggle.movement.LongJump;
import me.larp.client.module.impl.toggle.movement.NoBedStep;
import me.larp.client.module.impl.toggle.movement.NoFall;
import me.larp.client.module.impl.toggle.movement.NoSlow;
import me.larp.client.module.impl.toggle.movement.PacketFly;
import me.larp.client.module.impl.toggle.movement.Parkour;
import me.larp.client.module.impl.toggle.movement.SafeWalk;
import me.larp.client.module.impl.toggle.movement.Scaffold;
import me.larp.client.module.impl.toggle.movement.Spider;
import me.larp.client.module.impl.toggle.movement.AntiVoid;
import me.larp.client.module.impl.toggle.movement.Speed;
import me.larp.client.module.impl.toggle.movement.Sprint;
import me.larp.client.module.impl.toggle.movement.Step;
import me.larp.client.module.impl.toggle.movement.TargetStrafe;
import me.larp.client.module.impl.toggle.movement.Velocity;
import me.larp.client.module.impl.toggle.render.ClickGui;
import me.larp.client.module.impl.toggle.render.EatTimer;
import me.larp.client.module.impl.toggle.render.HUDEditor;
import me.larp.client.module.impl.toggle.render.HoleESP;
import me.larp.client.module.impl.toggle.render.Nametags;
import me.larp.client.module.impl.toggle.render.StorageESP;
import me.larp.client.module.impl.toggle.render.TabGui;
import me.larp.client.module.impl.toggle.render.Tracers;
import me.larp.client.module.impl.toggle.world.AutoMine;
import me.larp.client.module.impl.toggle.world.AutoShulker;
import me.larp.client.module.impl.toggle.world.InstaMine;
import me.larp.client.module.impl.toggle.world.SpeedMine;
import org.lwjgl.glfw.GLFW;

/** Manages {@link Module}s for Larp. */
public final class ModuleManager extends ListRegistry<Module> {

  public ModuleManager() {
    this.registry = new ArrayList();

    register(new Hud());
    register(new ClickGui());
    register(new TabGui());
    register(new EatTimer());
    register(new Colors());
    register(new HUDEditor());
    register(new HoleESP());
    register(new Tracers());
    register(new StorageESP());
    register(new Nametags());
    register(new DiscordRPC());
    register(new AutoItemDupe());
    register(new AutoTotem());
    register(new AnchorAura());
    register(new AntiRegear());
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
    register(new AutoTool());
    register(new PingSpoof());
    register(new Replenish());
    register(new ShulkerDupe());
    register(new SelfBed());
    register(new BedAura());
    register(new CevBreaker());
    register(new Speed());
    register(new PistonPush());
    register(new PistonCrystal());
    register(new HolePush());
    register(new TNTAura());
    register(new SelfProtect());
    register(new AutoCart());
    register(new AutoPot());
    register(new AutoShulker());
    register(new AutoMine());
    register(new InstaMine());
    register(new SpeedMine());
    register(new Velocity());
    register(new FakeLag());
    register(new FastFall());
    register(new HoleSnap());
    register(new Jesus());
    register(new LongJump());
    register(new ElytraFly());
    register(new NoFall());
    register(new NoSlow());
    register(new PacketFly());
    register(new Parkour());
    register(new SafeWalk());
    register(new Scaffold());
    register(new Spider());
    register(new AntiVoid());
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

    Larp.getInstance()
        .getKeybindManager()
        .getKeybindByLabel("ClickGui")
        .setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);

    new Config("module_configurations") {

      @Override
      public void load(Object... source) {
        LarpConfig.getInstance().loadAll();
      }

      @Override
      public void save(Object... destination) {
        LarpConfig.getInstance().saveAll();
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
