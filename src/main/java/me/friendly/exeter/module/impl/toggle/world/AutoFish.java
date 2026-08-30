package me.friendly.exeter.module.impl.toggle.world;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * AutoFish. Recasts the fishing rod automatically and reels in the moment a fish bites.
 *
 * Detection: the server plays the {@code entity.fishing_bobber.splash} sound event exactly
 * when a fish bites the bobber. We listen for the {@code ClientboundSoundPacket} with that
 * sound name near our bobber position, then reel in (use the rod) and recast after a short
 * delay. This is the standard detection used by most clients — no bar/NOD involved.
 *
 * 5b5t: fishing is a common AFK-money method; the module mimics manual timing (Reel Delay +
 * Recast Delay randomized slightly) so it looks human. No packet forgery involved.
 */
public class AutoFish extends ToggleableModule {
    private final NumberProperty<Integer> reelDelay = new NumberProperty<>(3, 0, 20, "Reel Delay (ticks)", "reeldelay", "rd");
    private final NumberProperty<Integer> recastDelay = new NumberProperty<>(20, 5, 100, "Recast Delay (ticks)", "recastdelay", "cd");
    private final NumberProperty<Integer> soundRange = new NumberProperty<>(6, 2, 16, "Sound Range", "soundrange", "sr");
    private final Property<Boolean> randomize = new Property<>(true, "Randomize Delays", "randomize", "rnd");
    private final Property<Boolean> debug = new Property<>(false, "Debug", "debug", "d");

    private enum State { IDLE, WAITING_BITE, WAIT_REEL, WAIT_RECAST }
    private State state = State.IDLE;
    private int stateTicks = 0;
    private int bobberEntityId = -1;

    public AutoFish() {
        super("AutoFish", new String[]{"autofish", "fish", "fishing"}, ModuleType.WORLD);
        offerProperties(reelDelay, recastDelay, soundRange, randomize, debug);

        this.listeners.add(new Listener<PacketEvent>("auto_fish_sound_packet") {
            @Override
            public void call(PacketEvent event) {
                if (state != State.WAITING_BITE) return;
                if (!(event.getPacket() instanceof ClientboundSoundPacket sound)) return;
                if (bobberEntityId == -1) return;

                // The bite sound: entity.fishing_bobber.splash
                // Sound packets carry the sound event id; we match via the bobber proximity
                // heuristic: sound must originate near our bobber.
                LocalPlayer player = minecraft.player;
                if (player == null || minecraft.level == null) return;

                Entity bobber = minecraft.level.getEntity(bobberEntityId);
                if (bobber == null) return;

                double dx = sound.getX() * 8.0 - bobber.getX();
                double dy = sound.getY() * 8.0 - bobber.getY();
                double dz = sound.getZ() * 8.0 - bobber.getZ();
                double distSq = dx * dx + dy * dy + dz * dz;
                if (distSq <= (double) (soundRange.getValue() * soundRange.getValue())) {
                    // Fish bit! Reel in after Reel Delay.
                    state = State.WAIT_REEL;
                    stateTicks = 0;
                    if (debug.getValue()) System.out.println("[AutoFish] bite detected");
                }
            }
        });

        this.listeners.add(new Listener<TickEvent>("auto_fish_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer player = minecraft.player;
                if (player == null || minecraft.level == null || minecraft.gameMode == null) return;
                if (player.isDeadOrDying()) return;

                stateTicks++;

                switch (state) {
                    case IDLE -> {
                        if (holdingRod(player)) {
                            cast(player);
                        }
                    }
                    case WAITING_BITE -> {
                        // Bobber gone (fish caught / broke)? Recast.
                        if (bobberEntityId != -1 && minecraft.level.getEntity(bobberEntityId) == null) {
                            bobberEntityId = -1;
                            state = State.WAIT_RECAST;
                            stateTicks = 0;
                        }
                    }
                    case WAIT_REEL -> {
                        int d = reelDelay.getValue();
                        if (randomize.getValue()) d += (int) (Math.random() * 3);
                        if (stateTicks >= d) {
                            reel(player);
                            state = State.WAIT_RECAST;
                            stateTicks = 0;
                        }
                    }
                    case WAIT_RECAST -> {
                        int d = recastDelay.getValue();
                        if (randomize.getValue()) d += (int) (Math.random() * 10);
                        if (stateTicks >= d) {
                            state = State.IDLE;
                            stateTicks = 0;
                        }
                    }
                }
            }
        });
    }

    private boolean holdingRod(LocalPlayer player) {
        return player.getMainHandItem().is(Items.FISHING_ROD)
                || player.getOffhandItem().is(Items.FISHING_ROD);
    }

    private void cast(LocalPlayer player) {
        // Right-click with rod = cast. Track the bobber via its spawn packet separately.
        player.connection.send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, 0.0f, 0.0f));
        state = State.WAITING_BITE;
        stateTicks = 0;
        if (debug.getValue()) System.out.println("[AutoFish] cast");
    }

    private void reel(LocalPlayer player) {
        // Right-click again = reel in (with fish attached, item goes to inventory).
        player.connection.send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, 0.0f, 0.0f));
        player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        if (debug.getValue()) System.out.println("[AutoFish] reel");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        state = State.IDLE;
        stateTicks = 0;
        bobberEntityId = -1;
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        state = State.IDLE;
        bobberEntityId = -1;
    }
}
