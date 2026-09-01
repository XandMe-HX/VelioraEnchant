package id.velioragardens.enchant;

import org.bukkit.configuration.ConfigurationSection;

/** Configurable per-level formula shared by fishing enchant effects. */
record LevelModifier(double base, double perLevel, double capacity, Action action) {
    enum Action { ADD, MULTIPLY }
    static LevelModifier from(ConfigurationSection section, double base, double perLevel, double capacity, Action action) {
        if (section == null) return new LevelModifier(base, perLevel, capacity, action);
        Action configured;
        try { configured = Action.valueOf(section.getString("Action", action.name()).toUpperCase()); }
        catch (IllegalArgumentException ignored) { configured = action; }
        return new LevelModifier(section.getDouble("Base", base), section.getDouble("Per_Level", perLevel), section.getDouble("Capacity", capacity), configured);
    }
    double value(int level) {
        if (level <= 0) return 0;
        double raw = action == Action.ADD ? base + perLevel * (level - 1) : base * Math.pow(perLevel, level - 1);
        return capacity > 0 ? Math.min(capacity, raw) : raw;
    }
}
