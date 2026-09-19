package me.earth.earthhack.impl.modules.combat.anticrystal;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Places blocks on dangerous crystals before they pop you.
 * Ported from GS++ (AntiCrystal).
 */
public class AntiCrystal extends Module
{
    protected final Setting<Double> rangePlace =
        register(new NumberSetting<>("RangePlace", 5.9, 0.0, 6.0));
    protected final Setting<Double> enemyRange =
        register(new NumberSetting<>("EnemyRange", 12.0, 0.0, 20.0));
    protected final Setting<Double> damageMin =
        register(new NumberSetting<>("DamageMin", 4.0, 0.0, 15.0));
    protected final Setting<Double> biasDamage =
        register(new NumberSetting<>("BiasDamage", 1.0, 0.0, 3.0));
    protected final Setting<BlockPlace> blockPlace =
        register(new EnumSetting<>("BlockPlace", BlockPlace.String));
    protected final Setting<Integer> tickDelay =
        register(new NumberSetting<>("TickDelay", 5, 0, 10));
    protected final Setting<Integer> blocksPerTick =
        register(new NumberSetting<>("BlocksPerTick", 4, 0, 8));
    protected final Setting<Boolean> onlyIfEnemy =
        register(new BooleanSetting("OnlyIfEnemy", true));
    protected final Setting<Boolean> damageCheck =
        register(new BooleanSetting("DamageCheck", true));
    protected final Setting<Boolean> switchBack =
        register(new BooleanSetting("SwitchBack", true));

    int delayTicks;
    boolean sneaking;

    public AntiCrystal()
    {
        super("AntiCrystal", Category.Combat);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Blocks crystals that would hurt you."));
    }

    @Override
    protected void onDisable()
    {
        if (sneaking && mc.player != null)
        {
            mc.player.connection.sendPacket(
                new net.minecraft.network.play.client.CPacketEntityAction(
                    mc.player,
                    net.minecraft.network.play.client.CPacketEntityAction
                        .Action.STOP_SNEAKING));
            sneaking = false;
        }
    }

    public enum BlockPlace
    {
        Pressure,
        String
    }
}
