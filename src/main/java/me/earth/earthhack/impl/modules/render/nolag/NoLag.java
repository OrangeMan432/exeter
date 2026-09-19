package me.earth.earthhack.impl.modules.render.nolag;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Removes laggy entities and chat spam for FPS.
 * Ported from Mio 0.6.9 (NoLag).
 */
public class NoLag extends Module
{
    protected final Setting<Boolean> antiSpam =
        register(new BooleanSetting("AntiSpam", true));
    protected final Setting<Boolean> skulls =
        register(new BooleanSetting("WitherSkulls", false));
    protected final Setting<Boolean> tnt =
        register(new BooleanSetting("PrimedTNT", false));
    protected final Setting<Boolean> scoreboards =
        register(new BooleanSetting("ScoreBoards", true));
    protected final Setting<Boolean> glowing =
        register(new BooleanSetting("GlowingEntities", true));
    protected final Setting<Boolean> parrots =
        register(new BooleanSetting("Parrots", true));

    public NoLag()
    {
        super("NoLag", Category.Render);
        this.listeners.add(new ListenerTick(this));
        this.listeners.add(new ListenerChat(this));
        this.listeners.add(new ListenerRenderEntity(this));
        this.setData(new SimpleData(this,
            "Removes FPS killers: skulls, TNT, parrots, spam."));
    }
}
