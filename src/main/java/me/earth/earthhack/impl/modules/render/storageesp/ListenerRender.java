package me.earth.earthhack.impl.modules.render.storageesp;

import me.earth.earthhack.impl.event.events.render.Render3DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.render.RenderUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityMinecartChest;
import net.minecraft.item.ItemShulkerBox;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.tileentity.TileEntityShulkerBox;
import net.minecraft.util.math.BlockPos;

import java.awt.*;

final class ListenerRender extends ModuleListener<StorageESP, Render3DEvent>
{
    public ListenerRender(StorageESP module)
    {
        super(module, Render3DEvent.class);
    }

    @Override
    public void invoke(Render3DEvent event)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        double rangeSq = module.range.getValue() * module.range.getValue();
        for (Object object : mc.world.loadedTileEntityList)
        {
            BlockPos pos = ((net.minecraft.tileentity.TileEntity) object)
                .getPos();
            if (mc.player.getDistanceSq(pos) > rangeSq)
            {
                continue;
            }

            Color color = colorFor(object);
            if (color != null)
            {
                RenderUtil.renderBox(pos,
                                     color,
                                     module.height.getValue());
            }
        }

        if (module.minecarts.getValue() || module.frames.getValue())
        {
            for (Entity entity : mc.world.loadedEntityList)
            {
                if (mc.player.getDistanceSq(entity) > rangeSq)
                {
                    continue;
                }

                if (module.minecarts.getValue()
                    && entity instanceof EntityMinecartChest)
                {
                    RenderUtil.renderBox(entity.getPosition(),
                                         module.minecartColor.getValue(),
                                         module.height.getValue());
                }
                else if (module.frames.getValue()
                    && entity instanceof EntityItemFrame
                    && ((EntityItemFrame) entity).getDisplayedItem()
                        .getItem() instanceof ItemShulkerBox)
                {
                    RenderUtil.renderBox(
                        entity.getPosition().add(0, -1, 0),
                        module.frameColor.getValue(),
                        module.height.getValue());
                }
            }
        }
    }

    private Color colorFor(Object tile)
    {
        if (tile instanceof TileEntityChest)
        {
            return module.chestColor.getValue();
        }
        else if (tile instanceof TileEntityEnderChest)
        {
            return module.enderChestColor.getValue();
        }
        else if (tile instanceof TileEntityShulkerBox)
        {
            return module.shulkerColor.getValue();
        }
        else if (tile instanceof TileEntityFurnace)
        {
            return module.furnaceColor.getValue();
        }
        else if (tile instanceof TileEntityHopper)
        {
            return module.hopperColor.getValue();
        }
        else if (tile instanceof TileEntityDispenser)
        {
            return module.dispenserColor.getValue();
        }

        return null;
    }
}
