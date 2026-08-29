package sh.orangeman.weirdpvp;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import sh.orangeman.weirdpvp.commands.AutoGearCommand;
import sh.orangeman.weirdpvp.hud.EatTimerHud;
import sh.orangeman.weirdpvp.modules.AntiHoleCamper;
import sh.orangeman.weirdpvp.modules.AutoCart;
import sh.orangeman.weirdpvp.modules.AutoGear;
import sh.orangeman.weirdpvp.modules.AutoPot;
import sh.orangeman.weirdpvp.modules.AutoShulker;
import sh.orangeman.weirdpvp.modules.BedAura;
import sh.orangeman.weirdpvp.modules.SelfBed;
import sh.orangeman.weirdpvp.modules.SpeedPlus;
import org.slf4j.Logger;

public class WeirdPvP extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("WeirdPvP");

    @Override
    public void onInitialize() {
        LOG.info("Initializing WeirdPvP Addon");

        Modules.get().add(new AntiHoleCamper());
        Modules.get().add(new AutoCart());
        Modules.get().add(new AutoShulker());
        Modules.get().add(new AutoPot());
        Modules.get().add(new AutoGear());
        Modules.get().add(new BedAura());
        Modules.get().add(new SelfBed());
        Modules.get().add(new SpeedPlus());
        Commands.add(new AutoGearCommand());

        Hud.get().register(EatTimerHud.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "sh.orangeman.weirdpvp";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("orangeman432", "meteor-weirdpvp");
    }
}
