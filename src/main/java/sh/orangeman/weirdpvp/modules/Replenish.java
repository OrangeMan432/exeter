package sh.orangeman.weirdpvp.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.item.ItemStack;
import sh.orangeman.weirdpvp.WeirdPvP;

public class Replenish extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> threshold = sgGeneral.add(new IntSetting.Builder()
        .name("threshold")
        .description("Refills a hotbar slot when its stack size drops to or below this.")
        .defaultValue(0)
        .range(0, 64)
        .build()
    );

    private final Setting<Integer> tickDelay = sgGeneral.add(new IntSetting.Builder()
        .name("tick-delay")
        .description("Ticks between replenish actions.")
        .defaultValue(0)
        .range(0, 20)
        .build()
    );

    private int timer;

    public Replenish() {
        super(WeirdPvP.CATEGORY, "replenish", "Refills your hotbar from your inventory.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;
        if (timer++ < tickDelay.get()) return;
        timer = 0;

        for (int i = 0; i < 9; i++) {
            ItemStack hotbar = mc.player.getInventory().getItem(i);
            if (hotbar.isEmpty() || hotbar.getCount() > threshold.get()) continue;

            FindItemResult found = InvUtils.find(itemStack -> !itemStack.isEmpty() && itemStack.getItem() == hotbar.getItem(), 9, 35);
            if (found.found()) InvUtils.move().from(found.slot()).to(i);
        }
    }
}
