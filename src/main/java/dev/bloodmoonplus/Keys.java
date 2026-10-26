package dev.bloodmoonplus;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

final class Keys {
    static NamespacedKey MOB, BOSS, SCALE, DIFF, TABLE, ITEM_ID, ITEM_ABILITIES, ITEM_PASSIVES,
            ITEM_RARITY, ITEM_SERIAL, BLEED, HEAL_AURA;

    private static Keys instance;

    private Keys() {}

    static Keys instance() {
        return instance;
    }

    static void init(Plugin p) {
        instance = new Keys();

        MOB = new NamespacedKey(p, "mob");
        BOSS = new NamespacedKey(p, "boss");
        SCALE = new NamespacedKey(p, "dmg_scale");
        DIFF = new NamespacedKey(p, "difficulty");
        TABLE = new NamespacedKey(p, "table");
        ITEM_ID = new NamespacedKey(p, "item_id");
        ITEM_ABILITIES = new NamespacedKey(p, "abilities");
        ITEM_PASSIVES = new NamespacedKey(p, "passives");
        ITEM_RARITY = new NamespacedKey(p, "rarity");
        ITEM_SERIAL = new NamespacedKey(p, "serial");
        BLEED = new NamespacedKey(p, "bleed_ticks");
        HEAL_AURA = new NamespacedKey(p, "heal_aura");
    }
}
