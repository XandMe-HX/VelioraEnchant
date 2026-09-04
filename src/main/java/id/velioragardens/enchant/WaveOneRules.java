package id.velioragardens.enchant;

final class WaveOneRules {
    private WaveOneRules() {}
    static double patienceBonus(int misses, int level) {
        if (misses < 5 || level <= 0) return 0;
        return Math.min(.10, (Math.min(100, misses) - 4) * Math.min(5, level) * .002);
    }
    static int allowedWear(int maxDamage, int current, int incoming, int level) {
        return Math.min(Math.max(0, incoming), Math.max(0, maxDamage - Math.clamp(level, 1, 3) - current));
    }
    static boolean crossesLowHealth(double health, double max, double damage) {
        return max > 0 && damage > 0 && health > max * .25 && health - damage > 0 && health - damage <= max * .25;
    }
    static double knockbackMultiplier(int level) { return 1 - Math.clamp(level, 0, 3) * .10; }
}
