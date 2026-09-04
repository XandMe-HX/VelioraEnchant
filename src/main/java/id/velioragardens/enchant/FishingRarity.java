package id.velioragardens.enchant;

import net.kyori.adventure.text.format.NamedTextColor;
import java.util.List;

enum FishingRarity {
    COMMON("common", NamedTextColor.WHITE, List.of("angler_luck", "deep_hook")),
    RARE("rare", NamedTextColor.GREEN, List.of("double_catch", "fisherman_heal", "treasure_hook")),
    EPIC("epic", NamedTextColor.LIGHT_PURPLE, List.of("auto_reel", "guardian_hook", "ocean_blessing", "river_spirit", "patient_angler")),
    LEGENDARY("legendary", NamedTextColor.GOLD, List.of("sunken_relic", "storm_angler", "mermaid_tears")),
    MYTHIC("mythic", NamedTextColor.RED, List.of("abyssal_hook", "leviathan_line")),
    SECRET("secret", NamedTextColor.DARK_PURPLE, List.of("veliora_secret"));
    final String id; final NamedTextColor color; final List<String> enchantments;
    FishingRarity(String id, NamedTextColor color, List<String> enchantments) { this.id=id; this.color=color; this.enchantments=enchantments; }
}
