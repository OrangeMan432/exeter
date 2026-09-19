package me.earth.earthhack.impl.modules.misc.chestswap;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * One-key chestplate and elytra swap.
 * Ported from SalHack (ChestSwap).
 */
public class ChestSwap extends Module
{
    protected final Setting<Boolean> preferElytra =
        register(new BooleanSetting("PreferElytra", true));
    protected final Setting<Boolean> noCurse =
        register(new BooleanSetting("NoCurse", false));

    public ChestSwap()
    {
        super("ChestSwap", Category.Misc);
        this.setData(new SimpleData(this,
            "Swaps chestplate and elytra instantly."));
    }

    @Override
    protected void onEnable()
    {
        if (mc.player == null || mc.world == null)
        {
            disable();
            return;
        }

        net.minecraft.item.ItemStack chest =
            mc.player.inventoryContainer.getSlot(6).getStack();
        if (chest.isEmpty())
        {
            int slot = findChestItem(preferElytra.getValue());
            if (!preferElytra.getValue() && slot == -1)
            {
                slot = findChestItem(true);
            }

            if (slot != -1)
            {
                swap(slot);
            }
        }
        else
        {
            int slot = findChestItem(
                chest.getItem() instanceof net.minecraft.item.ItemArmor);
            if (slot != -1)
            {
                swap(slot);
            }
        }

        disable();
    }

    private void swap(int slot)
    {
        mc.playerController.windowClick(
            mc.player.inventoryContainer.windowId, slot, 0,
            net.minecraft.inventory.ClickType.PICKUP, mc.player);
        mc.playerController.windowClick(
            mc.player.inventoryContainer.windowId, 6, 0,
            net.minecraft.inventory.ClickType.PICKUP, mc.player);
        mc.playerController.windowClick(
            mc.player.inventoryContainer.windowId, slot, 0,
            net.minecraft.inventory.ClickType.PICKUP, mc.player);
    }

    private int findChestItem(boolean elytra)
    {
        for (int i = 9; i < 45; i++)
        {
            net.minecraft.item.ItemStack stack = mc.player
                .inventoryContainer.getSlot(i).getStack();
            if (stack.isEmpty())
            {
                continue;
            }

            if (noCurse.getValue()
                && net.minecraft.enchantment.EnchantmentHelper.hasBindingCurse(
                    stack))
            {
                continue;
            }

            boolean isElytra =
                stack.getItem() instanceof net.minecraft.item.ItemElytra;
            if (elytra == isElytra
                && (isElytra
                    || stack.getItem()
                        instanceof net.minecraft.item.ItemArmor
                    && ((net.minecraft.item.ItemArmor) stack.getItem())
                        .armorType
                        == net.minecraft.inventory.EntityEquipmentSlot.CHEST))
            {
                return i;
            }
        }

        return -1;
    }
}
