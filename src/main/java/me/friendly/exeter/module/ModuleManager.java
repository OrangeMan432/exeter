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
import me.friendly.exeter.module.impl.toggle.combat.AutoCart;
import me.friendly.exeter.module.impl.toggle.combat.AutoPot;
import me.friendly.exeter.module.impl.toggle.combat.AutoTotem;
import me.friendly.exeter.module.impl.toggle.combat.BedAura;
import me.friendly.exeter.module.impl.toggle.combat.SelfBed;
import me.friendly.exeter.module.impl.toggle.movement.Speed;
import me.friendly.exeter.module.impl.toggle.combat.PistonPush;
import me.friendly.exeter.module.impl.toggle.movement.Velocity;
import me.friendly.exeter.module.impl.toggle.render.ClickGui;
import me.friendly.exeter.module.impl.toggle.render.EatTimer;
import me.friendly.exeter.module.impl.toggle.render.TabGui;
import me.friendly.exeter.module.impl.toggle.render.HUDEditor;
import me.friendly.exeter.module.impl.toggle.misc.ShulkerDupe;
import me.friendly.exeter.module.impl.toggle.misc.AutoItemDupe;
import me.friendly.exeter.module.impl.toggle.misc.DonkeyDupe;
import me.friendly.exeter.module.impl.toggle.misc.AutoGear;
import me.friendly.exeter.module.impl.toggle.world.AutoShulker;

/**
 * Manages {@link Module}s for Exeter.
 */
public final class ModuleManager extends ListRegistry<Module> {

    public ModuleManager() {
        this.registry = new ArrayList();

        register(new Hud());
        register(new ClickGui());
        register(new TabGui());
        register(new EatTimer());
        register(new Colors());
        register(new HUDEditor());
        register(new AntiAim());
        register(new ShulkerDupe());
        register(new AutoItemDupe());
        register(new DonkeyDupe());
        register(new AutoTotem());
        register(new AutoGear());
        register(new SelfBed());
        register(new BedAura());
        register(new Speed());
        register(new PistonPush());
        register(new AutoCart());
        register(new AutoPot());
        register(new AutoShulker());
        register(new Velocity());
        this.registry.sort((mod1, mod2) -> mod1.getLabel().compareTo(mod2.getLabel()));

        Exeter.getInstance().getKeybindManager().getKeybindByLabel("ClickGui").setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);

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

