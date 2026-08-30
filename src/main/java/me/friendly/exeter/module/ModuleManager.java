package me.friendly.exeter.module;

import java.util.ArrayList;
import me.friendly.api.interfaces.Toggleable;
import me.friendly.api.registry.ListRegistry;
import me.friendly.exeter.config.Config;
import me.friendly.exeter.config.ExeterConfig;
import me.friendly.exeter.core.Exeter;
import org.lwjgl.glfw.GLFW;
import me.friendly.exeter.module.impl.active.combat.AntiAim;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.module.impl.active.render.Hud;
import me.friendly.exeter.module.impl.toggle.combat.AntiHoleCamper;
import me.friendly.exeter.module.impl.toggle.combat.AntiCrystal;
import me.friendly.exeter.module.impl.toggle.combat.NoRotate;
import me.friendly.exeter.module.impl.toggle.combat.AutoPot;
import me.friendly.exeter.module.impl.toggle.combat.AutoTotem;
import me.friendly.exeter.module.impl.toggle.combat.BedAura;
import me.friendly.exeter.module.impl.toggle.combat.SelfBed;
import me.friendly.exeter.module.impl.toggle.combat.Velocity;
import me.friendly.exeter.module.impl.toggle.movement.SpeedPlus;
import me.friendly.exeter.module.impl.toggle.movement.AntiVoid;
import me.friendly.exeter.module.impl.toggle.movement.AutoJump;
import me.friendly.exeter.module.impl.toggle.movement.AirJump;
import me.friendly.exeter.module.impl.toggle.movement.ElytraFly;
import me.friendly.exeter.module.impl.toggle.movement.FastFall;
import me.friendly.exeter.module.impl.toggle.movement.NoFall;
import me.friendly.exeter.module.impl.toggle.movement.SafeWalk;
import me.friendly.exeter.module.impl.toggle.render.ClickGui;
import me.friendly.exeter.module.impl.toggle.render.TabGui;
import me.friendly.exeter.module.impl.toggle.render.HUDEditor;
import me.friendly.exeter.module.impl.toggle.render.CustomCrosshair;
import me.friendly.exeter.module.impl.toggle.render.ESP;
import me.friendly.exeter.module.impl.toggle.render.Fullbright;
import me.friendly.exeter.module.impl.toggle.render.NameTags;
import me.friendly.exeter.module.impl.toggle.render.NoHurtCam;
import me.friendly.exeter.module.impl.toggle.render.Tracers;
import me.friendly.exeter.module.impl.toggle.misc.ShulkerDupe;
import me.friendly.exeter.module.impl.toggle.misc.AutoItemDupe;
import me.friendly.exeter.module.impl.toggle.misc.DonkeyDupe;
import me.friendly.exeter.module.impl.toggle.misc.AntiAFK;
import me.friendly.exeter.module.impl.toggle.misc.AutoArmor;
import me.friendly.exeter.module.impl.toggle.misc.AutoEat;
import me.friendly.exeter.module.impl.toggle.misc.ChatSpam;
import me.friendly.exeter.module.impl.toggle.misc.InventoryResync;
import me.friendly.exeter.module.impl.toggle.misc.Refill;
import me.friendly.exeter.module.impl.toggle.misc.Sprint;
import me.friendly.exeter.module.impl.toggle.misc.NoSlow;
import me.friendly.exeter.module.impl.toggle.misc.AutoTool;
import me.friendly.exeter.module.impl.toggle.misc.AutoRespawn;
import me.friendly.exeter.module.impl.toggle.misc.Timer;
import me.friendly.exeter.module.impl.toggle.misc.FastEat;
import me.friendly.exeter.module.impl.toggle.misc.MidClickPearl;
import me.friendly.exeter.module.impl.toggle.combat.AutoCrystal;
import me.friendly.exeter.module.impl.toggle.render.NoWeather;
import me.friendly.exeter.module.impl.toggle.render.ItemHighlight;
import me.friendly.exeter.module.impl.toggle.world.AutoShulker;
import me.friendly.exeter.module.impl.toggle.world.AutoMine;
import me.friendly.exeter.module.impl.toggle.world.FastPlace;
import me.friendly.exeter.module.impl.toggle.world.Scaffold;

/**
 * Manages {@link Module}s for Exeter.
 */
public final class ModuleManager extends ListRegistry<Module> {

    public ModuleManager() {
        this.registry = new ArrayList();

        register(new Hud());
        register(new ClickGui());
        register(new TabGui());
        register(new Colors());
        register(new HUDEditor());
        register(new AntiAim());
        register(new ShulkerDupe());
        register(new AutoItemDupe());
        register(new DonkeyDupe());
        register(new Refill());
        register(new InventoryResync());
        register(new AutoArmor());
        register(new AutoEat());
        register(new Sprint());
        register(new AutoTotem());
        register(new SelfBed());
        register(new BedAura());
        register(new SpeedPlus());
        register(new AntiHoleCamper());
        register(new AutoPot());
        register(new AutoShulker());
        register(new Velocity());
        // --- render batch (26.2 pipeline) ---
        register(new Fullbright());
        register(new ESP());
        register(new Tracers());
        register(new NameTags());
        register(new NoHurtCam());
        register(new CustomCrosshair());
        // --- movement batch ---
        register(new AutoJump());
        register(new AirJump());
        register(new NoFall());
        register(new FastFall());
        register(new SafeWalk());
        register(new AntiVoid());
        register(new ElytraFly());
        // --- 5b5t-tailored batch ---
        register(new AntiCrystal());
        register(new NoRotate());
        register(new FastPlace());
        register(new AntiAFK());
        register(new ChatSpam());
        register(new AutoMine());
        register(new Scaffold());
        // --- batch 2 (5b5t essentials) ---
        register(new NoSlow());
        register(new AutoTool());
        register(new AutoRespawn());
        register(new Timer());
        register(new FastEat());
        register(new MidClickPearl());
        register(new AutoCrystal());
        register(new NoWeather());
        register(new ItemHighlight());
        this.registry.sort((mod1, mod2) -> mod1.getLabel().compareTo(mod2.getLabel()));

        Exeter.getInstance().getKeybindManager().getKeybindByLabel("Click Gui").setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);

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
