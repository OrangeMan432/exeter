package me.friendly.exeter.mixin.loader;

import net.fabricmc.api.ModInitializer;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

public class MixinLoader implements ModInitializer {

    public MixinLoader() {
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.exeter.json");
    }

    @Override
    public void onInitializeClient() {
        new MixinLoader();
    }
}