package me.friendly.exeter.module.impl.toggle.misc;

import java.util.HashMap;
import java.util.Map;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.equipment.Equippable;

/**
 * AutoArmor. Equips the best available armor piece per slot (head/chest/legs/feet) from the
 * inventory, replacing strictly worse equipped pieces.
 *
 * How it scores on 26.2: armor items are plain items carrying {@link DataComponents#EQUIPPABLE}
 * (which declares the {@link EquipmentSlot}) plus {@link DataComponents#ATTRIBUTE_MODIFIERS};
 * the piece's defense is the {@link Attributes#ARMOR} modifier amount (read via
 * {@code ItemAttributeModifiers.forEach(EquipmentSlotGroup, BiConsumer)}). Toughness is the
 * tiebreak, weighted by the Toughness Weight property. Damageable pieces below the durability
 * floor are skipped (don't auto-equip nearly-broken gear).
 *
 * Customization:
 *  - Min Durability % : skip pieces below this fraction of max durability.
 *  - Replace Equipped : off = only fill empty slots; on = swap out strictly worse pieces.
 *  - Toughness Weight : how much armor toughness counts vs plain armor in the score.
 *  - Keep Curio/Curse : skip pieces with custom names (preserve named/curious gear).
 *
 * All movement goes through the stack-size-aware click path ({@code PICKUP} twice), so
 * overstacked (127) item servers behave correctly. One swap per tick, skipped while any
 * screen is open or the cursor holds an item (never disrupts manual inventory work).
 */
public class AutoArmor extends ToggleableModule {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final NumberProperty<Integer> minDurabilityPct = new NumberProperty<>(10, 0, 100, "Min Durability %", "mindurability", "dur");
    private final Property<Boolean> replaceEquipped = new Property<>(true, "Replace Equipped", "replace", "upgrade");
    private final NumberProperty<Float> toughnessWeight = new NumberProperty<>(0.5f, 0.0f, 3.0f, "Toughness Weight", "toughness", "tw");
    private final Property<Boolean> keepNamed = new Property<>(true, "Keep Named", "keepnamed", "named");

    private int cooldown = 0;

    public AutoArmor() {
        super("AutoArmor", new String[]{"autoarmor", "armor"}, ModuleType.MISCELLANEOUS);
        offerProperties(minDurabilityPct, replaceEquipped, toughnessWeight, keepNamed);

        this.listeners.add(new Listener<TickEvent>("auto_armor_tick") {
            @Override
            public void call(TickEvent event) {
                if (minecraft.player == null) return;
                if (minecraft.gui.screen() != null) return;

                if (cooldown > 0) {
                    cooldown--;
                    return;
                }

                Player player = minecraft.player;
                AbstractContainerMenu menu = player.containerMenu;
                Inventory inv = player.getInventory();
                if (!menu.getCarried().isEmpty()) return; // never disrupt manual work

                // Index inventory slots by container slot for quick lookup.
                Map<Integer, Slot> invSlots = new HashMap<>();
                for (Slot s : menu.slots) {
                    if (s.container == inv) invSlots.put(s.getContainerSlot(), s);
                }

                for (EquipmentSlot equipSlot : ARMOR_SLOTS) {
                    ItemStack equipped = player.getItemBySlot(equipSlot);

                    // Best candidate in the main inventory + hotbar for this slot.
                    Slot bestSlot = null;
                    double bestScore = score(equipped, toughnessWeight.getValue(), keepNamed.getValue());

                    for (Map.Entry<Integer, Slot> e : invSlots.entrySet()) {
                        int cs = e.getKey();
                        if (cs > 35) continue; // main inv 9-35 + hotbar 0-8
                        ItemStack candidate = e.getValue().getItem();
                        if (candidate.isEmpty()) continue;

                        var equippable = candidate.get(DataComponents.EQUIPPABLE);
                        if (equippable == null || equippable.slot() != equipSlot) continue;
                        if (candidate.getMaxDamage() > 0) {
                            int max = candidate.getMaxDamage();
                            if ((max - candidate.getDamageValue()) * 100 / max < minDurabilityPct.getValue()) {
                                continue; // too damaged
                            }
                        }

                        double score = score(candidate, toughnessWeight.getValue(), keepNamed.getValue());
                        if (score > bestScore) {
                            bestScore = score;
                            bestSlot = e.getValue();
                        }
                    }

                    if (bestSlot == null) continue;

                    boolean replacing = !equipped.isEmpty();
                    if (replacing && !replaceEquipped.getValue()) continue;

                    // Equip: pick up candidate, click the armor slot, return the displaced
                    // piece to the source slot (armor slots only accept their own type, so
                    // the swap is clean; if the displaced piece can't go back, drop the
                    // cursor by clicking the source slot again).
                    menu.clicked(bestSlot.index, 0, ContainerInput.PICKUP, player);
                    Slot armorSlot = findArmorMenuSlot(menu, equipSlot);
                    if (armorSlot == null) {
                        menu.clicked(bestSlot.index, 0, ContainerInput.PICKUP, player); // undo pickup
                        continue;
                    }
                    menu.clicked(armorSlot.index, 0, ContainerInput.PICKUP, player);
                    if (!menu.getCarried().isEmpty()) {
                        menu.clicked(bestSlot.index, 0, ContainerInput.PICKUP, player);
                    }
                    cooldown = 1;
                    return; // one swap per tick
                }
            }
        });
    }

    /**
     * Scores a piece: armor attribute amount for the slot, plus toughness × weight.
     * Named pieces (potential curios) score -1 so they're only "best" when nothing else fits
     * an empty slot — and {@code keepNamed} skips them entirely.
     */
    private static double score(ItemStack stack, float toughnessWeight, boolean keepNamed) {
        if (stack.isEmpty()) return -1;
        if (keepNamed && stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) return -1;

        ItemAttributeModifiers mods = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (mods == null) return -1;

        double[] armor = new double[1];
        double[] toughness = new double[1];
        mods.forEach(EquipmentSlotGroup.ARMOR, (attribute, modifier) -> {
            if (attribute == Attributes.ARMOR && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                armor[0] += modifier.amount();
            } else if (attribute == Attributes.ARMOR_TOUGHNESS
                    && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                toughness[0] += modifier.amount();
            }
        });
        return armor[0] + toughness[0] * toughnessWeight;
    }

    /**
     * Finds the player-armor menu slot for an EquipmentSlot. ArmorSlot stores its
     * EquipmentSlot in a private field with no public accessor on 26.2, so the slot type is
     * matched by construction order: the player menu builds armor slots HEAD→CHEST→LEGS→FEET
     * (inventory slots 0-3). Matching on the ArmorSlot type + equipment inventory index keeps
     * this independent of reflection while staying robust across menu layouts.
     */
    private static Slot findArmorMenuSlot(AbstractContainerMenu menu, EquipmentSlot equipSlot) {
        int equipIndex = switch (equipSlot) {
            case HEAD -> 0;
            case CHEST -> 1;
            case LEGS -> 2;
            case FEET -> 3;
            default -> -1;
        };
        if (equipIndex < 0) return null;
        for (Slot s : menu.slots) {
            if (s instanceof ArmorSlot && s.getContainerSlot() == equipIndex) {
                return s;
            }
        }
        return null;
    }
}
