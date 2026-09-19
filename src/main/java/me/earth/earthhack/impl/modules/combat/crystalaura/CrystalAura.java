package me.earth.earthhack.impl.modules.combat.crystalaura;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.ColorSetting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;
import me.earth.earthhack.impl.util.math.StopWatch;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;

import java.awt.*;

/**
 * Compact crystal aura in the Wurst+3 style.
 * Ported from Wurst+3 (CrystalAura).
 */
public class CrystalAura extends Module
{
    protected final Setting<Double> breakRange =
        register(new NumberSetting<>("BreakRange", 5.0, 0.0, 6.0));
    protected final Setting<Double> placeRange =
        register(new NumberSetting<>("PlaceRange", 5.0, 0.0, 6.0));
    protected final Setting<Double> wallRange =
        register(new NumberSetting<>("WallRange", 3.0, 0.0, 6.0));
    protected final Setting<Double> targetRange =
        register(new NumberSetting<>("TargetRange", 12.0, 0.0, 20.0));
    protected final Setting<Integer> placeDelay =
        register(new NumberSetting<>("PlaceDelay", 0, 0, 10));
    protected final Setting<Integer> breakDelay =
        register(new NumberSetting<>("BreakDelay", 0, 0, 10));
    protected final Setting<Float> minPlace =
        register(new NumberSetting<>("MinPlace", 4.0f, 0.0f, 20.0f));
    protected final Setting<Float> maxSelfPlace =
        register(new NumberSetting<>("MaxSelfPlace", 8.0f, 0.0f, 20.0f));
    protected final Setting<Float> minBreak =
        register(new NumberSetting<>("MinBreak", 4.0f, 0.0f, 20.0f));
    protected final Setting<Float> maxSelfBreak =
        register(new NumberSetting<>("MaxSelfBreak", 8.0f, 0.0f, 20.0f));
    protected final Setting<Boolean> antiSuicide =
        register(new BooleanSetting("AntiSuicide", true));
    protected final Setting<Rotate> rotate =
        register(new EnumSetting<>("Rotate", Rotate.Off));
    protected final Setting<Switch> switchMode =
        register(new EnumSetting<>("Switch", Switch.Normal));
    protected final Setting<Integer> maxCrystals =
        register(new NumberSetting<>("MaxCrystals", 1, 1, 4));
    protected final Setting<Placements> placements =
        register(new EnumSetting<>("Placements", Placements.Damage));
    protected final Setting<Boolean> render =
        register(new BooleanSetting("Render", true));
    protected final Setting<Color> renderColor =
        register(new ColorSetting("Color", new Color(255, 0, 255, 120)));

    final StopWatch placeTimer = new StopWatch();
    final StopWatch breakTimer = new StopWatch();
    EntityPlayer target;
    BlockPos renderPos;

    public CrystalAura()
    {
        super("CrystalAura", Category.Combat);
        this.listeners.add(new ListenerMotion(this));
        this.listeners.add(new ListenerRender(this));
        this.setData(new SimpleData(this,
            "Wurst-style crystal aura."));
    }

    @Override
    protected void onDisable()
    {
        target = null;
        renderPos = null;
    }

    public enum Rotate
    {
        Off,
        Silent
    }

    public enum Switch
    {
        Normal,
        Packet,
        None
    }

    public enum Placements
    {
        Damage,
        Nearby
    }
}
