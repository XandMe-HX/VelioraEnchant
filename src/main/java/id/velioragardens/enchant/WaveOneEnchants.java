package id.velioragardens.enchant;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTables;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Event-driven mechanics. No player/area scans or repeating tasks. */
final class WaveOneEnchants implements Listener {
    private final VelioraEnchantPlugin plugin;
    private final Map<UUID, Charge> charges = new HashMap<>();
    private final Map<UUID, Cast> casts = new HashMap<>();
    private record Charge(long expires, int level) {}
    private record Cast(UUID hook, int level, long started) {}
    WaveOneEnchants(VelioraEnchantPlugin plugin) { this.plugin = plugin; }
    void clear() { charges.clear(); casts.clear(); }
    private NamespacedKey key(String value) { return new NamespacedKey(plugin, "wave1_" + value); }
    private int level(ItemStack item, String id) { return plugin.waveLevel(item, id); }
    private boolean cooldown(Player p, String id, long defaultTicks) {
        long now = System.currentTimeMillis();
        long until = p.getPersistentDataContainer().getOrDefault(key(id), PersistentDataType.LONG, 0L);
        if (until > now) return false;
        long configured = plugin.getConfig().getLong("custom-enchants." + id + ".cooldown-ticks", defaultTicks);
        if (configured < 0) configured = defaultTicks;
        p.getPersistentDataContainer().set(key(id), PersistentDataType.LONG, now + Math.clamp(configured, 20, 72000) * 50);
        return true;
    }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void secondWind(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        int level = level(p.getInventory().getChestplate(), "second_wind");
        if (level == 0 || !WaveOneRules.crossesLowHealth(p.getHealth(), p.getMaxHealth(), event.getFinalDamage())) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (event.isCancelled() || !p.isOnline() || p.isDead() || p.getHealth() > p.getMaxHealth() * .25
                    || level(p.getInventory().getChestplate(), "second_wind") == 0) return;
            if (cooldown(p, "second_wind", 1800)) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40 + level * 20, 0));
                p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, .35f, 1.2f);
                p.sendActionBar(Component.text("Second Wind aktif", NamedTextColor.AQUA));
            }
        });
    }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void steadfast(EntityKnockbackEvent event) {
        if (!(event.getEntity() instanceof Player p) || !p.isSneaking()) return;
        if (event.getCause() != EntityKnockbackEvent.Cause.ENTITY_ATTACK
                && event.getCause() != EntityKnockbackEvent.Cause.SWEEP_ATTACK
                && event.getCause() != EntityKnockbackEvent.Cause.DAMAGE) return;
        int level = level(p.getInventory().getLeggings(), "steadfast");
        if (level > 0) event.setKnockback(event.getKnockback().clone().multiply(WaveOneRules.knockbackMultiplier(level)));
    }
    @SuppressWarnings("deprecation")
    private boolean blocked(EntityDamageByEntityEvent event) {
        return event.isApplicable(EntityDamageEvent.DamageModifier.BLOCKING)
                && event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) < 0;
    }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void chargeRiposte(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player p) || !p.isBlocking() || !blocked(event)) return;
        int level = level(p.getActiveItem(), "riposte");
        if (level > 0 && cooldown(p, "riposte", 200)) {
            charges.put(p.getUniqueId(), new Charge(System.currentTimeMillis() + 4000, level));
            p.sendActionBar(Component.text("Riposte siap selama 4 detik", NamedTextColor.GOLD));
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void riposte(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player p) || !(event.getEntity() instanceof LivingEntity)
                || event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        Charge charge = charges.get(p.getUniqueId());
        if (charge == null) return;
        if (charge.expires < System.currentTimeMillis()) { charges.remove(p.getUniqueId()); return; }
        if (!plugin.waveEnabled("riposte") || p.getAttackCooldown() < .9f || event.getFinalDamage() <= 0 || blocked(event)) return;
        charges.remove(p.getUniqueId());
        event.setDamage(event.getDamage() + charge.level * .5);
    }
    private int reserve(ItemStack item) { return level(item, "careful_hands"); }
    private int maxWear(ItemStack item, Damageable meta) { return meta.hasMaxDamage() ? meta.getMaxDamage() : item.getType().getMaxDurability(); }
    private boolean worn(ItemStack item) {
        int level = reserve(item);
        return level > 0 && item.getItemMeta() instanceof Damageable d && !d.isUnbreakable()
                && maxWear(item, d) > 0 && maxWear(item, d) - d.getDamage() <= level;
    }
    private void warn(Player p) {
        if (cooldown(p, "careful_hands_warning", 40)) p.sendActionBar(Component.text("Careful Hands: perbaiki alat sebelum dipakai lagi.", NamedTextColor.YELLOW));
    }
    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
    public void preventBreak(BlockBreakEvent event) {
        if (worn(event.getPlayer().getInventory().getItemInMainHand())) { event.setCancelled(true); warn(event.getPlayer()); }
    }
    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
    public void preventToolAction(PlayerInteractEvent event) {
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK && worn(event.getItem())) {
            event.setCancelled(true); warn(event.getPlayer());
        }
    }
    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
    public void preventBrokenWeapon(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p && worn(p.getInventory().getItemInMainHand())) { event.setCancelled(true); warn(p); }
    }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void durability(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem(); int level = reserve(item);
        if (level == 0 || !(item.getItemMeta() instanceof Damageable d) || d.isUnbreakable()) return;
        int max = maxWear(item, d); if (max <= 0) return;
        int allowed = WaveOneRules.allowedWear(max, d.getDamage(), event.getDamage(), level);
        if (allowed < event.getDamage()) { event.setDamage(allowed); warn(event.getPlayer()); }
    }
    private ItemStack rod(Player p) {
        return p.getInventory().getItemInMainHand().getType() == Material.FISHING_ROD
                ? p.getInventory().getItemInMainHand() : p.getInventory().getItemInOffHand();
    }
    double patienceBonus(Player p) {
        if (p == null || !plugin.getConfig().getBoolean("fishing.enabled", true)) return 0;
        return WaveOneRules.patienceBonus(p.getPersistentDataContainer().getOrDefault(key("misses"), PersistentDataType.INTEGER, 0), level(rod(p), "patient_angler"));
    }
    void recordCatch(Player p, boolean rare) {
        if (p == null || level(rod(p), "patient_angler") == 0 || !plugin.getConfig().getBoolean("fishing.enabled", true)) return;
        if (!cooldown(p, "patient_catch", 40)) return;
        int misses = p.getPersistentDataContainer().getOrDefault(key("misses"), PersistentDataType.INTEGER, 0);
        p.getPersistentDataContainer().set(key("misses"), PersistentDataType.INTEGER, rare ? 0 : Math.clamp(misses + 1, 0, 100));
    }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void fishing(PlayerFishEvent event) {
        Player p = event.getPlayer();
        if (event.getState() == PlayerFishEvent.State.FISHING) {
            casts.put(p.getUniqueId(), new Cast(event.getHook().getUniqueId(), level(rod(p), "patient_angler"), System.currentTimeMillis()));
            return;
        }
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Cast cast = casts.remove(p.getUniqueId());
        // Suite removes the vanilla item before starting its minigame; its success hook records the catch instead.
        if (cast == null || cast.level <= 0 || !cast.hook.equals(event.getHook().getUniqueId())
                || System.currentTimeMillis() - cast.started > 600000 || !(event.getCaught() instanceof Item item) || item.isDead()
                || !plugin.getConfig().getBoolean("fishing.enabled", true)) return;
        Material type = item.getItemStack().getType();
        boolean ordinaryFish = type == Material.COD || type == Material.SALMON || type == Material.TROPICAL_FISH || type == Material.PUFFERFISH;
        boolean treasure = type == Material.ENCHANTED_BOOK || type == Material.BOW || type == Material.FISHING_ROD
                || type == Material.NAME_TAG || type == Material.NAUTILUS_SHELL || type == Material.SADDLE;
        // Replace ONE vanilla fish; never duplicate drops. 5% vanilla treasure baseline * max 10% relative bonus.
        if (ordinaryFish && event.getHook().isInOpenWater() && ThreadLocalRandom.current().nextDouble() < .05 * patienceBonus(p)) {
            Collection<ItemStack> loot = LootTables.FISHING_TREASURE.getLootTable().populateLoot(ThreadLocalRandom.current(),
                    new LootContext.Builder(event.getHook().getLocation()).lootedEntity(event.getHook()).killer(p).luck(0).build());
            if (!loot.isEmpty()) { item.setItemStack(loot.iterator().next().clone()); treasure = true; }
        }
        final boolean rare = treasure;
        plugin.getServer().getScheduler().runTask(plugin, () -> { if (!event.isCancelled() && p.isOnline()) recordCatch(p, rare); });
    }
    @EventHandler public void quit(PlayerQuitEvent event) { charges.remove(event.getPlayer().getUniqueId()); casts.remove(event.getPlayer().getUniqueId()); }
}
