package me.earth.earthhack.impl.modules.misc.autoshulkerdupe;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;
import me.earth.earthhack.impl.util.math.StopWatch;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.math.BlockPos;

/**
 * 5b5t stacked-shulker dupe machine.
 * Ported from Techale via ToxicAven 5b-AutoDupes (AutoShulkerDupe).
 * 5b5t only, needs confirm enabled.
 */
public class AutoShulkerDupe extends Module
{
    protected final Setting<Boolean> hopperCheck =
        register(new BooleanSetting("HopperCheck", false));
    protected final Setting<Integer> itemCrafting =
        register(new NumberSetting<>("ItemCrafting", 60, 0, 100));
    protected final Setting<Integer> maxStackWait =
        register(new NumberSetting<>("MaxStackWait", 60, 0, 100));
    protected final Setting<Integer> waitPlace =
        register(new NumberSetting<>("WaitPlace", 60, 0, 100));
    protected final Setting<Boolean> instructions =
        register(new BooleanSetting("Instructions", false));
    protected final Setting<Boolean> confirm =
        register(new BooleanSetting("Confirm", false));

    BlockPos shulkerPos;
    BlockPos workbenchPos;
    int slotPick;
    int slotShulk;
    int slotWood;
    int stage;
    int tickPutItem;
    boolean beforePlaced;
    final StopWatch timer = new StopWatch();

    public AutoShulkerDupe()
    {
        super("AutoShulkerDupe", Category.Misc);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "5b5t stacked shulker dupe. Confirm first."));
    }

    @Override
    protected void onEnable()
    {
        if (mc.player == null || mc.world == null)
        {
            disable();
            return;
        }

        if (!confirm.getValue())
        {
            me.earth.earthhack.impl.util.text.ChatUtil.sendMessage(
                "5b5t stacked-shulker dupe. Stand on a crafting table "
                + "flush with the ground, look down, shulker in hand, "
                + "pickaxe in hotbar, planks in inventory. "
                + "Enable Confirm to run.");
            disable();
            return;
        }

        if (instructions.getValue())
        {
            instructions.setValue(false);
            me.earth.earthhack.impl.util.text.ChatUtil.sendMessage(
                "Stand on a crafting table flush with the ground.");
        }

        workbenchPos = new BlockPos(
            mc.player.posX + 0.5, mc.player.posY - 1.0, mc.player.posZ + 0.5);
        shulkerPos = null;
        double[][] offsets =
            {{1.0, -2.0, 0.0}, {-1.0, -2.0, 0.0},
             {0.0, -2.0, 1.0}, {0.0, -2.0, -1.0}};
        for (double[] o : offsets)
        {
            BlockPos pos = new BlockPos(mc.player.posX + o[0],
                                        mc.player.posY,
                                        mc.player.posZ + o[2]);
            if (hopperCheck.getValue())
            {
                BlockPos hopper = new BlockPos(mc.player.posX + o[0],
                                               mc.player.posY + o[1],
                                               mc.player.posZ + o[2]);
                if (mc.world.getBlockState(hopper).getBlock()
                        instanceof net.minecraft.block.BlockHopper)
                {
                    shulkerPos = pos;
                    break;
                }
            }
            else if (mc.world.getBlockState(pos).getBlock()
                    instanceof BlockAir)
            {
                shulkerPos = pos;
                break;
            }
        }

        if (shulkerPos == null)
        {
            disable();
            return;
        }

        slotPick = findItem(net.minecraft.init.Items.DIAMOND_PICKAXE);
        slotWood = findBlock(Blocks.PLANKS);
        slotShulk = findShulker();
        if (slotPick == -1 || slotWood == -1 || slotShulk == -1)
        {
            disable();
            return;
        }

        stage = 0;
    }

    private int findShulker()
    {
        for (int i = 0; i < 9; i++)
        {
            net.minecraft.item.ItemStack stack =
                mc.player.inventory.mainInventory.get(i);
            if (!stack.isEmpty()
                && stack.getItem() instanceof ItemBlock
                && ((ItemBlock) stack.getItem()).getBlock()
                    instanceof BlockShulkerBox)
            {
                return i;
            }
        }

        return -1;
    }

    private int findItem(net.minecraft.item.Item item)
    {
        for (int i = 0; i < 9; i++)
        {
            if (item == mc.player.inventory.mainInventory.get(i).getItem())
            {
                return i;
            }
        }

        return -1;
    }

    private int findBlock(Block block)
    {
        for (int i = 0; i < 36; i++)
        {
            net.minecraft.item.ItemStack stack =
                mc.player.inventory.mainInventory.get(i);
            if (!stack.isEmpty()
                && stack.getItem() instanceof ItemBlock
                && block == ((ItemBlock) stack.getItem()).getBlock())
            {
                return i;
            }
        }

        return -1;
    }

    @Override
    protected void onDisable()
    {
        stage = 0;
        tickPutItem = 0;
        beforePlaced = false;
        shulkerPos = null;
    }

    String stageName()
    {
        switch (stage)
        {
            case 0:
                return "Dropping shulker";
            case 1:
                return "Crafting button";
            case 2:
                return "Waiting on the shulker";
            case 3:
                return "Breaking the shulker";
            default:
                return "";
        }
    }
}
