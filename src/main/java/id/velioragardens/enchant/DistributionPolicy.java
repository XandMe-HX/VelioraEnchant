package id.velioragardens.enchant;

import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

/** Reads distribution limits without tying enchant gameplay to a particular world generator. */
final class DistributionPolicy {
    enum Source { STRUCTURE_LOOT, LIBRARIAN, FISHERMAN }
    private final ConfigurationSection root;
    DistributionPolicy(ConfigurationSection root) { this.root=root; }
    boolean enabled(Source source) { return root != null && root.getBoolean(key(source)+".enabled", false); }
    double chance(Source source) { return root == null ? 0 : Math.max(0,Math.min(1,root.getDouble(key(source)+".chance",0))); }
    FishingRarity cap(Source source) { if(root==null)return FishingRarity.COMMON; try{return FishingRarity.values()[Math.min(FishingRarity.EPIC.ordinal(),FishingRarity.valueOf(root.getString(key(source)+".rarity-cap","COMMON").toUpperCase(Locale.ROOT)).ordinal())];}catch(IllegalArgumentException ignored){return FishingRarity.COMMON;} }
    List<String> lootTables() { return root==null?List.of():root.getStringList("structure-loot.allowed-tables"); }
    private String key(Source source) { return switch(source){case STRUCTURE_LOOT->"structure-loot";case LIBRARIAN->"librarian";case FISHERMAN->"fisherman";}; }
}
