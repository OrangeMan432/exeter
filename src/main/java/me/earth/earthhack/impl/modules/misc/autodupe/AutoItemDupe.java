package me.earth.earthhack.impl.modules.misc.autodupe;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;
import me.earth.earthhack.impl.util.math.StopWatch;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;

/**
 * 5b5t crafting recipe desync dupe, automated.
 * Ported from ToxicAven 5b-AutoDupes (AutoItemDupe, Kotlin)
 * with the or4acle method updates. 5b5t only.
 */
public class AutoItemDupe extends Module
{
    protected final Setting<Mode> mode =
        register(new EnumSetting<>("Mode", Mode.HELD));
    protected final Setting<Boolean> autoRepeat =
        register(new BooleanSetting("AutoRepeat", true));
    protected final Setting<Integer> delay =
        register(new NumberSetting<>("Delay", 5, 1, 40));
    protected final Setting<Boolean> confirm =
        register(new BooleanSetting("Confirm", false));
    protected final Setting<Boolean> instructions =
        register(new BooleanSetting("Instructions", true));

    Phase phase = Phase.DROP;
    final StopWatch timer = new StopWatch();
    int idBefore;
    int countBefore;
    int slotBefore = -1;
    float oldPitch;
    boolean pitched;
    IRecipe recipe;
    final java.util.Deque<Integer> queue = new java.util.ArrayDeque<>();

    public AutoItemDupe()
    {
        super("AutoItemDupe", Category.Misc);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "5b5t recipe dupe. Planks in inventory, hold item."));
    }

    @Override
    protected void onEnable()
    {
        phase = Phase.DROP;
        queue.clear();
        pitched = false;
        recipe = net.minecraft.item.crafting.CraftingManager.getRecipe(
            new ResourceLocation("wooden_button"));
        if (recipe == null)
        {
            disable();
            return;
        }

        if (mc.player != null && instructions.getValue())
        {
            instructions.setValue(false);
            me.earth.earthhack.impl.util.text.ChatUtil.sendMessage(
                "Hold the item to dupe, keep planks, wait for pickup. "
                + "If it fails, open the recipe book manually.");
        }

        if (!confirm.getValue())
        {
            me.earth.earthhack.impl.util.text.ChatUtil.sendMessage(
                net.minecraft.util.text.TextFormatting.RED
                + "[AutoItemDupe] Enable Confirm, this is 5b5t only.");
            disable();
        }
    }

    @Override
    protected void onDisable()
    {
        if (pitched && mc.player != null)
        {
            mc.player.rotationPitch = oldPitch;
            pitched = false;
        }

        phase = Phase.DROP;
        queue.clear();
    }

    public enum Mode
    {
        HELD,
        ALL
    }

    public enum Phase
    {
        DROP,
        PICKUP
    }
}
