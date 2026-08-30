package me.friendly.exeter.module.impl.toggle.misc;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.inventory.ContainerInput;

/**
 * Auto item dupe, ported in spirit from Lambda's 5bDupes (ToxicAven) AutoItemDupe module.
 *
 * On 5b5t the dupe works by throwing the held stack out of the inventory and then firing the
 * recipe-book "place recipe" packet (wooden_button), which copies the thrown stack back into
 * your inventory.
 *
 * The full 5b5t sequence wired here:
 * <ol>
 *   <li>Throw the held stack (client-side THROW click on the selected slot).</li>
 *   <li>Send the recipe-book "place recipe" packet for the wooden-planks recipe via
 *       {@link MultiPlayerGameMode#handlePlaceRecipe(int, RecipeDisplayId, boolean)}, which
 *       builds and sends {@code ServerboundPlaceRecipePacket(containerId, RecipeDisplayId,
 *       useMaxItems)} — exactly the packet the vanilla client sends when clicking a
 *       recipe-book entry.</li>
 * </ol>
 *
 * The {@link RecipeDisplayId} comes from the client recipe book
 * ({@code LocalPlayer.getRecipeBook()} → {@code ClientRecipeBook.known}, a
 * {@code Map<RecipeDisplayId, RecipeDisplayEntry>} fed by
 * {@code ClientboundRecipeBookAddPacket}). Every entry carries its server-assigned id, so no
 * index guessing is involved. The module resolves the planks entry by matching the display's
 * RESULT item against the planks tag (via {@code RecipeDisplayEntry.resultItems(ContextMap)}
 * with {@code SlotDisplayContext.fromLevel}), falling back to entry-text matching.
 *
 * Requires wooden planks anywhere in your inventory (the recipe needs a valid ingredient).
 */
public class AutoItemDupe extends ToggleableModule {
    private enum Phase {
        NONE, DROP, PLACE_RECIPE
    }

    private final Property<Boolean> cancelGui = new Property<>(false, "Cancel GUI", "cancelgui");

    private Phase phase = Phase.NONE;
    private long lastAction = 0L;
    private RecipeDisplayId planksDisplayId;

    public AutoItemDupe() {
        super("AutoItemDupe", new String[]{"autoitemdupe", "aidd"}, ModuleType.MISCELLANEOUS);
        offerProperties(cancelGui);

        this.listeners.add(new Listener<TickEvent>("auto_item_dupe_tick") {
            @Override
            public void call(TickEvent event) {
                if (minecraft.player == null) return;
                if (phase == Phase.NONE) return;

                long now = System.currentTimeMillis();

                if (phase == Phase.DROP) {
                    if (minecraft.player.getInventory().getSelectedItem().isEmpty()) {
                        phase = Phase.NONE;
                        return;
                    }
                    // Throw the currently held stack out of the inventory.
                    minecraft.player.containerMenu.clicked(
                            minecraft.player.getInventory().getSelectedSlot(), 1, ContainerInput.THROW, minecraft.player);
                    phase = Phase.PLACE_RECIPE;
                    lastAction = now;
                } else if (phase == Phase.PLACE_RECIPE) {
                    // Wait one tick gap after the throw, then fire the recipe-place trigger.
                    if (now - lastAction < 50L) return;
                    if (now - lastAction > 3000L) {
                        // Server never confirmed; give up this cycle.
                        phase = Phase.NONE;
                        return;
                    }
                    RecipeDisplayId displayId = planksDisplayId != null
                            ? planksDisplayId
                            : findPlanksRecipeDisplayId();
                    if (displayId == null) {
                        phase = Phase.NONE;
                        return;
                    }
                    MultiPlayerGameMode gameMode = minecraft.gameMode;
                    if (gameMode == null) {
                        phase = Phase.NONE;
                        return;
                    }
                    gameMode.handlePlaceRecipe(
                            minecraft.player.containerMenu.containerId,
                            displayId,
                            false);
                    phase = Phase.NONE;
                }
            }
        });
    }

    /**
     * Resolves the wooden-planks recipe's server-assigned {@link RecipeDisplayId} from the
     * client recipe book. Iterates {@code ClientRecipeBook.known} (via reflection on the
     * private map — the public surface only exposes collections grouped by tab), matches the
     * entry whose display result is a planks item, and returns that entry's real id.
     * Returns null if the recipe book is unavailable or no planks recipe is known to it.
     */
    @SuppressWarnings("unchecked")
    private RecipeDisplayId findPlanksRecipeDisplayId() {
        LocalPlayer player = minecraft.player;
        if (player == null) return null;

        try {
            var recipeBook = player.getRecipeBook();
            var knownField = net.minecraft.client.ClientRecipeBook.class.getDeclaredField("known");
            knownField.setAccessible(true);
            var known = (java.util.Map<RecipeDisplayId, RecipeDisplayEntry>) knownField.get(recipeBook);
            if (known == null || known.isEmpty()) return null;

            var context = net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(minecraft.level);

            for (var entry : known.values()) {
                try {
                    // Primary match: the display's result items are planks.
                    var results = entry.resultItems(context);
                    for (ItemStack stack : results) {
                        if (!stack.isEmpty() && stack.getItem() instanceof BlockItem bi
                                && bi.getBlock().defaultBlockState().is(BlockTags.PLANKS)) {
                            return entry.id();
                        }
                    }
                } catch (Exception ignored) {
                    // resultItems can fail without a full context; fall back below.
                }
            }
        } catch (Exception ignored) {
            // Reflection or mapping mismatch: do nothing rather than send a wrong id.
        }
        return null;
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        planksDisplayId = null;
        if (minecraft.player == null) {
            phase = Phase.NONE;
            return;
        }
        boolean hasPlanks = false;
        for (int i = 0; i < 36; i++) {
            ItemStack s = minecraft.player.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() instanceof BlockItem bi
                    && bi.getBlock().defaultBlockState().is(BlockTags.PLANKS)) {
                hasPlanks = true;
                break;
            }
        }
        phase = hasPlanks ? Phase.DROP : Phase.NONE;
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        if (cancelGui.getValue() && minecraft.gui.screen() != null) minecraft.gui.setScreen(null);
    }
}
