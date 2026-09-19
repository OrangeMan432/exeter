package me.earth.earthhack.impl.modules.misc.kitdelete;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BindSetting;
import me.earth.earthhack.api.util.bind.Bind;
import me.earth.earthhack.impl.util.client.SimpleData;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import org.lwjgl.input.Keyboard;

/**
 * Sends /deleteukit for hovered kit pieces.
 * Ported from Phobos 1.9 (KitDelete).
 */
public class KitDelete extends Module
{
    protected final Setting<Bind> deleteKey =
        register(new BindSetting("Key", new Bind(-1)));

    boolean keyDown;

    public KitDelete()
    {
        super("KitDelete", Category.Misc);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Deletes kits with a keypress in GUIs."));
    }
}
