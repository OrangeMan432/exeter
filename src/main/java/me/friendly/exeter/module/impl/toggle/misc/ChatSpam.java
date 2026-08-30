package me.friendly.exeter.module.impl.toggle.misc;

import java.util.ArrayList;
import java.util.List;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.player.LocalPlayer;

/**
 * ChatSpam. Sends configured messages on an interval (5b5t ad-chains, trade spam, dupe adverts).
 *
 * Messages cycle in order; a random suffix can be appended to dodge duplicate-message
 * filters (5b5t blocks identical consecutive messages). Delay is in ticks (20 = 1s); the
 * default 220s (~11s) matches the slowest common antispam window.
 *
 * The message list is a single string property split on {@code |} — configurable from the
 * ClickGui without a dedicated screen.
 *
 * 5b5t rate-limits chat (AEF chat-spam module), so the minimum delay is 100 ticks (5s)
 * to stay under the message-rate window; default 220 ticks (~11s) matches the slowest
 * common antispam window.
 */
public class ChatSpam extends ToggleableModule {
    private final Property<String> messages = new Property<>(
            "Sell dupes at 0,0! | Join the humza squad!", "Messages", "msg", "messages");
    private final NumberProperty<Integer> delay = new NumberProperty<>(220, 100, 1200, "Delay Ticks", "delay", "d");
    private final Property<Boolean> randomSuffix = new Property<>(true, "Random Suffix", "suffix", "rs");

    private int timer = 0;
    private int index = 0;

    public ChatSpam() {
        super("ChatSpam", new String[]{"chatspam", "spammer", "ad"}, ModuleType.MISCELLANEOUS);
        offerProperties(messages, delay, randomSuffix);

        this.listeners.add(new Listener<TickEvent>("chat_spam_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer player = minecraft.player;
                if (player == null || minecraft.getConnection() == null) return;

                if (++timer < delay.getValue()) return;
                timer = 0;

                List<String> msgs = splitMessages();
                if (msgs.isEmpty()) return;

                String msg = msgs.get(index % msgs.size());
                index++;

                // Messages support %placeholders% (e.g. "Selling at %coords%! | %players% online").
                msg = me.friendly.exeter.util.PlaceholderAPI.apply(msg);

                if (randomSuffix.getValue()) {
                    msg = msg + " \u00a77[" + Integer.toHexString((int) (Math.random() * 0xFFFF)) + "]";
                }

                minecraft.getConnection().sendChat(msg);
            }
        });
    }

    private List<String> splitMessages() {
        List<String> out = new ArrayList<>();
        for (String s : messages.getValue().split("\\|")) {
            s = s.trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        timer = 0;
    }
}
