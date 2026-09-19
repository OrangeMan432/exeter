package me.earth.earthhack.impl.modules.render.burrowesp;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.ColorSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

import java.awt.*;

/**
 * Boxes players standing inside blocks.
 * Ported from Lemon (BurrowESP).
 */
public class BurrowESP extends Module
{
    protected final Setting<Boolean> showSelf =
        register(new BooleanSetting("Self", true));
    protected final Setting<Color> selfColor =
        register(new ColorSetting("SelfColor", new Color(0, 255, 0, 255)));
    protected final Setting<Boolean> showFriends =
        register(new BooleanSetting("Friends", true));
    protected final Setting<Color> friendColor =
        register(new ColorSetting("FriendColor", new Color(0, 0, 255, 255)));
    protected final Setting<Boolean> showEnemies =
        register(new BooleanSetting("Enemies", true));
    protected final Setting<Color> enemyColor =
        register(new ColorSetting("EnemyColor", new Color(255, 0, 0, 255)));
    protected final Setting<Float> height =
        register(new NumberSetting<>("Height", 1.0f, 0.0f, 1.0f));

    public BurrowESP()
    {
        super("BurrowESP", Category.Render);
        this.listeners.add(new ListenerRender(this));
        this.setData(new SimpleData(this,
            "Highlights burrowed players."));
    }
}
