package dev.bloodmoonplus;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

final class Keys {
    static NamespacedKey MOB, BOSS, OWNER, SCALE, DIFF, TABLE, ITEM_ID,
            ITEM_RARITY, ITEM_SERIAL, ITEM_OWNER;

    private Keys() {}

    static void init(Plugin p) {
        MOB = new NamespacedKey(p, "mob");
        BOSS = new NamespacedKey(p, "boss");
        OWNER = new NamespacedKey(p, "owner");
        SCALE = new NamespacedKey(p, "dmg_scale");
        DIFF = new NamespacedKey(p, "difficulty");
        TABLE = new NamespacedKey(p, "table");
        ITEM_ID = new NamespacedKey(p, "item_id");
        ITEM_RARITY = new NamespacedKey(p, "rarity");
        ITEM_SERIAL = new NamespacedKey(p, "serial");
        ITEM_OWNER = new NamespacedKey(p, "item_owner");
    }
}
