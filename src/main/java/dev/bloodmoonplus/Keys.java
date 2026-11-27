package dev.bloodmoonplus;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

final class Keys {
    static NamespacedKey MOB, BOSS, OWNER, SCALE, DIFF, TABLE, ITEM_ID,
            ITEM_RARITY, ITEM_OWNER, DROP;

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
        ITEM_OWNER = new NamespacedKey(p, "item_owner");
        // Marca de los Items del suelo que soltó el evento (para la limpieza periódica).
        DROP = new NamespacedKey(p, "event_drop");
    }
}
