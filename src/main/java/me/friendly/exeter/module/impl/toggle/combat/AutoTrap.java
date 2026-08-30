package me.friendly.exeter.module.impl.toggle.combat;

import java.util.Comparator;

import me.friendly.api.event.Listener;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * AutoTrap. 把坑里的敌人封顶 — 在敌方头部上方盖一层天花板（可选四壁延伸），让他
 * 完全失去 crystal 落点和逃跑路径。cpvp 标配进攻模块（Future/Rusherhack 均有）。
 *
 * 逻辑 per tick：
 *  - 目标：最近敌人（跳过好友），要求其处于坑中（头部上方为空）且在 Place Range 内。
 *  - 位置：目标头顶 2 格（head+1）那层，围绕目标四壁的空位逐格放置（每 tick 一块）。
 *    可选 Top Only：只封顶不封壁（省料、快）。
 *  - 放置走 vanilla useItemOn（服务端校验），支持 silent rotation，受 Delay Ticks 节流。
 *
 * 安全：不往任何玩家（含自己）的碰撞箱里放块；尊重 AntiCrystal place timeout；
 * 屏幕/使用物品时暂停。
 */
public class AutoTrap extends ToggleableModule {
    private final NumberProperty<Double> placeRange = new NumberProperty<>(4.5, 1.0, 6.0, "Place Range", "range", "r");
    private final NumberProperty<Integer> delayTicks = new NumberProperty<>(0, 0, 10, "Delay Ticks", "delay", "d");
    private final Property<Boolean> topOnly = new Property<>(false, "Top Only", "toponly", "to");
    private final Property<Boolean> silentAim = new Property<>(true, "Silent Aim", "silentaim", "sa");
    private final Property<Boolean> skipFriends = new Property<>(true, "Skip Friends", "friends", "f");

    private int cooldown = 0;

    public AutoTrap() {
        super("AutoTrap", new String[]{"autotrap", "trap"}, ModuleType.COMBAT);
        offerProperties(placeRange, delayTicks, topOnly, silentAim, skipFriends);

        this.listeners.add(new Listener<TickEvent>("auto_trap_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer self = minecraft.player;
                if (self == null || minecraft.level == null || minecraft.gameMode == null) return;
                if (minecraft.gui.screen() != null || self.isUsingItem() || self.isDeadOrDying()) return;

                if (cooldown > 0) {
                    cooldown--;
                    return;
                }

                var antiCrystal = Exeter.getInstance().getModuleManager().getModuleByAlias("anticrystal");
                if (antiCrystal instanceof AntiCrystal ac && ac.isPlaceBlocked()) return;

                Player enemy = nearestEnemy(self);
                if (enemy == null) return;

                // 目标头顶层：脚 +2（头 +1 再上一层，留出放置空间）。
                BlockPos ceilingY = enemy.blockPosition().above(2);

                // 封顶优先：目标头顶正上方的四格（其身位所在格 + 相邻格均可形成盖板）。
                BlockPos cap = enemy.blockPosition().above(2);
                if (needsBlock(cap) && !entityInside(cap) && PlayerUtil.inRange(cap, placeRange.getValue())) {
                    if (placeBlock(cap)) cooldown = Math.max(1, delayTicks.getValue());
                    return;
                }

                if (topOnly.getValue()) return;

                // 封壁：头顶层四周（把盖板延伸成完整罩子）。
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockPos wall = cap.relative(d);
                    if (!needsBlock(wall) || entityInside(wall)) continue;
                    if (!PlayerUtil.inRange(wall, placeRange.getValue())) continue;
                    if (placeBlock(wall)) cooldown = Math.max(1, delayTicks.getValue());
                    return; // one per tick
                }
            }
        });
    }

    private Player nearestEnemy(LocalPlayer self) {
        return minecraft.level.players().stream()
                .filter(p -> p != self && p.isAlive())
                .filter(p -> !(skipFriends.getValue()
                        && Exeter.getInstance().getFriendManager().isFriend(p.getName().getString())))
                .filter(p -> self.distanceTo(p) <= placeRange.getValue() + 2.0)
                .filter(p -> minecraft.level.getBlockState(p.blockPosition().above(2)).isAir()) // 头顶有空位才值得封
                .min(Comparator.comparingDouble(self::distanceTo))
                .orElse(null);
    }

    private boolean needsBlock(BlockPos pos) {
        var state = minecraft.level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    private boolean entityInside(BlockPos pos) {
        var box = new net.minecraft.world.phys.AABB(pos);
        for (var p : minecraft.level.players()) {
            if (p.isAlive() && p.getBoundingBox().intersects(box)) return true;
        }
        return false;
    }

    private boolean placeBlock(BlockPos pos) {
        int slot = findBlockSlot();
        if (slot == -1) return false;

        Direction face = adjacentFace(pos);
        if (face == null) return false;

        int original = minecraft.player.getInventory().getSelectedSlot();
        boolean swapped = slot != original;
        if (swapped) PlayerUtil.swapTo(slot);

        if (silentAim.getValue()) {
            double yaw = PlayerUtil.getYaw(pos);
            double pitch = PlayerUtil.getPitch(pos);
            PlayerUtil.withRotation(yaw, pitch, () -> {
                PlayerUtil.useItemOn(pos, face);
                PlayerUtil.swingHand();
            });
        } else {
            PlayerUtil.useItemOn(pos, face);
            PlayerUtil.swingHand();
        }

        if (swapped) PlayerUtil.swapBack();
        return true;
    }

    private int findBlockSlot() {
        var inv = minecraft.player.getInventory();
        for (int i = 0; i <= 8; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem bi)) continue;
            var block = bi.getBlock();
            if (block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN) {
                return i;
            }
        }
        // 兜底：任何方块都行（Trap 不挑料）。
        for (int i = 0; i <= 8; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) return i;
        }
        return -1;
    }

    private Direction adjacentFace(BlockPos target) {
        for (Direction d : Direction.values()) {
            BlockPos adj = target.relative(d);
            var state = minecraft.level.getBlockState(adj);
            if (!state.isAir() && !state.canBeReplaced()
                    && !state.getCollisionShape(minecraft.level, adj).isEmpty()) {
                return d.getOpposite();
            }
        }
        return null;
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        cooldown = 0;
    }
}
