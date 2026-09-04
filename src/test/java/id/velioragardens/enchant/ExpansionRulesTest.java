package id.velioragardens.enchant;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ExpansionRulesTest {
    @Test void allEntriesRegisteredAndDescriptionsPresent() {
        assertEquals(41,ExpansionRules.SPECS.size());
        ExpansionRules.SPECS.forEach((id,s)->{
            assertTrue(LegacyEnchant.find(id).isPresent(),id);
            assertFalse(s.description().isBlank(),id);
            assertTrue(s.max()>0 && s.max()<=9);
            assertFalse(ExpansionRules.accepts(id,Material.STONE),id);
        });
    }
    @Test void excludedEntriesStayExcluded() {
        for(String id:new String[]{"vanish","allurement","foraging","nether_prospector","blast_mining","curse_of_breaklessness","curse_of_harmlessness","fuddle","drain"})
            assertFalse(LegacyEnchant.find(id).isPresent(),id);
    }
    @Test void reflectionIsCappedAndNeverNegative() {
        assertEquals(3,ExpansionRules.reflection(1000,3));
        assertEquals(0,ExpansionRules.reflection(-1,3));
        assertEquals(1.5,ExpansionRules.reflection(10,3),1e-9);
        assertEquals(0,ExpansionRules.reflection(10,0));
    }
    @Test void aliasesDoNotCreateDuplicates() {
        assertEquals(LegacyEnchant.AUTO_SMELT,LegacyEnchant.find("infernal_touch").orElseThrow());
        assertEquals(LegacyEnchant.HASTE_TOOL,LegacyEnchant.find("alacrity").orElseThrow());
        assertEquals(LegacyEnchant.BLIND,LegacyEnchant.find("blackout").orElseThrow());
    }
    @Test void specificItemsAndLevels() {
        assertTrue(ExpansionRules.accepts("thor",Material.CROSSBOW));
        assertFalse(ExpansionRules.accepts("thor",Material.DIAMOND_AXE));
        assertTrue(ExpansionRules.accepts("double_blow",Material.TRIDENT));
        assertFalse(ExpansionRules.accepts("double_blow",Material.DIAMOND_SWORD));
        assertEquals(9,ExpansionRules.SPECS.get("sharpness_hook").max());
    }
}
