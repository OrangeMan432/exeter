package me.earth.earthhack.impl.modules.combat.nofriendattack;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Cancels attacks against friends.
 * Ported from Phobos 1.9 (BlockTweaks NoFriendAttack).
 */
public class NoFriendAttack extends Module
{
    public NoFriendAttack()
    {
        super("NoFriendAttack", Category.Combat);
        this.listeners.add(new ListenerSend(this));
        this.setData(new SimpleData(this,
            "Never hit friends, even by accident."));
    }
}
