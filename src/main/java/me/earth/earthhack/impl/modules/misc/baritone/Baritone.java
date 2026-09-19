package me.earth.earthhack.impl.modules.misc.baritone;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.impl.util.client.SimpleData;
import me.earth.earthhack.impl.util.helpers.command.CustomCommandModule;
import me.earth.earthhack.impl.util.text.ChatUtil;
import net.minecraft.util.text.TextFormatting;

/**
 * Drives an installed Baritone Forge 1.12.2 mod.
 * Needs the Baritone mod jar alongside Exeter; this module only
 * sends it commands. Use: +baritone goto &lt;x&gt; &lt;z&gt; [y],
 * +baritone stop, +baritone pause, +baritone resume,
 * +baritone follow &lt;player&gt;, +baritone mine &lt;block&gt;.
 */
public class Baritone extends Module implements CustomCommandModule
{
    private static final String[] ARGS =
        new String[]{"GOTO", "STOP", "PAUSE", "RESUME", "FOLLOW", "MINE"};

    protected final Setting<Boolean> pauseOnDisable =
        register(new BooleanSetting("PauseOnDisable", true));

    public Baritone()
    {
        super("Baritone", Category.Misc);
        this.setData(new SimpleData(this,
            "Sends pathing commands to Baritone."));
    }

    @Override
    protected void onDisable()
    {
        if (pauseOnDisable.getValue())
        {
            run("pause");
        }
    }

    @Override
    public boolean execute(String[] args)
    {
        if (args.length < 2)
        {
            return false;
        }

        String command = args[1].toLowerCase();
        switch (command)
        {
            case "goto":
                if (args.length < 4)
                {
                    return false;
                }

                if (args.length >= 5)
                {
                    run("goto " + args[2] + " " + args[3] + " " + args[4]);
                }
                else
                {
                    run("goto " + args[2] + " " + args[3]);
                }

                return true;
            case "stop":
                run("stop");
                return true;
            case "pause":
                run("pause");
                return true;
            case "resume":
                run("resume");
                return true;
            case "follow":
                if (args.length < 3)
                {
                    return false;
                }

                run("follow " + args[2]);
                return true;
            case "mine":
                if (args.length < 3)
                {
                    return false;
                }

                run("mine " + args[2]);
                return true;
            default:
                return false;
        }
    }

    @Override
    public String[] getArgs()
    {
        return ARGS;
    }

    private void run(String command)
    {
        try
        {
            boolean ok = baritone.api.BaritoneAPI
                .getProvider()
                .getPrimaryBaritone()
                .getCommandManager()
                .execute(command);
            if (!ok)
            {
                ChatUtil.sendMessage(TextFormatting.RED
                    + "[Baritone] Rejected: " + command);
            }
        }
        catch (Throwable t)
        {
            ChatUtil.sendMessage(TextFormatting.RED
                + "[Baritone] Not installed? Drop the Baritone "
                + "Forge 1.12.2 jar next to Exeter.");
        }
    }
}
