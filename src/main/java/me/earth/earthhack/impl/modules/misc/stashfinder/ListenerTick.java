package me.earth.earthhack.impl.modules.misc.stashfinder;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.text.ChatUtil;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntityShulkerBox;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextFormatting;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

final class ListenerTick extends ModuleListener<StashFinder, TickEvent>
{
    public ListenerTick(StashFinder module)
    {
        super(module, TickEvent.class);
    }

    @Override
    public void invoke(TickEvent event)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        if (module.ticks++ < 100)
        {
            return;
        }

        module.ticks = 0;
        Map<ChunkPos, Integer> counts = new HashMap<>();
        for (Object object : mc.world.loadedTileEntityList)
        {
            if (object instanceof TileEntityChest
                || object instanceof TileEntityEnderChest
                || object instanceof TileEntityShulkerBox)
            {
                net.minecraft.tileentity.TileEntity tile =
                    (net.minecraft.tileentity.TileEntity) object;
                ChunkPos chunk = new ChunkPos(tile.getPos());
                counts.put(chunk, counts.getOrDefault(chunk, 0) + 1);
            }
        }

        for (Map.Entry<ChunkPos, Integer> entry : counts.entrySet())
        {
            if (entry.getValue() >= module.minStorage.getValue()
                && !module.logged.contains(entry.getKey()))
            {
                module.logged.add(entry.getKey());
                String coords = "(" + (entry.getKey().x * 16 + 8)
                    + ", " + (entry.getKey().z * 16 + 8) + ")";
                if (module.notify.getValue())
                {
                    ChatUtil.sendMessage(TextFormatting.GOLD
                        + "[StashFinder] " + TextFormatting.RESET
                        + entry.getValue() + " storage blocks at "
                        + TextFormatting.GOLD + coords);
                }

                saveFile(coords, entry.getValue());
            }
        }
    }

    private void saveFile(String coords, int count)
    {
        if (!module.saveToFile.getValue())
        {
            return;
        }

        try
        {
            File file = new File("./earthhack/stashes.txt");
            //noinspection ResultOfMethodCallIgnored
            file.getParentFile().mkdirs();

            PrintWriter writer = new PrintWriter(new FileWriter(file, true));
            String ip = !mc.isSingleplayer()
                ? mc.getCurrentServerData().serverIP
                : "singleplayer";
            writer.println(count + " storage at " + coords
                + " Server: " + ip);
            writer.close();
        }
        catch (Exception ignored)
        {
            // Silently ignore IO failures.
        }
    }
}
