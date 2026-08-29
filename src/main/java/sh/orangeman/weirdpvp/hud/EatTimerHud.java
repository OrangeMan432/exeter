package sh.orangeman.weirdpvp.hud;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;

public class EatTimerHud extends HudElement {
    public static final HudGroup GROUP = new HudGroup("WeirdPvP");

    public static final HudElementInfo<EatTimerHud> INFO = new HudElementInfo<EatTimerHud>(
        GROUP,
        "eat-timer",
        "Eat Timer",
        "Shows eating progress as a percentage.",
        EatTimerHud::new
    );

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Whether to render text with a shadow.")
        .defaultValue(true)
        .build()
    );

    private boolean wasUsingItem = false;
    private int totalTicks = 0;

    public EatTimerHud() {
        super(INFO);
    }

    @Override
    public void tick(HudRenderer renderer) {
        Player player = MeteorClient.mc.player;

        if (player != null && player.isUsingItem()) {
            ItemStack useItem = player.getUseItem();
            Consumable consumable = useItem.get(DataComponents.CONSUMABLE);

            if (!wasUsingItem) {
                // Eating just started — grab the total ticks from the item
                totalTicks = consumable != null ? consumable.consumeTicks() : 32;
            } else if (totalTicks == 0) {
                // Fallback if somehow zero
                totalTicks = consumable != null ? consumable.consumeTicks() : 32;
            }

            wasUsingItem = true;
        } else {
            // Eating ended (completed, canceled, or restarted)
            wasUsingItem = false;
            totalTicks = 0;
        }

        String text = "";
        if (wasUsingItem && totalTicks > 0 && player != null) {
            int elapsed = player.getTicksUsingItem();
            double percent = (double) elapsed / totalTicks * 100.0;
            text = String.format("%.1f%%", percent);
        }

        String displayText = isInEditor() ? "100.0%" : text;
        setSize(renderer.textWidth(displayText), renderer.textHeight());
    }

    @Override
    public void render(HudRenderer renderer) {
        Player player = MeteorClient.mc.player;

        String text = "";
        if (wasUsingItem && totalTicks > 0 && player != null) {
            int elapsed = player.getTicksUsingItem();
            double percent = (double) elapsed / totalTicks * 100.0;
            text = String.format("%.1f%%", percent);
        }

        String displayText = isInEditor() ? "100.0%" : text;
        renderer.text(displayText, x, y, Color.WHITE, shadow.get());
    }
}
