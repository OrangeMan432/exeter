package sh.orangeman.weirdpvp.modules;

import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.AABB;
import sh.orangeman.weirdpvp.WeirdPvP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutoPot extends Module {
    public enum Page {
        General,
        BadPot
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Page> page = sgGeneral.add(new EnumSetting.Builder<Page>()
        .name("page")
        .description("Which settings to show.")
        .defaultValue(Page.General)
        .build()
    );

    private final Setting<Boolean> hp = sgGeneral.add(new BoolSetting.Builder()
        .name("health-potion")
        .description("Throws a healing splash potion when your health is low.")
        .defaultValue(false)
        .visible(() -> page.get() == Page.General)
        .build()
    );

    private final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder()
        .name("health")
        .description("The health threshold to throw a healing potion at.")
        .defaultValue(16)
        .range(0, 20)
        .visible(() -> hp.get() && page.get() == Page.General)
        .build()
    );

    private final Setting<Boolean> equal = sgGeneral.add(new BoolSetting.Builder()
        .name("equal")
        .description("Also throws when your health is exactly equal to the threshold.")
        .defaultValue(false)
        .visible(() -> hp.get() && page.get() == Page.General)
        .build()
    );

    private final Setting<Boolean> predict = sgGeneral.add(new BoolSetting.Builder()
        .name("predict")
        .description("Predicts incoming damage and throws the potion earlier.")
        .defaultValue(false)
        .visible(() -> hp.get() && page.get() == Page.General)
        .build()
    );

    private final Setting<Double> times = sgGeneral.add(new DoubleSetting.Builder()
        .name("time-seconds")
        .description("How many seconds of damage to predict.")
        .defaultValue(1.0)
        .range(0, 5)
        .visible(() -> hp.get() && predict.get() && page.get() == Page.General)
        .build()
    );

    private final Setting<Integer> predictHpDelay = sgGeneral.add(new IntSetting.Builder()
        .name("predict-health-delay")
        .description("The delay in milliseconds between predicted health potions.")
        .defaultValue(50)
        .range(0, 1000)
        .visible(() -> hp.get() && predict.get() && page.get() == Page.General)
        .build()
    );

    private final Setting<Integer> healthSlot = sgGeneral.add(new IntSetting.Builder()
        .name("health-slot")
        .description("The hotbar slot to move the health potion to.")
        .defaultValue(1)
        .range(1, 9)
        .visible(() -> hp.get() && page.get() == Page.General)
        .build()
    );

    private final Setting<Integer> hpDelay = sgGeneral.add(new IntSetting.Builder()
        .name("health-delay")
        .description("The delay in milliseconds between health potions.")
        .defaultValue(50)
        .range(0, 1000)
        .visible(() -> hp.get() && page.get() == Page.General)
        .build()
    );

    private final Setting<Boolean> speed = sgGeneral.add(new BoolSetting.Builder()
        .name("swiftness")
        .description("Throws a swiftness splash potion when the effect is about to run out.")
        .defaultValue(false)
        .visible(() -> page.get() == Page.General)
        .build()
    );

    private final Setting<Integer> time = sgGeneral.add(new IntSetting.Builder()
        .name("time-left")
        .description("Throws a new swiftness potion when the effect has less seconds left than this.")
        .defaultValue(5)
        .range(0, 30)
        .visible(() -> speed.get() && page.get() == Page.General)
        .build()
    );

    private final Setting<Integer> swiftnessSlot = sgGeneral.add(new IntSetting.Builder()
        .name("swiftness-slot")
        .description("The hotbar slot to move the swiftness potion to.")
        .defaultValue(1)
        .range(1, 9)
        .visible(() -> speed.get() && page.get() == Page.General)
        .build()
    );

    private final Setting<Integer> speedDelay = sgGeneral.add(new IntSetting.Builder()
        .name("swiftness-delay")
        .description("The delay in milliseconds between swiftness potions.")
        .defaultValue(50)
        .range(0, 1000)
        .visible(() -> speed.get() && page.get() == Page.General)
        .build()
    );

    private final Setting<Boolean> only = sgGeneral.add(new BoolSetting.Builder()
        .name("on-ground-only")
        .description("Only throws potions when there is a floor to splash on.")
        .defaultValue(true)
        .visible(() -> page.get() == Page.General)
        .build()
    );

    private final Setting<Boolean> silentSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("packet-switch")
        .description("Switches the held item with a packet.")
        .defaultValue(true)
        .visible(() -> page.get() == Page.General)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The delay in seconds between bad pot throws.")
        .defaultValue(10)
        .range(0, 30)
        .visible(() -> page.get() == Page.BadPot)
        .build()
    );

    private final Setting<Double> factor = sgGeneral.add(new DoubleSetting.Builder()
        .name("factor")
        .description("The distance factor used when estimating the effect duration on hit players.")
        .defaultValue(0.75)
        .range(0, 1.5)
        .visible(() -> page.get() == Page.BadPot)
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The range to throw bad potions at enemies within.")
        .defaultValue(4.0)
        .range(0, 10)
        .visible(() -> page.get() == Page.BadPot)
        .build()
    );

    private final Setting<Integer> badSlot = sgGeneral.add(new IntSetting.Builder()
        .name("badpot-slot")
        .description("The hotbar slot to move the bad potion to.")
        .defaultValue(1)
        .range(1, 9)
        .visible(() -> page.get() == Page.BadPot)
        .build()
    );

    private final Setting<Boolean> weak = sgGeneral.add(new BoolSetting.Builder()
        .name("weakness")
        .description("Throws weakness potions at nearby enemies.")
        .defaultValue(false)
        .visible(() -> page.get() == Page.BadPot)
        .build()
    );

    private final Setting<Boolean> jump = sgGeneral.add(new BoolSetting.Builder()
        .name("jump-boost")
        .description("Throws leaping potions at nearby enemies.")
        .defaultValue(false)
        .visible(() -> page.get() == Page.BadPot)
        .build()
    );

    private final Setting<Boolean> poison = sgGeneral.add(new BoolSetting.Builder()
        .name("poison")
        .description("Throws poison potions at nearby enemies.")
        .defaultValue(false)
        .visible(() -> page.get() == Page.BadPot)
        .build()
    );

    private final Setting<Boolean> slow = sgGeneral.add(new BoolSetting.Builder()
        .name("slowness")
        .description("Throws slowness potions at nearby enemies.")
        .defaultValue(false)
        .visible(() -> page.get() == Page.BadPot)
        .build()
    );

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
        .name("debug")
        .description("Prints the current weakness effect timers.")
        .defaultValue(false)
        .visible(() -> page.get() == Page.BadPot)
        .build()
    );

    private final HashMap<Integer, Long> weaknessTime = new HashMap<>();
    private final HashMap<Integer, Long> jumpBoostTime = new HashMap<>();
    private final HashMap<Integer, Long> poisonTime = new HashMap<>();
    private final HashMap<Integer, Long> slownessTime = new HashMap<>();

    private long hpTimer;
    private long hpPredictTimer;
    private long speedTimer;
    private long lastSpeedThrow;
    private long badPotTimer;

    private int potionSlot;
    private int potSlot;
    private double lastHealth = 36.0;
    private boolean preHp;

    public AutoPot() {
        super(WeirdPvP.CATEGORY, "auto-pot", "Throws splash potions automatically.");
    }

    @Override
    public void onActivate() {
        weaknessTime.clear();
        jumpBoostTime.clear();
        poisonTime.clear();
        slownessTime.clear();
        potionSlot = -1;
        potSlot = -1;
        lastHealth = 36.0;
        preHp = false;
    }

    @Override
    public void onDeactivate() {
        potionSlot = -1;
        potSlot = -1;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.level == null || mc.player == null || mc.player.isDeadOrDying()) return;

        long time = System.currentTimeMillis();
        for (Player player : mc.level.players()) {
            int id = player.getId();
            if (weaknessTime.containsKey(id) && weaknessTime.get(id) <= time) weaknessTime.remove(id);
            if (jumpBoostTime.containsKey(id) && jumpBoostTime.get(id) <= time) jumpBoostTime.remove(id);
            if (poisonTime.containsKey(id) && poisonTime.get(id) <= time) poisonTime.remove(id);
            if (slownessTime.containsKey(id) && slownessTime.get(id) <= time) slownessTime.remove(id);
        }

        if (debug.get()) {
            StringBuilder weak = new StringBuilder("Weakness");
            for (Player player : mc.level.players()) {
                if (weaknessTime.containsKey(player.getId())) {
                    weak.append(" ").append(player.getName().getString()).append(" ").append(weaknessTime.get(player.getId()) - time).append(",");
                }
            }
            if (!weak.toString().equals("Weakness")) {
                info("%s", weak);
            }
        }

        if (!canThrow()) return;

        if (potionSlot == -1) {
            potionSlot = getPotion();
        }

        if (potSlot == -1) {
            potSlot = getBadPot();
        }

        if (potionSlot != -1 || potSlot != -1) {
            if (potionSlot > 8) {
                if (mc.gui.screen() instanceof AbstractContainerScreen && !(mc.gui.screen() instanceof InventoryScreen)) {
                    return;
                }
                int finalSlot = potionSlot == getPotionSlot("swiftness") ? swiftnessSlot.get() - 1 : healthSlot.get() - 1;
                mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, potionSlot, finalSlot, ContainerInput.SWAP, mc.player);
                potionSlot = finalSlot;
            }

            if (potSlot > 8) {
                if (mc.gui.screen() instanceof AbstractContainerScreen && !(mc.gui.screen() instanceof InventoryScreen)) {
                    return;
                }
                mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, potSlot, badSlot.get() - 1, ContainerInput.SWAP, mc.player);
                potSlot = badSlot.get() - 1;
            }

            int slot = potionSlot == -1 ? potSlot : potionSlot;
            Rotations.rotate(mc.player.getYRot(), 90, 100, () -> throwPotion(slot));
        }
    }

    private void throwPotion(int slot) {
        if (slot < 0 || slot > 8) return;
        int oldSlot = mc.player.getInventory().getSelectedSlot();
        InvUtils.swap(slot, silentSwitch.get());
        mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, mc.player.getYRot(), 90f));
        if (silentSwitch.get()) {
            InvUtils.swapBack();
        } else {
            mc.player.getInventory().setSelectedSlot(oldSlot);
        }
        potionSlot = -1;
        potSlot = -1;
    }

    private int getPotion() {
        if (hp.get()) {
            if (healthCheck(health.get()) && passedMs(hpTimer, hpDelay.get())) {
                preHp = false;
                hpTimer = System.currentTimeMillis();
                int slot = getPotionSlot("healing");
                if (slot != -1) {
                    return slot;
                }
            }

            if (predict.get()) {
                healthPredict();
            }

            if (preHp && passedMs(hpPredictTimer, predictHpDelay.get())) {
                preHp = false;
                hpPredictTimer = System.currentTimeMillis();
                int slot = getPotionSlot("healing");
                if (slot != -1) {
                    return slot;
                }
            }
        }

        if (speed.get()
            && (!mc.player.hasEffect(MobEffects.SPEED)
                || mc.player.getEffect(MobEffects.SPEED).getDuration() <= time.get() * 20)
            && passedMs(speedTimer, speedDelay.get())
            && passedMs(lastSpeedThrow, 1000)) {
            speedTimer = System.currentTimeMillis();
            lastSpeedThrow = System.currentTimeMillis();
            return getPotionSlot("swiftness");
        }

        return -1;
    }

    private int getBadPot() {
        if (passedS(badPotTimer, delay.get())) {
            badPotTimer = System.currentTimeMillis();
            for (Player player : mc.level.players()) {
                if (player != mc.player
                    && mc.getConnection().getPlayerInfo(player.getUUID()) != null
                    && !basicChecks(player)
                    && !(mc.player.distanceTo(player) > range.get())) {
                    if (weak.get() && !weaknessTime.containsKey(player.getId())) {
                        int slot = getPotionSlot("weakness");
                        if (slot != -1) {
                            return slot;
                        }
                    }

                    if (jump.get() && !jumpBoostTime.containsKey(player.getId())) {
                        int slot = getPotionSlot("leaping");
                        if (slot != -1) {
                            return slot;
                        }
                    }

                    if (poison.get() && !poisonTime.containsKey(player.getId())) {
                        int slot = getPotionSlot("poison");
                        if (slot != -1) {
                            return slot;
                        }
                    }

                    if (slow.get() && !slownessTime.containsKey(player.getId())) {
                        return getPotionSlot("slowness");
                    }
                }
            }
        }
        return -1;
    }

    private boolean basicChecks(Player player) {
        return !player.isAlive() || !Friends.get().shouldAttack(player);
    }

    private boolean healthCheck(double value) {
        return mc.player.getHealth() < value || (equal.get() && mc.player.getHealth() == value);
    }

    private void healthPredict() {
        double healthNow = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        if (healthNow == 36.0) {
            lastHealth = 36.0;
        }

        double change = healthNow - lastHealth;
        if (change < 0.0) {
            lastHealth = healthNow;
            healthNow += change * times.get();
            preHp = healthNow < health.get() || (equal.get() && healthNow == health.get());
        }
    }

    private int getPotionSlot(String potion) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() == Items.SPLASH_POTION) {
                PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
                if (contents != null) {
                    Holder<Potion> holder = contents.potion().orElse(null);
                    if (holder != null && holder.unwrapKey().isPresent()
                        && holder.unwrapKey().get().identifier().getPath().contains(potion)) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private boolean canThrow() {
        if (only.get() || mc.player.isInLava() || mc.player.isInWater()) {
            double eyeX = mc.player.getX();
            double eyeZ = mc.player.getZ();
            int feetY = mc.player.getBlockY();
            for (int dy = 1; dy <= 2; dy++) {
                BlockPos pos = BlockPos.containing(eyeX, feetY - dy, eyeZ);
                BlockState state = mc.level.getBlockState(pos);
                if (!state.isAir() && !state.getCollisionShape(mc.level, pos).isEmpty()) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private boolean passedMs(long start, int ms) {
        return System.currentTimeMillis() - start >= ms;
    }

    private boolean passedS(long start, int seconds) {
        return System.currentTimeMillis() - start >= seconds * 1000L;
    }

    @EventHandler
    private void onEntityRemoved(EntityRemovedEvent event) {
        if (event.entity instanceof ThrownSplashPotion) {
            PotionContents contents = ((ThrownSplashPotion) event.entity).getItem().get(DataComponents.POTION_CONTENTS);
            if (contents == null) {
                return;
            }

            List<MobEffectInstance> effectList = new ArrayList<>();
            contents.getAllEffects().forEach(effectList::add);

            MobEffectInstance weakness = null;
            MobEffectInstance jumpBoost = null;
            MobEffectInstance poison = null;
            MobEffectInstance slowness = null;

            for (MobEffectInstance effect : effectList) {
                if (effect.getEffect() == MobEffects.WEAKNESS) weakness = effect;
                if (effect.getEffect() == MobEffects.JUMP_BOOST) jumpBoost = effect;
                if (effect.getEffect() == MobEffects.POISON) poison = effect;
                if (effect.getEffect() == MobEffects.SLOWNESS) slowness = effect;
            }

            AABB box = event.entity.getBoundingBox().inflate(4.0, 2.0, 4.0);

            for (Player player : mc.level.players()) {
                if (player != mc.player
                    && mc.getConnection().getPlayerInfo(player.getUUID()) != null
                    && player.isAlive()
                    && box.intersects(player.getBoundingBox())) {
                    double distanceSq = event.entity.distanceToSqr(player);
                    if (distanceSq < 16.0) {
                        double factor = Math.sqrt(distanceSq) * this.factor.get();
                        if (weakness != null) {
                            double duration = factor * weakness.getDuration();
                            weaknessTime.put(player.getId(), (long) (System.currentTimeMillis() + duration * 50.0));
                        }
                        if (jumpBoost != null) {
                            double duration = factor * jumpBoost.getDuration();
                            jumpBoostTime.put(player.getId(), (long) (System.currentTimeMillis() + duration * 50.0));
                        }
                        if (poison != null) {
                            double duration = factor * poison.getDuration();
                            poisonTime.put(player.getId(), (long) (System.currentTimeMillis() + duration * 50.0));
                        }
                        if (slowness != null) {
                            double duration = factor * slowness.getDuration();
                            slownessTime.put(player.getId(), (long) (System.currentTimeMillis() + duration * 50.0));
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundRemoveEntitiesPacket) {
            for (int id : ((ClientboundRemoveEntitiesPacket) event.packet).getEntityIds()) {
                weaknessTime.remove(id);
                jumpBoostTime.remove(id);
                poisonTime.remove(id);
                slownessTime.remove(id);
            }
        }

        if (event.packet instanceof ClientboundEntityEventPacket && ((ClientboundEntityEventPacket) event.packet).getEventId() == 35) {
            int id = ((ClientboundEntityEventPacket) event.packet).getEntity(mc.level).getId();
            weaknessTime.remove(id);
            jumpBoostTime.remove(id);
            poisonTime.remove(id);
            slownessTime.remove(id);
        }
    }
}