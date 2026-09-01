/* Legacy catalogue from VelioraCustomEnchant V1, ported to the modern engine. */
package id.velioragardens.enchant;

import org.bukkit.Material;

import java.util.*;

enum LegacyEnchant {
    ABSORB(Category.ARMOR), ANTI_STUN(Category.ARMOR), AUTO_FARM(Category.TOOL), AUTO_REPAIR(Category.TOOL), AUTO_SMELT(Category.TOOL), AXOLOTL_BUFF(Category.ARMOR),
    BARRIER(Category.ARMOR), BLAST(Category.WEAPON), BLEED(Category.WEAPON), BLIND(Category.WEAPON), BLINDING_ARROW(Category.BOW), BLOCK(Category.ARMOR), BURNING(Category.WEAPON),
    COBWEB(Category.WEAPON), CRAVING(Category.WEAPON), CRITICAL(Category.WEAPON), DEATH_ANGEL(Category.ARMOR), DEBUFF(Category.WEAPON),
    DOLPHINS_GRACE(Category.ARMOR), DUCTILE(Category.ARMOR), EMERGENCY_DEFENCE(Category.ARMOR), EMNITY(Category.WEAPON), EXPERIENCE(Category.TOOL), FIRE_BOOTS(Category.ARMOR), FLOWER(Category.TOOL),
    FOCUS_FIRE(Category.BOW), FOOD_POCKET(Category.ARMOR), FORCE_SHIELD(Category.ARMOR), FREEZE(Category.WEAPON), FROST_ARROW(Category.BOW), FROZEN_HOOK(Category.BOW), GRIMOIRE(Category.WEAPON),
    GUARDED(Category.ARMOR), HAIL_STORM(Category.WEAPON), HASTE_AURA(Category.ARMOR), HEAL(Category.ARMOR), ILLUSION(Category.WEAPON), IMPLANT(Category.WEAPON), JUMP(Category.ARMOR),
    LEVELS(Category.TOOL), LIFE_STEAL(Category.WEAPON), LIGHTNING(Category.WEAPON), LIGHT_SPIRIT(Category.ARMOR), LUCK(Category.TOOL), LUCKY_TREASURE(Category.TOOL),
    MOLTEN(Category.ARMOR), NIGHT_VISION(Category.ARMOR), NULLED(Category.WEAPON), OBSIDIAN_PLATE(Category.ARMOR), OMNIVAMP(Category.WEAPON), PHOENIX(Category.ARMOR), POISON(Category.WEAPON),
    POISONOUS_THORNS(Category.ARMOR), PROTECTION(Category.ARMOR), REGAIN(Category.ARMOR), REGENERATION(Category.ARMOR), SATURATION(Category.ARMOR), SECOND_LIFE(Category.ARMOR), SHARPEN(Category.WEAPON),
    SLOW_FALL(Category.ARMOR), SOUL_EATER(Category.WEAPON), SPEED(Category.ARMOR), STEAL(Category.WEAPON), STELLA(Category.ARMOR), STORM(Category.WEAPON), STURDY(Category.ARMOR),
    SUDDEN_BLOW(Category.WEAPON), TANK(Category.ARMOR), TELEPATHY(Category.TOOL), TIME_TRAVEL(Category.ARMOR), UNBREAKING(Category.TOOL),
    WATER_BREATHING(Category.ARMOR), WIND_STRIKE(Category.WEAPON), SHIELD_RESISTANCE(Category.SHIELD), WIND_BURST(Category.MACE);

    enum Category { WEAPON, TOOL, ARMOR, BOW, SHIELD, MACE }
    private final Category category;
    LegacyEnchant(Category category) { this.category = category; }
    String id() { return name().toLowerCase(Locale.ROOT); }
    Category category() { return category; }
    static Optional<LegacyEnchant> find(String id) {
        String compact = id.replace("_", "").toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(value -> value.name().replace("_", "").equals(compact)).findFirst();
    }
    static List<String> ids() { return Arrays.stream(values()).map(LegacyEnchant::id).toList(); }
    boolean accepts(Material material) { return switch (category) {
        case WEAPON -> material.name().endsWith("_SWORD") || material.name().endsWith("_AXE") || material == Material.MACE || material == Material.TRIDENT;
        case TOOL -> material.name().endsWith("_PICKAXE") || material.name().endsWith("_AXE") || material.name().endsWith("_SHOVEL") || material.name().endsWith("_HOE");
        case ARMOR -> material.name().endsWith("_HELMET") || material.name().endsWith("_CHESTPLATE") || material.name().endsWith("_LEGGINGS") || material.name().endsWith("_BOOTS") || material == Material.ELYTRA;
        case BOW -> material == Material.BOW || material == Material.CROSSBOW;
        case SHIELD -> material == Material.SHIELD;
        case MACE -> material == Material.MACE;
    }; }
}
