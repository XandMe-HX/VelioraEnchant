package id.velioragardens.enchant;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WaveOneRulesTest {
    @Test void secondWindNeedsSurvivingThresholdCrossing() {
        assertTrue(WaveOneRules.crossesLowHealth(10, 20, 6));
        assertFalse(WaveOneRules.crossesLowHealth(4, 20, 1));
        assertFalse(WaveOneRules.crossesLowHealth(10, 20, 10));
        assertFalse(WaveOneRules.crossesLowHealth(10, 20, 0));
        assertTrue(WaveOneRules.crossesLowHealth(30, 100, 6));
    }
    @Test void armorSlotsRestricted() {
        assertTrue(LegacyEnchant.SECOND_WIND.accepts(Material.NETHERITE_CHESTPLATE));
        assertFalse(LegacyEnchant.SECOND_WIND.accepts(Material.ELYTRA));
        assertFalse(LegacyEnchant.SECOND_WIND.accepts(Material.DIAMOND_BOOTS));
        assertTrue(LegacyEnchant.STEADFAST.accepts(Material.IRON_LEGGINGS));
        assertFalse(LegacyEnchant.STEADFAST.accepts(Material.IRON_HELMET));
    }
    @Test void toolShieldRodRestrictions() {
        assertTrue(LegacyEnchant.CAREFUL_HANDS.accepts(Material.DIAMOND_PICKAXE));
        assertTrue(LegacyEnchant.CAREFUL_HANDS.accepts(Material.NETHERITE_HOE));
        assertFalse(LegacyEnchant.CAREFUL_HANDS.accepts(Material.DIAMOND_SWORD));
        assertTrue(LegacyEnchant.RIPOSTE.accepts(Material.SHIELD));
        assertFalse(LegacyEnchant.RIPOSTE.accepts(Material.BOW));
        assertTrue(LegacyEnchant.PATIENT_ANGLER.accepts(Material.FISHING_ROD));
    }
    @Test void durabilityCannotReachBreakingPoint() {
        assertEquals(1, WaveOneRules.allowedWear(100, 98, 9, 1));
        assertEquals(0, WaveOneRules.allowedWear(100, 99, 1, 1));
        assertEquals(0, WaveOneRules.allowedWear(100, 97, 8, 3));
        assertEquals(2, WaveOneRules.allowedWear(100, 10, 2, 3));
    }
    @Test void knockbackNeverImmune() {
        assertEquals(.9, WaveOneRules.knockbackMultiplier(1), 1e-9);
        assertEquals(.7, WaveOneRules.knockbackMultiplier(3), 1e-9);
        assertEquals(.7, WaveOneRules.knockbackMultiplier(100), 1e-9);
    }
    @Test void patienceStartsAfterFiveCatchesAndCaps() {
        assertEquals(0, WaveOneRules.patienceBonus(4, 5));
        assertEquals(.002, WaveOneRules.patienceBonus(5, 1), 1e-9);
        assertEquals(.01, WaveOneRules.patienceBonus(5, 5), 1e-9);
        assertEquals(.10, WaveOneRules.patienceBonus(Integer.MAX_VALUE, 5), 1e-9);
        assertEquals(0, WaveOneRules.patienceBonus(0, 5));
        assertEquals(0, WaveOneRules.patienceBonus(100, 0));
    }
}
