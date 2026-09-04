package id.velioragardens.enchant;

import org.bukkit.*;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Bounded, event-driven effects. Never scans chunks or duplicates resource drops. */
final class WaveTwoEnchants implements Listener {
    private final VelioraEnchantPlugin plugin;
    private final Map<UUID, Long> combat = new HashMap<>();
    private final Map<UUID, Integer> mined = new HashMap<>();
    private final Set<UUID> adjusting = new HashSet<>();
    WaveTwoEnchants(VelioraEnchantPlugin plugin) { this.plugin = plugin; }
    void clear() { combat.clear(); mined.clear(); adjusting.clear(); }
    private NamespacedKey key(String id) { return new NamespacedKey(plugin, "wave2_" + id); }
    private int level(ItemStack item, String id) { return plugin.waveLevel(item, id); }
    private boolean ready(Player p, String id, int ticks) {
        long now = System.currentTimeMillis();
        if (p.getPersistentDataContainer().getOrDefault(key(id), PersistentDataType.LONG, 0L) > now) return false;
        int configured = plugin.getConfig().getInt("custom-enchants." + id + ".cooldown-ticks", -1);
        p.getPersistentDataContainer().set(key(id), PersistentDataType.LONG, now + Math.clamp(configured < 0 ? ticks : configured, 20, 72000) * 50L);
        return true;
    }
    private void potion(LivingEntity entity, PotionEffectType type, int ticks) {
        // Do not replace a stronger/longer effect supplied by another plugin.
        PotionEffect old = entity.getPotionEffect(type);
        if (old == null || (old.getAmplifier() == 0 && !old.isInfinite() && old.getDuration() < ticks))
            entity.addPotionEffect(new PotionEffect(type, ticks, 0, true, false, true));
    }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void fire(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (e.getCause() != EntityDamageEvent.DamageCause.FIRE && e.getCause() != EntityDamageEvent.DamageCause.FIRE_TICK
                && e.getCause() != EntityDamageEvent.DamageCause.LAVA && e.getCause() != EntityDamageEvent.DamageCause.HOT_FLOOR) return;
        int l = level(p.getInventory().getChestplate(), "emberguard");
        if (l > 0) e.setDamage(e.getDamage() * (1 - .05 * l));
    }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void fall(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p) || e.getCause() != EntityDamageEvent.DamageCause.FALL || e.getFinalDamage() < 2) return;
        int l = level(p.getInventory().getBoots(), "soft_landing");
        if (l == 0) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!e.isCancelled() && p.isOnline() && !p.isDead() && level(p.getInventory().getBoots(), "soft_landing") > 0 && ready(p,"soft_landing",200)) potion(p,PotionEffectType.SPEED,20 + 20*l);
        });
    }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void path(PlayerMoveEvent e) {
        Player p=e.getPlayer();
        if (e.getTo() == null || !e.hasChangedPosition() || combat.getOrDefault(p.getUniqueId(),0L)>System.currentTimeMillis()) return;
        int l=level(p.getInventory().getBoots(),"trailblazer");
        if(l>0 && p.getLocation().subtract(0,.2,0).getBlock().getType()==Material.DIRT_PATH && ready(p,"trailblazer",20)) potion(p,PotionEffectType.SPEED,20+10*l);
    }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void clarity(EntityPotionEffectEvent e) {
        if (!(e.getEntity() instanceof Player p) || adjusting.contains(p.getUniqueId()) || e.getNewEffect()==null) return;
        PotionEffect effect=e.getNewEffect();
        if (effect.getType()!=PotionEffectType.BLINDNESS && effect.getType()!=PotionEffectType.DARKNESS) return;
        int l=level(p.getInventory().getHelmet(),"clear_mind");
        if(l==0 || effect.isInfinite() || effect.getDuration()<=20) return;
        plugin.getServer().getScheduler().runTask(plugin,()->{
            if(e.isCancelled() || !p.isOnline() || level(p.getInventory().getHelmet(),"clear_mind")==0) return;
            PotionEffect current=p.getPotionEffect(effect.getType());
            if(current==null || current.getAmplifier()!=effect.getAmplifier() || current.isInfinite() || current.getDuration()>effect.getDuration() || !ready(p,"clear_mind",600)) return;
            adjusting.add(p.getUniqueId());
            try {
                p.removePotionEffect(current.getType());
                p.addPotionEffect(new PotionEffect(current.getType(),Math.max(20,(int)(current.getDuration()*(1-.15*l))),current.getAmplifier(),current.isAmbient(),current.hasParticles(),current.hasIcon()));
            } finally { adjusting.remove(p.getUniqueId()); }
        });
    }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void hit(EntityDamageByEntityEvent e) {
        if(ExpansionEnchants.secondary || ExpansionEnchants.generated(e))return;
        if(e.getFinalDamage()<=0 || !(e.getEntity() instanceof LivingEntity target)) return;
        Player p=e.getDamager() instanceof Player direct ? direct : e.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter ? shooter : null;
        if(e.getEntity() instanceof Player victim) combat.put(victim.getUniqueId(),System.currentTimeMillis()+10000);
        if(p==null) return;
        combat.put(p.getUniqueId(),System.currentTimeMillis()+10000);
        if(e.getCause()==EntityDamageEvent.DamageCause.ENTITY_ATTACK && e.getDamager()==p && p.getAttackCooldown()>=.9f) {
            int l=level(p.getInventory().getItemInMainHand(),"pursuit");
            if(l>0 && ready(p,"pursuit",200)) potion(p,PotionEffectType.SPEED,20+20*l);
            int tidal=level(p.getInventory().getItemInMainHand(),"tidal_stride");
            if(tidal>0 && p.isInWater() && ready(p,"tidal_stride",200)) potion(p,PotionEffectType.DOLPHINS_GRACE,20*tidal);
        }
        if(e.getDamager() instanceof Projectile projectile) {
            int l=projectile.getPersistentDataContainer().getOrDefault(key("crippling_shot"),PersistentDataType.INTEGER,0);
            if(l>0 && plugin.waveEnabled("crippling_shot") && ThreadLocalRandom.current().nextDouble()<.10*Math.min(2,l) && ready(p,"crippling_shot",160)) potion(target,PotionEffectType.SLOWNESS,20+20*Math.min(2,l));
            int tidal=projectile.getPersistentDataContainer().getOrDefault(key("tidal_stride"),PersistentDataType.INTEGER,0);
            if(tidal>0 && plugin.waveEnabled("tidal_stride") && p.isInWater() && ready(p,"tidal_stride",200)) potion(p,PotionEffectType.DOLPHINS_GRACE,20*Math.min(3,tidal));
        }
    }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void shot(EntityShootBowEvent e) {
        if(!(e.getEntity() instanceof Player p)) return;
        int l=level(e.getBow(),"crippling_shot");
        if(l>0 && e.getForce()>=.9f) e.getProjectile().getPersistentDataContainer().set(key("crippling_shot"),PersistentDataType.INTEGER,l);
        int recoil=level(e.getBow(),"recoil_step");
        if(recoil>0 && p.isSneaking() && p.isOnGround() && !p.isInsideVehicle() && ready(p,"recoil_step",200)) {
            var backward=p.getLocation().getDirection().setY(0);
            if(backward.lengthSquared()>.001) p.setVelocity(backward.normalize().multiply(-.15*recoil).setY(p.getVelocity().getY()));
        }
    }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void trident(ProjectileLaunchEvent e) {
        if(e.getEntity() instanceof Trident trident) {
            int l=level(trident.getItemStack(),"tidal_stride");
            if(l>0) trident.getPersistentDataContainer().set(key("tidal_stride"),PersistentDataType.INTEGER,l);
        }
    }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void mine(BlockBreakEvent e) {
        Player p=e.getPlayer();
        if(p.getGameMode()!=GameMode.SURVIVAL || !e.isDropItems()) return;
        int l=level(p.getInventory().getItemInMainHand(),"measured_work");
        if(l>0 && ready(p,"measured_count",20)) {
            int count=mined.merge(p.getUniqueId(),1,Integer::sum);
            if(count>=12) { mined.remove(p.getUniqueId()); if(ready(p,"measured_work",300)) potion(p,PotionEffectType.HASTE,40+20*l); }
        }
        int crop=level(p.getInventory().getItemInMainHand(),"cultivator");
        if(crop>0 && e.getBlock().getBlockData() instanceof Ageable age && age.getAge()==age.getMaximumAge()
                && Set.of(Material.WHEAT,Material.CARROTS,Material.POTATOES,Material.BEETROOTS).contains(e.getBlock().getType())
                && p.getFoodLevel()<20 && ThreadLocalRandom.current().nextDouble()<.1*crop && ready(p,"cultivator",200)) p.setFoodLevel(Math.min(20,p.getFoodLevel()+1));
    }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void shear(PlayerItemDamageEvent e) {
        // Covers shears durability uses, including wool, leaves and shearing. No extra drops.
        int l=level(e.getItem(),"gentle_shear");
        if(l>0 && ThreadLocalRandom.current().nextDouble()<.1*l) e.setCancelled(true);
    }
    double fishingBonus(Player p,String id) {
        if(p==null || !plugin.getConfig().getBoolean("fishing.enabled",true)) return 0;
        ItemStack rod=p.getInventory().getItemInMainHand().getType()==Material.FISHING_ROD ? p.getInventory().getItemInMainHand() : p.getInventory().getItemInOffHand();
        int l=level(rod,id);
        String biome=p.getLocation().getBlock().getBiome().getKey().getKey();
        if(id.equals("deepwater_pact") && !(biome.startsWith("deep_") && biome.endsWith("ocean"))) return 0;
        return switch(id) { case "deepwater_pact" -> .02*l; case "relic_seeker" -> .03*l; case "secret_whisper" -> .01*l; default -> 0; };
    }
    @EventHandler public void quit(PlayerQuitEvent e) { combat.remove(e.getPlayer().getUniqueId()); mined.remove(e.getPlayer().getUniqueId()); adjusting.remove(e.getPlayer().getUniqueId()); }
}
