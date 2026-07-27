package me.friendly.exeter.module.impl.active.render;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Collection;

import me.friendly.api.event.Listener;
import me.friendly.api.interfaces.Toggleable;
import me.friendly.api.minecraft.helper.PlayerHelper;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.RenderGameOverlayEvent;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.client.resources.language.I18n;

public final class Hud
extends Module {
    public static final Property<Boolean> customFont = new Property<Boolean>(false, "CustomFont", "cf", "font");
    private final Property<Boolean> watermark = new Property<Boolean>(false, "Watermark", "wm", "water");
    private final Property<Boolean> transparent = new Property<Boolean>(true, "Transparent", "trans");
    private final Property<Boolean> direction = new Property<Boolean>(true, "Direction", "facing", "d");
    private final Property<Boolean> armor = new Property<Boolean>(true, "Armor", "a");
    private final Property<Boolean> potions = new Property<Boolean>(true, "Potions", "pots");
    private final Property<Boolean> time = new Property<Boolean>(true, "Time", "t");
    private final Property<Boolean> coords = new Property<Boolean>(true, "Coords", "coord", "c", "cord");
    private final Property<Boolean> arraylist = new Property<Boolean>(true, "ArrayList", "array", "al");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm a");
    private final EnumProperty<Organize> organize = new EnumProperty<Organize>(Organize.LENGTH, "Organize", "o");
    private final EnumProperty<Look> look = new EnumProperty<Look>(Look.DEFAULT, "Casing", "c");

    public Hud() {
        super("Hud", new String[]{"textgui", "hud", "overlay"});
        this.offerProperties(this.customFont, this.look, this.watermark, this.organize, this.transparent, this.potions, this.armor, this.time, this.direction, this.arraylist, this.coords);
        Exeter.getInstance().getEventManager().register(new Listener<RenderGameOverlayEvent>("text_gui_render_game_overlay_listener"){

            @Override
            public void call(RenderGameOverlayEvent event) {
                Collection<MobEffectInstance> effects;
                if (minecraft.gui.getDebugOverlay().showDebugScreen() || event.getType() != RenderGameOverlayEvent.Type.IN_GAME) {
                    return;
                }
                if (watermark.getValue()) {
                    FontUtil.drawString(String.format("%s \u00a77%s %s", Exeter.TITLE, Exeter.BUILD, Exeter.HASH), 2.0f, 2.0f, (Boolean) transparent.getValue() != false ? -1711276033 : -1);
                    
                }
                int scaledWidth = minecraft.getWindow().getGuiScaledWidth();
                int scaledHeight = minecraft.getWindow().getGuiScaledHeight();
                int positionY = -7;
                if (arraylist.getValue()) {
                    List<Module> modules = Exeter.getInstance().getModuleManager().getRegistry();
                    switch ((Organize)((Object) organize.getValue())) {
                        case ABC: {
                            modules.sort((mod1, mod2) -> mod1.getTag().compareTo(mod2.getTag()));
                            break;
                        }
                        case LENGTH: {
                            modules.sort((mod1, mod2) -> FontUtil.getStringWidth(mod2.getTag()) - FontUtil.getStringWidth(mod1.getTag()));
                        }
                    }
                    for (Module module : modules) {
                        ToggleableModule toggleableModule;
                        if (!(module instanceof Toggleable) || !(toggleableModule = (ToggleableModule)module).isDrawn() || toggleableModule.getColor() == 0 || !toggleableModule.isRunning()) continue;
                        int labelWidth = FontUtil.getStringWidth(getTag(toggleableModule.getTag()));
                        FontUtil.drawString(getTag(toggleableModule.getTag()), scaledWidth - labelWidth - 2, positionY += 9, toggleableModule.getColor());
                    }
                }
                if (armor.getValue()) {
                    int x = 15;
                    net.minecraft.client.gui.render.state.GuiRenderState guiRenderState = new net.minecraft.client.gui.render.state.GuiRenderState();
                    net.minecraft.client.gui.GuiGraphics localGui = new net.minecraft.client.gui.GuiGraphics(minecraft, guiRenderState, scaledWidth, scaledHeight);
                    for (int index = 3; index >= 0; --index) {
                        net.minecraft.world.entity.EquipmentSlot slot = index == 3 ? net.minecraft.world.entity.EquipmentSlot.HEAD : (index == 2 ? net.minecraft.world.entity.EquipmentSlot.CHEST : (index == 1 ? net.minecraft.world.entity.EquipmentSlot.LEGS : net.minecraft.world.entity.EquipmentSlot.FEET));
                        ItemStack stack = minecraft.player.getItemBySlot(slot);
                        if (stack == null || stack.isEmpty()) continue;
                        int y = (minecraft.player.isUnderWater() && !minecraft.player.getAbilities().instabuild) ? 65 : (minecraft.player.getAbilities().instabuild ? 38 : 55);
                        localGui.renderItem(stack, scaledWidth / 2 + x, scaledHeight - y);
                        localGui.renderItemDecorations(minecraft.font, stack, scaledWidth / 2 + x, scaledHeight - y);
                        x += 18;
                    }
                }

                int y = scaledHeight - (minecraft.screen instanceof net.minecraft.client.gui.screens.ChatScreen ? 24 : 10);

                if (potions.getValue()
                        && (effects = minecraft.player.getActiveEffects()) != null
                        && !effects.isEmpty())
                {

                    for (MobEffectInstance effect : effects) {
                        if (effect == null) return;

                        MobEffect mobEffect = effect.getEffect().value();
                        if (mobEffect == null) {
                            continue;
                        }

                        String name = I18n.get(mobEffect.getDescriptionId());
                        name = name + String.format(" \u00a77%s : %s", effect.getAmplifier() + 1, MobEffectUtil.formatDuration(effect, 1.0f, 1.0f).getString());
                        int align = scaledWidth - FontUtil.getStringWidth(name) - 2;
                        FontUtil.drawString(name, align, y, mobEffect.getColor());

                        y -= 9;
                    }
                }

                y += 9;

                if (coords.getValue()) {
                    String coordinatesFormat = String.format("\u00a7f%s, %s, %s \u00a77XYZ", (int)minecraft.player.getX(), (int)minecraft.player.getY(), (int)minecraft.player.getZ());
                    FontUtil.drawString(coordinatesFormat, scaledWidth - FontUtil.getStringWidth(coordinatesFormat) - 2, y -= 9, -1);
                }
                if (time.getValue()) {
                    String time = String.format("\u00a77%s", dateFormat.format(new Date()));
                    FontUtil.drawString(time, scaledWidth - FontUtil.getStringWidth(time) - 2, y -= 9, -1);
                }
                if (direction.getValue()) {
                    String direction = String.format("\u00a77%s", PlayerHelper.getFacingWithProperCapitals().toUpperCase());
                    FontUtil.drawString(direction, scaledWidth - FontUtil.getStringWidth(direction) - 2, y -= 9, -1);
                }
            }
        });
    }

    private String getTag(String tag) {
        switch (look.getValue()) {
            case UPPER: {
                tag = tag.toUpperCase();
                break;
            }
            case LOWER: {
                tag = tag.toLowerCase();
                break;
            }
            case CUB: {
                tag = String.format("[%s]", tag.toLowerCase());
            }
        }

        return tag;
    }

    private enum Look {
        DEFAULT,
        LOWER,
        UPPER,
        CUB;

    }

    private enum Organize {
        ABC,
        LENGTH;

    }
}

