/* Legacy catalogue from VelioraCustomEnchant V1, ported to the modern engine. */
package id.velioragardens.enchant;

import org.bukkit.Material;

import java.util.*;

enum LegacyEnchant {
    HASTE_TOOL(Category.TOOL),
    ABRASION(Category.WEAPON), ADRENALINE(Category.ARMOR), ARCTIC_FREEZE(Category.WEAPON), ASCEND(Category.WEAPON), AURA(Category.ARMOR), BLAZE_REAPER(Category.WEAPON), BRIGHTNESS(Category.WEAPON), CAFFEINATED(Category.WEAPON), CARVE(Category.WEAPON), CHARGE(Category.WEAPON), CONTAGION(Category.BOW), CUBISM(Category.WEAPON), DOUBLE_BLOW(Category.WEAPON), END_AFFINITY(Category.ARMOR), ENDERBANE(Category.WEAPON), ESCAPE(Category.ARMOR), EXPLOSIVE(Category.BOW), FEATHER_STEP(Category.ARMOR), FINISHING(Category.WEAPON), FIRE_HOOK(Category.FISHING_ROD), FIRST_STRIKE(Category.WEAPON), GETAWAY(Category.ARMOR), INCINERATE(Category.WEAPON), MULTI_SHOT(Category.BOW), NETHER_AFFINITY(Category.ARMOR), NINJA(Category.WEAPON), POISONED_HOOK(Category.FISHING_ROD), POSTPONE(Category.WEAPON), RAVENOUS(Category.WEAPON), REBOUNDING(Category.ARMOR), REPEL(Category.WEAPON), RESONATE(Category.ARMOR), RUMBLE(Category.ARMOR), SCORCHING(Category.ARMOR), SHARPNESS_HOOK(Category.FISHING_ROD), SHURA(Category.WEAPON), SKULLCRUSHER(Category.WEAPON), STARVATION(Category.WEAPON), THOR(Category.WEAPON), ZOMBIE_CRUSHER(Category.WEAPON),
    EMBERGUARD(Category.ARMOR), SOFT_LANDING(Category.ARMOR), TRAILBLAZER(Category.ARMOR), CLEAR_MIND(Category.ARMOR),
    PURSUIT(Category.WEAPON), CRIPPLING_SHOT(Category.BOW), RECOIL_STEP(Category.BOW), TIDAL_STRIDE(Category.WEAPON),
    MEASURED_WORK(Category.TOOL), CULTIVATOR(Category.TOOL), GENTLE_SHEAR(Category.TOOL),
    DEEPWATER_PACT(Category.FISHING_ROD), RELIC_SEEKER(Category.FISHING_ROD), SECRET_WHISPER(Category.FISHING_ROD),
    SECOND_WIND(Category.ARMOR), STEADFAST(Category.ARMOR), RIPOSTE(Category.SHIELD), CAREFUL_HANDS(Category.TOOL), PATIENT_ANGLER(Category.FISHING_ROD),
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
    WATER_BREATHING(Category.ARMOR), WIND_STRIKE(Category.WEAPON), SHIELD_RESISTANCE(Category.SHIELD), WIND_BURST(Category.MACE),
    ANGLER_LUCK(Category.FISHING_ROD), DEEP_HOOK(Category.FISHING_ROD), DOUBLE_CATCH(Category.FISHING_ROD), FISHERMAN_HEAL(Category.FISHING_ROD), TREASURE_HOOK(Category.FISHING_ROD),
    AUTO_REEL(Category.FISHING_ROD), GUARDIAN_HOOK(Category.FISHING_ROD), OCEAN_BLESSING(Category.FISHING_ROD), RIVER_SPIRIT(Category.FISHING_ROD), SUNKEN_RELIC(Category.FISHING_ROD),
    STORM_ANGLER(Category.FISHING_ROD), MERMAID_TEARS(Category.FISHING_ROD), ABYSSAL_HOOK(Category.FISHING_ROD), LEVIATHAN_LINE(Category.FISHING_ROD), VELIORA_SECRET(Category.FISHING_ROD);

    enum Category { WEAPON, TOOL, ARMOR, BOW, SHIELD, MACE, FISHING_ROD }
    private final Category category;
    LegacyEnchant(Category category) { this.category = category; }
    String id() { return name().toLowerCase(Locale.ROOT); }
    Category category() { return category; }
    static Optional<LegacyEnchant> find(String id) {
        id = ExpansionRules.alias(id);
        String compact = id.replace("_", "").toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(value -> value.name().replace("_", "").equals(compact)).findFirst();
    }
    static List<String> ids() { return Arrays.stream(values()).map(LegacyEnchant::id).toList(); }
    boolean accepts(Material material) {
        if (ExpansionRules.isNew(id())) return ExpansionRules.accepts(id(),material);
        if (this == EMBERGUARD) return material.name().endsWith("_CHESTPLATE");
        if (this == SOFT_LANDING || this == TRAILBLAZER) return material.name().endsWith("_BOOTS");
        if (this == CLEAR_MIND) return material.name().endsWith("_HELMET");
        if (this == PURSUIT) return material.name().endsWith("_SWORD");
        if (this == CRIPPLING_SHOT) return material == Material.BOW;
        if (this == RECOIL_STEP) return material == Material.CROSSBOW;
        if (this == TIDAL_STRIDE) return material == Material.TRIDENT;
        if (this == CULTIVATOR) return material.name().endsWith("_HOE");
        if (this == GENTLE_SHEAR) return material == Material.SHEARS;
        if (this == MEASURED_WORK) return material.name().endsWith("_PICKAXE") || material.name().endsWith("_AXE") || material.name().endsWith("_SHOVEL");
        if (this == SECOND_WIND) return material.name().endsWith("_CHESTPLATE");
        if (this == STEADFAST) return material.name().endsWith("_LEGGINGS");
        return switch (category) {
        case WEAPON -> material.name().endsWith("_SWORD") || material.name().endsWith("_AXE") || material == Material.MACE || material == Material.TRIDENT;
        case TOOL -> material.name().endsWith("_PICKAXE") || material.name().endsWith("_AXE") || material.name().endsWith("_SHOVEL") || material.name().endsWith("_HOE");
        case ARMOR -> material.name().endsWith("_HELMET") || material.name().endsWith("_CHESTPLATE") || material.name().endsWith("_LEGGINGS") || material.name().endsWith("_BOOTS") || material == Material.ELYTRA;
        case BOW -> material == Material.BOW || material == Material.CROSSBOW;
        case SHIELD -> material == Material.SHIELD;
        case MACE -> material == Material.MACE;
        case FISHING_ROD -> material == Material.FISHING_ROD;
    }; }
}
