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

    /** Encantamientos de ExcellentEnchants: viven en su propio namespace del registro de
     *  Bukkit, así que no hace falta compilar contra su API. Si el plugin no está, el
     *  registro devuelve null y el encantamiento simplemente se omite. */
    private static final String EXCELLENT = "excellentenchants";

    static Enchantment enchantment(String key) {
        NamespacedKey k = NamespacedKey.fromString(norm(key));
        if (k == null) return null;
        Enchantment vanilla = Registry.ENCHANTMENT.get(k);
        if (vanilla != null) return vanilla;
        NamespacedKey custom = NamespacedKey.fromString(EXCELLENT + ":" + norm(key));
        return custom == null ? null : Registry.ENCHANTMENT.get(custom);
    }

    /** true si el encantamiento resuelto es de ExcellentEnchants (namespace propio). */
    static boolean isCustom(Enchantment en) {
        return en != null && EXCELLENT.equals(en.getKey().getNamespace());
    }

    static PotionEffectType effect(String key) {
        NamespacedKey k = NamespacedKey.fromString(norm(key));
        return k == null ? null : Registry.EFFECT.get(k);
    }
}
