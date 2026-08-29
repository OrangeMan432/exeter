package me.friendly.exeter.module.impl.toggle.render;

import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.hud.HudEditorScreen;

public final class HUDEditor
extends ToggleableModule {
    public HUDEditor() {
        super("HUDEditor", new String[]{"hudeditor", "hudedit"}, ModuleType.RENDER);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        this.minecraft.gui.setScreen(HudEditorScreen.getInstance());
        this.setRunning(false);
    }
}
