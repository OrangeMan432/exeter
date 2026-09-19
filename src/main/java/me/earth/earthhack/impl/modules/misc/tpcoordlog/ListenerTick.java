package me.earth.earthhack.impl.modules.misc.tpcoordlog;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.text.ChatUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

final class ListenerTick extends ModuleListener<TPCoordLog, TickEvent>
{
    public ListenerTick(TPCoordLog module)
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

        if (module.ticks >= 50)
        {
            module.ticks = 0;

            for (Entity entity : mc.world.loadedEntityList)
            {
                if (entity instanceof EntityPlayer
                    && !entity.getName().equals(mc.player.getName()))
                {
                    Vec3d playerPos =
                        new Vec3d(entity.posX, entity.posY, entity.posZ);

                    if (module.knownPlayers.containsKey(entity))
                    {
                        if (Math.abs(module.knownPlayers.get(entity)
                                                        .distanceTo(playerPos)) > 50.0
                            && Math.abs(mc.player.getPositionVector()
                                                 .distanceTo(playerPos)) > 100.0
                            && (!module.teleportedPlayers.containsKey(entity.getName())
                                || module.teleportedPlayers.get(entity.getName()) != playerPos))
                        {
                            ChatUtil.sendMessage(
                                TextFormatting.WHITE + entity.getName()
                                    + TextFormatting.GRAY + " has TP'd to "
                                    + TextFormatting.WHITE + toString(playerPos));
                            saveFile(toString(playerPos), entity.getName());

                            module.knownPlayers.remove(entity);
                            module.teleportedPlayers.put(entity.getName(), playerPos);
                        }
                    }

                    module.knownPlayers.put(entity, playerPos);
                }
            }
        }

        if (module.forgetTicks >= 9000000)
        {
            module.teleportedPlayers.clear();
        }

        module.ticks++;
        module.forgetTicks++;
    }

    private String toString(Vec3d vector)
    {
        return "("
            + (int) Math.floor(vector.x)
            + ", "
            + (int) Math.floor(vector.z)
            + ")";
    }

    private void saveFile(String pos, String name)
    {
        if (!module.saveToFile.getValue())
        {
            return;
        }

        try
        {
            File file = new File("./earthhack/tpcoords.txt");
            //noinspection ResultOfMethodCallIgnored
            file.getParentFile().mkdirs();

            PrintWriter writer = new PrintWriter(new FileWriter(file, true));
            String ip = !mc.isSingleplayer()
                ? mc.getCurrentServerData().serverIP
                : "singleplayer";
            writer.println(
                "(Teleport) IGN: " + name + " Pos: " + pos + " Server: " + ip);
            writer.close();
        }
        catch (Exception ignored)
        {
            // Mio behavior: silently ignore IO failures.
        }
    }
}
