package id.velioragardens.enchant;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class WaveTwoCatalogueTest {
    @Test void allFourteenAcceptTheirIntendedItem() {
        Map<LegacyEnchant,Material> cases=Map.ofEntries(
            Map.entry(LegacyEnchant.EMBERGUARD,Material.DIAMOND_CHESTPLATE),
            Map.entry(LegacyEnchant.SOFT_LANDING,Material.DIAMOND_BOOTS),
            Map.entry(LegacyEnchant.TRAILBLAZER,Material.DIAMOND_BOOTS),
            Map.entry(LegacyEnchant.CLEAR_MIND,Material.DIAMOND_HELMET),
            Map.entry(LegacyEnchant.PURSUIT,Material.DIAMOND_SWORD),
            Map.entry(LegacyEnchant.CRIPPLING_SHOT,Material.BOW),
            Map.entry(LegacyEnchant.RECOIL_STEP,Material.CROSSBOW),
            Map.entry(LegacyEnchant.TIDAL_STRIDE,Material.TRIDENT),
            Map.entry(LegacyEnchant.MEASURED_WORK,Material.DIAMOND_PICKAXE),
            Map.entry(LegacyEnchant.CULTIVATOR,Material.DIAMOND_HOE),
            Map.entry(LegacyEnchant.GENTLE_SHEAR,Material.SHEARS),
            Map.entry(LegacyEnchant.DEEPWATER_PACT,Material.FISHING_ROD),
            Map.entry(LegacyEnchant.RELIC_SEEKER,Material.FISHING_ROD),
            Map.entry(LegacyEnchant.SECRET_WHISPER,Material.FISHING_ROD));
        assertEquals(14,cases.size());
        cases.forEach((enchant,item)->{ assertTrue(enchant.accepts(item),enchant.id()); assertFalse(enchant.accepts(Material.STONE),enchant.id()); assertEquals(enchant,LegacyEnchant.find(enchant.id()).orElseThrow()); });
    }
    @Test void bowsAndToolsCannotCrossApply() {
        assertFalse(LegacyEnchant.CRIPPLING_SHOT.accepts(Material.CROSSBOW));
        assertFalse(LegacyEnchant.RECOIL_STEP.accepts(Material.BOW));
        assertFalse(LegacyEnchant.PURSUIT.accepts(Material.DIAMOND_AXE));
        assertFalse(LegacyEnchant.MEASURED_WORK.accepts(Material.DIAMOND_HOE));
        assertFalse(LegacyEnchant.CULTIVATOR.accepts(Material.DIAMOND_PICKAXE));
    }
    @Test void fishingBooksHaveRarityPools() {
        assertTrue(FishingRarity.LEGENDARY.enchantments.contains("deepwater_pact"));
        assertTrue(FishingRarity.MYTHIC.enchantments.contains("relic_seeker"));
        assertTrue(FishingRarity.SECRET.enchantments.contains("secret_whisper"));
    }
}
