package dev.bloodmoonplus;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;

/** Único punto de acceso a los registros de Bukkit (si la API cambia, solo se toca este archivo). */
final class Reg {
    private Reg() {}

    static String norm(String s) {
        String k = s.trim().toLowerCase(Locale.ROOT);
        return k.startsWith("minecraft:") ? k.substring(10) : k;
    }

    static Enchantment enchantment(String key) {
        NamespacedKey k = NamespacedKey.fromString(norm(key));
        return k == null ? null : Registry.ENCHANTMENT.get(k);
    }

    static PotionEffectType effect(String key) {
        NamespacedKey k = NamespacedKey.fromString(norm(key));
        return k == null ? null : Registry.EFFECT.get(k);
    }
}
