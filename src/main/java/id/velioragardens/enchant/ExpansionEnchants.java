package id.velioragardens.enchant;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** No area ticks, block explosions or unbounded entity spawns. */
final class ExpansionEnchants implements Listener {
    static boolean secondary;
    static boolean generated(EntityDamageByEntityEvent e) {
        return e.getDamager().getPersistentDataContainer().has(NamespacedKey.fromString("velioraenchant:exp_secondary"),PersistentDataType.BYTE);
    }
    private final VelioraEnchantPlugin plugin;
    private final Set<Arrow> arrows=new HashSet<>();
    private LivingEntity checkingTarget;
    private boolean acceptedDamage;
    ExpansionEnchants(VelioraEnchantPlugin p) { plugin=p; }
    void clear() { arrows.forEach(Entity::remove); arrows.clear(); }
    private NamespacedKey key(String s) { return new NamespacedKey(plugin,"exp_"+s); }
    private int level(ItemStack i,String id) { return plugin.waveLevel(i,id); }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void haste(org.bukkit.event.block.BlockBreakEvent e) {
        int l=level(e.getPlayer().getInventory().getItemInMainHand(),"haste_tool");
        if(l>0 && e.getPlayer().getGameMode()==GameMode.SURVIVAL && ready(e.getPlayer(),"haste_tool",200))effect(e.getPlayer(),PotionEffectType.HASTE,20*l);
    }
    private int armor(Player p,String id) {
        int l=0; for(ItemStack i:p.getInventory().getArmorContents()) l=Math.max(l,level(i,id)); return l;
    }
    private boolean ready(Player p,String id,int ticks) {
        long now=System.currentTimeMillis();
        if(p.getPersistentDataContainer().getOrDefault(key(id),PersistentDataType.LONG,0L)>now) return false;
        int c=plugin.getConfig().getInt("custom-enchants."+id+".cooldown-ticks",-1);
        p.getPersistentDataContainer().set(key(id),PersistentDataType.LONG,now+50L*Math.max(ticks,c));
        return true;
    }
    private void effect(LivingEntity e,PotionEffectType type,int ticks) {
        PotionEffect old=e.getPotionEffect(type);
        if(old==null) e.addPotionEffect(new PotionEffect(type,ticks,0,true,false,true));
    }
    private boolean damage(LivingEntity target,Player source,double amount) {
        if(amount<=0 || !target.isValid() || target.isDead() || target==source) return false;
        LivingEntity previousTarget=checkingTarget; boolean previousAccepted=acceptedDamage;
        checkingTarget=target; acceptedDamage=false;
        boolean previous=secondary; secondary=true;
        try { target.damage(Math.min(6,amount),source); return acceptedDamage; }
        finally { secondary=previous; checkingTarget=previousTarget; acceptedDamage=previousAccepted; }
    }
    private void area(Player p,LivingEntity center,double damage) {
        int count=0;
        for(Entity e:center.getNearbyEntities(3,3,3)) {
            if(count>=3) break;
            if(e instanceof LivingEntity l && l!=p && !(l instanceof ArmorStand) && p.hasLineOfSight(l)) { damage(l,p,damage); count++; }
        }
    }
    private int weapon(Player p,Entity damager,String id) {
        if(damager instanceof Projectile projectile)
            return plugin.waveEnabled(id)?Math.clamp(projectile.getPersistentDataContainer().getOrDefault(key(id),PersistentDataType.INTEGER,0),0,ExpansionRules.SPECS.get(id).max()):0;
        return level(p.getInventory().getItemInMainHand(),id);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void modifiers(EntityDamageEvent e) {
        if(secondary) return;
        if(e instanceof EntityDamageByEntityEvent hit && generated(hit))return;
        if(e.getEntity() instanceof Player p) {
            if(e.getCause()==EntityDamageEvent.DamageCause.VOID || e.getCause()==EntityDamageEvent.DamageCause.SUICIDE) return;
            if(e.getCause()==EntityDamageEvent.DamageCause.FALL) {
                int l=armor(p,"feather_step");
                if(l>0 && ThreadLocalRandom.current().nextDouble()<.05*l && ready(p,"feather_step",400)) {e.setCancelled(true);return;}
            }
            String affinity=p.getWorld().getEnvironment()==World.Environment.NETHER?"nether_affinity":p.getWorld().getEnvironment()==World.Environment.THE_END?"end_affinity":"";
            int l=affinity.isEmpty()?0:armor(p,affinity);
            if(l>0) e.setDamage(e.getDamage()*(1-.03*l));
        }
        if(!(e instanceof EntityDamageByEntityEvent hit) || !(e.getEntity() instanceof LivingEntity target)) return;
        Player p=hit.getDamager() instanceof Player direct?direct:hit.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter?shooter:null;
        if(p==null) return;
        if(e.getEntity() instanceof Player victim && armor(victim,"aura")>0) {
            // Uses nearby player query only on damage, not a repeating world scan.
            if(victim.getNearbyEntities(3,3,3).stream().anyMatch(x->x instanceof Player && x!=p))
                e.setDamage(e.getDamage()*(1-.03*armor(victim,"aura")));
        }
        double bonus=0;
        String type=target.getType().name();
        Map<String,Boolean> conditions=Map.ofEntries(
            Map.entry("blaze_reaper",Set.of("BLAZE","GHAST","MAGMA_CUBE","PIGLIN","PIGLIN_BRUTE","HOGLIN","ZOGLIN","ZOMBIFIED_PIGLIN","WITHER_SKELETON").contains(type)),
            Map.entry("brightness",type.equals("WARDEN") && target.getLocation().getBlock().getLightLevel()<8),
            Map.entry("cubism",target instanceof Slime),
            Map.entry("enderbane",type.equals("ENDERMAN")||type.equals("ENDER_DRAGON")),
            Map.entry("finishing",target.getHealth()<target.getMaxHealth()*.25),
            Map.entry("first_strike",target.getHealth()>=target.getMaxHealth()),
            Map.entry("incinerate",target instanceof Spider),
            Map.entry("ninja",p.isSneaking()),
            Map.entry("shura",p.getHealth()<p.getMaxHealth()*.5 && p.getFallDistance()>0 && !p.isOnGround() && !p.isInWater()),
            Map.entry("skullcrusher",target instanceof AbstractSkeleton),
            Map.entry("zombie_crusher",target instanceof Zombie));
        for(var c:conditions.entrySet()) if(c.getValue()) bonus+=weapon(p,hit.getDamager(),c.getKey())*.05;
        e.setDamage(e.getDamage()*(1+Math.min(.30,bonus)));
        int twice=weapon(p,hit.getDamager(),"double_blow");
        if(twice>0 && ThreadLocalRandom.current().nextDouble()<.05*twice && ready(p,"double_blow",300)) e.setDamage(e.getDamage()*2);
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void hit(EntityDamageByEntityEvent e) {
        if(checkingTarget==e.getEntity() && e.getFinalDamage()>0)acceptedDamage=true;
        if(generated(e))return;
        if(secondary || e.getFinalDamage()<=0 || !(e.getEntity() instanceof LivingEntity target)) return;
        Player p=e.getDamager() instanceof Player direct?direct:e.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter?shooter:null;
        if(p!=null) {
            for(String id:List.of("abrasion","arctic_freeze","caffeinated","carve","contagion","explosive","ravenous","repel","starvation","thor")) {
                int l=weapon(p,e.getDamager(),id); if(l==0) continue;
                if(id.equals("caffeinated")&&!p.isSprinting()) continue;
                int cd=Set.of("contagion","explosive","thor","caffeinated").contains(id)?300:id.equals("ravenous")?400-40*(l-1):200;
                if(!ready(p,id,cd)) continue;
                switch(id) {
                    case "abrasion" -> { if(target instanceof Player victim) {
                        ItemStack[] contents=victim.getInventory().getArmorContents();
                        for(ItemStack item:contents) if(item!=null && item.getItemMeta() instanceof Damageable meta && !meta.isUnbreakable()) {
                            int max=meta.hasMaxDamage()?meta.getMaxDamage():item.getType().getMaxDurability();
                            meta.setDamage(Math.min(Math.max(0,max-1),meta.getDamage()+l)); item.setItemMeta(meta);
                        }
                        victim.getInventory().setArmorContents(contents);
                    }}
                    case "arctic_freeze" -> {
                        effect(target,PotionEffectType.SLOWNESS,20*l);
                        plugin.getServer().getScheduler().runTaskLater(plugin,()->{
                            if(p.isOnline() && target.isValid() && p.getWorld()==target.getWorld() && p.getLocation().distanceSquared(target.getLocation())<=256) damage(target,p,.5*l);
                        },20);
                    }
                    case "caffeinated" -> effect(p,PotionEffectType.HASTE,20*l);
                    case "carve","contagion","explosive" -> {
                        area(p,target,Math.min(3,l));
                        target.getWorld().spawnParticle(id.equals("explosive")?Particle.EXPLOSION:Particle.CLOUD,target.getLocation(),6,.4,.4,.4,0);
                    }
                    case "ravenous" -> p.setFoodLevel(Math.min(20,p.getFoodLevel()+1));
                    case "repel" -> {
                        var v=target.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
                        if(v.lengthSquared()>.001) target.setVelocity(v.normalize().multiply(.15*l).setY(target.getVelocity().getY()));
                    }
                    case "starvation" -> effect(target,PotionEffectType.HUNGER,20*l);
                    case "thor" -> {
                        // Visual only: cannot ignite blocks or transform mobs.
                        target.getWorld().strikeLightningEffect(target.getLocation());
                        damage(target,p,Math.min(3,l));
                    }
                }
            }
        }
        if(target instanceof Player victim) {
            if(!(e.getDamager() instanceof Player) && !(e.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player)) {
                int a=armor(victim,"adrenaline"); if(a>0 && ready(victim,"adrenaline",400)) effect(victim,PotionEffectType.STRENGTH,20*a);
            }
            for(String id:List.of("escape","getaway")) {
                int l=armor(victim,id);
                if(l>0 && (!id.equals("getaway") || victim.getHealth()-e.getFinalDamage()<=victim.getMaxHealth()*.20)
                    && victim.getHealth()>e.getFinalDamage() && ready(victim,id,400)) effect(victim,PotionEffectType.SPEED,20*l);
            }
            LivingEntity attacker=e.getDamager() instanceof LivingEntity living?living:e.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity living?living:null;
            if(attacker!=null && attacker!=victim) {
                int reflect=Math.max(armor(victim,"rebounding"),armor(victim,"resonate"));
                if(reflect>0 && ready(victim,"reflection",Math.max(300,Math.max(plugin.getConfig().getInt("custom-enchants.rebounding.cooldown-ticks",300),plugin.getConfig().getInt("custom-enchants.resonate.cooldown-ticks",300))))) damage(attacker,victim,ExpansionRules.reflection(e.getFinalDamage(),reflect));
                int scorch=armor(victim,"scorching"); if(scorch>0 && ready(victim,"scorching",200)) attacker.setFireTicks(Math.max(attacker.getFireTicks(),20*scorch));
                int rumble=armor(victim,"rumble"); if(rumble>0 && ready(victim,"rumble",300)) area(victim,victim,rumble);
            }
        }
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void shoot(EntityShootBowEvent e) {
        if(!(e.getEntity() instanceof Player p)) return;
        for(String id:List.of("cubism","thor","contagion","explosive")) {
            int l=level(e.getBow(),id); if(l>0)e.getProjectile().getPersistentDataContainer().set(key(id),PersistentDataType.INTEGER,l);
        }
        int l=level(e.getBow(),"multi_shot");
        if(l==0 || e.getForce()<.9 || arrows.size()>=96 || !ready(p,"multi_shot",300)) return;
        for(int i=0;i<l;i++) {
            Arrow arrow=p.getWorld().spawnArrow(p.getEyeLocation(),p.getLocation().getDirection(),2f,6f);
            arrow.setShooter(p); arrow.setDamage(1); arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            arrow.getPersistentDataContainer().set(key("secondary"),PersistentDataType.BYTE,(byte)1);
            arrows.add(arrow);
            plugin.getServer().getScheduler().runTaskLater(plugin,()->{arrow.remove();arrows.remove(arrow);},60);
        }
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void trident(ProjectileLaunchEvent e) {
        if(e.getEntity() instanceof Trident t) for(String id:List.of("thor","double_blow")) {
            int l=level(t.getItemStack(),id);if(l>0)t.getPersistentDataContainer().set(key(id),PersistentDataType.INTEGER,l);
        }
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void hook(PlayerFishEvent e) {
        if(e.getState()!=PlayerFishEvent.State.CAUGHT_ENTITY || !(e.getCaught() instanceof LivingEntity target)) return;
        Player p=e.getPlayer(); ItemStack rod=p.getInventory().getItemInMainHand();
        if(rod.getType()!=Material.FISHING_ROD)rod=p.getInventory().getItemInOffHand();
        int sharp=level(rod,"sharpness_hook"), fire=level(rod,"fire_hook"), poison=level(rod,"poisoned_hook");
        if(sharp+fire+poison==0 || !ready(p,"combat_hook",200))return;
        // Route through cancellable damage before applying debuffs (claim/PvP plugins may deny it).
        if(!damage(target,p,Math.max(.01,.25*sharp)))return;
        if(fire>0)target.setFireTicks(Math.max(target.getFireTicks(),20*fire));
        if(poison>0)effect(target,PotionEffectType.POISON,20*poison);
    }
    @EventHandler(priority=EventPriority.MONITOR)
    public void interact(PlayerInteractEvent e) {
        if(e.useItemInHand()==Event.Result.DENY)return;
        if(e.getHand()!=org.bukkit.inventory.EquipmentSlot.HAND || !e.getAction().isRightClick())return;
        Player p=e.getPlayer(); String id=p.isSneaking()?"charge":"ascend"; int l=level(e.getItem(),id);
        if(l==0 || p.isInsideVehicle() || p.isFlying() || !p.isOnGround() || !ready(p,id,id.equals("charge")?300:600))return;
        if(id.equals("ascend"))effect(p,PotionEffectType.LEVITATION,20*l);
        else { var v=p.getLocation().getDirection().setY(0); if(v.lengthSquared()>.001)p.setVelocity(v.normalize().multiply(.35*l)); }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void postpone(io.papermc.paper.event.entity.EntityKnockbackEvent e) {
        if(secondary || !(e.getEntity() instanceof Mob mob))return;
        if(!(mob.getLastDamageCause() instanceof EntityDamageByEntityEvent hit) || !(hit.getDamager() instanceof Player p))return;
        if(e.getCause()!=io.papermc.paper.event.entity.EntityKnockbackEvent.Cause.ENTITY_ATTACK)return;
        int l=level(p.getInventory().getItemInMainHand(),"postpone");
        if(l>0 && ThreadLocalRandom.current().nextDouble()<.1*l && ready(p,"postpone",100)) e.setKnockback(e.getKnockback().multiply(0));
    }
}
