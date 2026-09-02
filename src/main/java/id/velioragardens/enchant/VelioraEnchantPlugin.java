/*
 * VelioraEnchant - maintained for Veliora Gardens.
 * Clean Paper implementation inspired by the supplied VelioraCustomEnchant V1/V2 sources.
 */
package id.velioragardens.enchant;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.block.data.Ageable;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class VelioraEnchantPlugin extends JavaPlugin implements Listener, TabExecutor {
    private NamespacedKey customKey;
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> windUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Long> repairReady = new ConcurrentHashMap<>();
    private final Set<Location> veinBreaking = ConcurrentHashMap.newKeySet();
    private final Map<String, Enchantment> overlevel = new LinkedHashMap<>();
    private boolean excellentEnchantsPresent;
    private DistributionPolicy distributionPolicy;
    private NamespacedKey villagerTradeMarkerKey;
    private static final Component FISHING_MENU_TITLE=Component.text("Veliora Fishing Enchants",NamedTextColor.AQUA);

    @Override public void onEnable() {
        saveDefaultConfig();
        excellentEnchantsPresent = getServer().getPluginManager().isPluginEnabled("ExcellentEnchants");
        boolean suitePresent = getServer().getPluginManager().isPluginEnabled("VelioraSuite");
        for (LegacyEnchant enchant : LegacyEnchant.values()) {
            String path = "custom-enchants." + enchant.id();
            getConfig().addDefault(path + ".enabled", true);
            getConfig().addDefault(path + ".max-level", defaultMaxLevel(enchant));
            getConfig().addDefault(path + ".cooldown-ticks", -1);
        }
        getConfig().options().copyDefaults(true);
        if (getConfig().getInt("config-version",1) < 2) {
            getConfig().set("distribution.structure-loot.enabled",true);
            getConfig().set("distribution.librarian.enabled",true);
            getConfig().set("distribution.fisherman.enabled",true);
            getConfig().set("config-version",2);
        }
        migrateLegacyId("lifesteal", "life_steal");
        migrateLegacyId("autosmelt", "auto_smelt");
        getConfig().set("config-version",3);
        saveConfig();
        distributionPolicy=new DistributionPolicy(getConfig().getConfigurationSection("distribution"));
        customKey = new NamespacedKey(this, "custom_enchant");
        villagerTradeMarkerKey = new NamespacedKey(this,"custom_trade_added");
        overlevel.put("sharpness", Enchantment.SHARPNESS);
        overlevel.put("impaling", Enchantment.IMPALING);
        overlevel.put("protection", Enchantment.PROTECTION);
        overlevel.put("fire_protection", Enchantment.FIRE_PROTECTION);
        overlevel.put("blast_protection", Enchantment.BLAST_PROTECTION);
        overlevel.put("projectile_protection", Enchantment.PROJECTILE_PROTECTION);
        overlevel.put("thorns", Enchantment.THORNS);
        overlevel.put("density", Enchantment.DENSITY);
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("velioraenchant")).setExecutor(this);
        Objects.requireNonNull(getCommand("velioraenchant")).setTabCompleter(this);
        getServer().getScheduler().runTaskTimer(this, this::applyPassiveEffects, 200L, 200L);
        if (excellentEnchantsPresent && getConfig().getBoolean("excellent-enchants-bridge.enabled", true)) getLogger().info("ExcellentEnchants detected: duplicate Veliora effects will be suppressed per item.");
        getLogger().info("Fishing rod enchant engine enabled" + (suitePresent ? " with optional VelioraSuite hook available." : " without VelioraSuite dependency."));
    }
    @Override public void onDisable() { cooldowns.clear(); windUntil.clear(); repairReady.clear(); veinBreaking.clear(); }

    @EventHandler(ignoreCancelled = true)
    public void onAnvil(PrepareAnvilEvent event) {
        ItemStack left = event.getInventory().getItem(0);
        ItemStack right = event.getInventory().getItem(1);
        ItemStack result = event.getResult();
        if (left == null || right == null) return;
        boolean customBook = isCustomBook(right);
        if (result == null && customBook) result = left.clone();
        if (result == null) return;
        if (getConfig().getBoolean("overlevel.enabled", true)) {
            event.getView().bypassEnchantmentLevelRestriction(true);
            if (getConfig().getBoolean("overlevel.bypass-too-expensive", true)) event.getView().setMaximumRepairCost(Integer.MAX_VALUE);
        }
        ItemStack edited = result.clone(); boolean changed = applyCustomBook(left, right, edited);
        for (var entry : getConfig().getBoolean("overlevel.enabled", true) ? overlevel.entrySet() : Collections.<Map.Entry<String, Enchantment>>emptySet()) {
            Enchantment enchant = entry.getValue();
            int a = enchantLevel(left, enchant), b = enchantLevel(right, enchant);
            if (a == 0 && b == 0) continue;
            int target = Math.min(cap(entry.getKey(), enchant.getMaxLevel()), a == b ? a + 1 : Math.max(a, b));
            if (target > enchantLevel(edited, enchant)) { setEnchant(edited, enchant, target); changed = true; }
        }
        if (changed) {
            if (event.getView().getRepairCost() < 1) event.getView().setRepairCost(1);
            event.setResult(edited);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnchantingTable(EnchantItemEvent event) {
        if (!getConfig().getBoolean("distribution.enchanting-table.enabled", true)) return;
        if (event.getExpLevelCost() < getConfig().getInt("distribution.enchanting-table.minimum-level-cost", 15)) return;
        if (Math.random() >= getConfig().getDouble("distribution.enchanting-table.chance", .12)) return;
        Material material = event.getItem().getType();
        List<LegacyEnchant> candidates = Arrays.stream(LegacyEnchant.values())
            .filter(enchant -> enabled(enchant.id()))
            .filter(enchant -> material == Material.BOOK || material == Material.ENCHANTED_BOOK || enchant.accepts(material))
            .toList();
        if (candidates.isEmpty()) return;
        LegacyEnchant enchant = candidates.get(getRandom().nextInt(candidates.size()));
        int level = Math.min(defaultMaxLevel(enchant), Math.max(1, event.getExpLevelCost() / 15));
        applyCustomEnchant(event.getItem(), enchant.id(), level);
        event.getEnchanter().sendActionBar(Component.text("Custom enchant didapat: " + pretty(enchant.id()) + " " + roman(level), categoryColor(enchant.category())));
    }

    @EventHandler(ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof LivingEntity target)) return;
        if (player.equals(target) || player.getGameMode() == GameMode.SPECTATOR) return;
        ItemStack weapon = player.getInventory().getItemInMainHand();
        int lifesteal = customLevel(weapon, "life_steal");
        if (lifesteal > 0 && enabled("life_steal") && ready(player, "life_steal", getConfig().getLong("custom-enchants.life_steal.cooldown-ticks", 20))) {
            double heal = getConfig().getDouble("custom-enchants.life_steal.heal-per-level", .35) * lifesteal;
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + heal));
            player.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 1, 0), 2, .25, .3, .25, 0);
        }
        int bleed = customLevel(weapon, "bleed");
        if (bleed > 0 && ready(player, "bleed", 30)) target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * (1 + bleed), 0, true, false, true));
        int poison = customLevel(weapon, "poison");
        if (poison > 0 && ready(player, "poison", 30)) target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * (1 + poison), 0, true, false, true));
        int blind = Math.max(customLevel(weapon, "blind"), customLevel(weapon, "debuff"));
        if (blind > 0 && ready(player, "blind", 40)) target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * (1 + blind), 0, true, false, true));
        int burning = customLevel(weapon, "burning");
        if (burning > 0) target.setFireTicks(Math.max(target.getFireTicks(), 20 * (1 + burning)));
        int critical = customLevel(weapon, "critical");
        if (critical > 0 && Math.random() < Math.min(.45, critical * .08)) { event.setDamage(event.getDamage() * (1 + critical * .12)); player.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0,1,0), 12,.3,.4,.3,0); }
        int lightning = customLevel(weapon, "lightning");
        if (lightning > 0 && ready(player, "lightning", 80)) target.getWorld().strikeLightningEffect(target.getLocation());
        int freeze = customLevel(weapon, "freeze");
        if (freeze > 0 && ready(player, "freeze", 50)) target.setFreezeTicks(Math.min(140, target.getFreezeTicks() + freeze * 30));
        int knockback = Math.max(customLevel(weapon, "wind_strike"), customLevel(weapon, "sudden_blow"));
        if (knockback > 0 && ready(player, "wind_strike", 35)) target.setVelocity(target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(.25 + knockback * .12).setY(.18));
        int sharpen = Math.max(customLevel(weapon, "sharpen"), customLevel(weapon, "blast")); if (sharpen > 0) event.setDamage(event.getDamage() * (1 + sharpen * .06));
        int omnivamp = Math.max(customLevel(weapon, "omnivamp"), customLevel(weapon, "soul_eater")); if (omnivamp > 0 && ready(player,"omnivamp",25)) player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + event.getFinalDamage() * .03 * omnivamp));
        if (customLevel(weapon,"cobweb") > 0 && ready(player,"cobweb",60)) trapTarget(target);
        if (customLevel(weapon,"storm") > 0 && ready(player,"storm",100)) { target.getWorld().strikeLightningEffect(target.getLocation()); target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1)); }
        if (customLevel(weapon,"nulled") > 0 && ready(player,"nulled",70)) target.getActivePotionEffects().forEach(effect -> target.removePotionEffect(effect.getType()));
        int craving = customLevel(weapon, "craving");
        if (craving > 0 && target instanceof Player victim && ready(player,"craving",50)) victim.setFoodLevel(Math.max(0, victim.getFoodLevel() - craving));
        int emnity = customLevel(weapon, "emnity");
        if (emnity > 0 && ready(player,"emnity",45)) target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * (1 + emnity), Math.min(1, emnity - 1), true, false, true));
        int grimoire = customLevel(weapon, "grimoire");
        if (grimoire > 0 && ready(player,"grimoire",65)) castGrimoire(player, target, grimoire);
        int hail = customLevel(weapon, "hail_storm");
        if (hail > 0 && ready(player,"hail_storm",90)) castHail(target, hail);
        int illusion = customLevel(weapon, "illusion");
        if (illusion > 0 && ready(player,"illusion",80)) { player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * (1 + illusion), 0, true, false, true)); player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0,1,0), 24,.4,.6,.4,.1); }
        int implant = customLevel(weapon, "implant");
        if (implant > 0 && ready(player,"implant",40)) player.giveExp(implant * 2);
        int steal = customLevel(weapon, "steal");
        if (steal > 0 && target instanceof Player victim && ready(player,"steal",60)) { int amount=Math.min(steal, victim.getFoodLevel()); victim.setFoodLevel(victim.getFoodLevel()-amount); player.setFoodLevel(Math.min(20,player.getFoodLevel()+amount)); }
        int frozenHook=customLevel(weapon,"frozen_hook"); if(frozenHook>0&&ready(player,"frozen_hook",45)){target.setFreezeTicks(Math.min(160,target.getFreezeTicks()+frozenHook*35));target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,30+frozenHook*10,0));}
        if (event.getFinalDamage() >= target.getHealth()) applyExcellentKillHeal(player, weapon, "vampire", "vampire");
        int wind = customLevel(weapon, "wind_burst");
        if (wind > 0 && enabled("wind_burst") && weapon.getType() == Material.MACE && ready(player, "wind_burst", getConfig().getLong("custom-enchants.wind_burst.cooldown-ticks", 220))) {
            windUntil.put(player.getUniqueId(), tick() + getConfig().getLong("custom-enchants.wind_burst.duration-ticks", 80));
            target.setVelocity(target.getVelocity().add(new Vector(0, .4 + wind * .08, 0)));
            player.getWorld().spawnParticle(Particle.GUST, player.getLocation().add(0, 1, 0), 10, .3, .3, .3, 0);
            player.playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1f, 1.15f);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL || !(event.getEntity() instanceof Player player)) return;
        if (windUntil.getOrDefault(player.getUniqueId(), 0L) > tick()) {
            int level = customLevel(player.getInventory().getItemInMainHand(), "wind_burst");
            event.setDamage(event.getDamage() * Math.max(.1, 1 - level * getConfig().getDouble("custom-enchants.wind_burst.fall-reduction-per-level", .10)));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLethalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getFinalDamage() < player.getHealth()) return;
        if (has(player, "phoenix") || has(player, "second_life") || has(player, "death_angel")) {
            String id = has(player, "phoenix") ? "phoenix" : has(player, "second_life") ? "second_life" : "death_angel";
            if (ready(player, id, 20L * 180)) { event.setCancelled(true); player.setHealth(Math.min(player.getMaxHealth(), 4 + highest(player, id))); player.setFireTicks(0); player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1, true, true, true)); player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0,1,0), 30,.35,.7,.35,.05); }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile projectile) || !(projectile.getShooter() instanceof Player player) || !(event.getEntity() instanceof LivingEntity target)) return;
        ItemStack bow = player.getInventory().getItemInMainHand().getType() == Material.BOW || player.getInventory().getItemInMainHand().getType() == Material.CROSSBOW ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();
        int blind = customLevel(bow, "blinding_arrow"); if (blind > 0 && ready(player,"blinding_arrow",35)) target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * blind, 0));
        int frost = customLevel(bow, "frost_arrow"); if (frost > 0 && ready(player,"frost_arrow",35)) target.setFreezeTicks(Math.min(140, target.getFreezeTicks() + frost * 30));
        int focus = customLevel(bow, "focus_fire"); if (focus > 0) event.setDamage(event.getDamage() * (1 + focus * .08));
        int frozenHook=customLevel(bow,"frozen_hook"); if(frozenHook>0&&ready(player,"frozen_hook",45)){target.setFreezeTicks(Math.min(160,target.getFreezeTicks()+frozenHook*35));target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,30+frozenHook*10,0));}
        if (event.getFinalDamage() >= target.getHealth()) applyExcellentKillHeal(player, bow, "vampiricarrows", "vampiric_arrows");
    }

    @EventHandler(ignoreCancelled = true)
    public void onShield(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player defender) || !defender.isBlocking()) return;
        ItemStack shield = defender.getInventory().getItemInOffHand();
        int level = customLevel(shield, "shield_resistance");
        if (level <= 0 || !enabled("shield_resistance")) return;
        if (!(event.getDamager() instanceof Player attacker) || !attacker.getInventory().getItemInMainHand().getType().name().endsWith("_AXE")) return;
        double chance = Math.min(.80, level * getConfig().getDouble("custom-enchants.shield_resistance.reduction-per-level", .10));
        if (Math.random() < chance) getServer().getScheduler().runTask(this, () -> defender.setCooldown(Material.SHIELD, 0));
    }
    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof FishingMenuHolder) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        double reduction = 0;
        reduction += highest(player,"protection") * .025;
        reduction += highest(player,"guarded") * .02;
        reduction += highest(player,"obsidian_plate") * .025;
        reduction += highest(player,"ductile") * .015;
        reduction += highest(player,"sturdy") * .015;
        reduction += highest(player,"block") * .02;
        reduction += highest(player,"force_shield") * .025;
        if (player.getHealth() / player.getMaxHealth() < .35) reduction += highest(player,"emergency_defence") * .04;
        if (reduction > 0) event.setDamage(event.getDamage() * Math.max(.35, 1 - Math.min(.55, reduction)));
        int thorns = highest(player,"poisonous_thorns");
        if (thorns > 0 && event instanceof EntityDamageByEntityEvent hit && hit.getDamager() instanceof LivingEntity attacker && ready(player,"poisonous_thorns",35)) attacker.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * thorns, 0));
    }

    @EventHandler(ignoreCancelled = true)
    public void onDurability(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem(); int unbreaking=Math.max(customLevel(item,"unbreaking"),customLevel(item,"ductile"));
        if (customLevel(item,"auto_repair") > 0) repairReady.put(event.getPlayer().getUniqueId(), tick());
        if (unbreaking > 0 && Math.random() < Math.min(.75, unbreaking * .12)) event.setCancelled(true);
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) { UUID id=event.getPlayer().getUniqueId(); cooldowns.keySet().removeIf(key->key.startsWith(id+":")); windUntil.remove(id); repairReady.remove(id); }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer(); ItemStack tool = player.getInventory().getItemInMainHand(); Block origin = event.getBlock();
        boolean smelted = customLevel(tool, "auto_smelt") > 0 && enabled("auto_smelt") && autosmelt(event, player, origin);
        if (!smelted && customLevel(tool, "telepathy") > 0 && enabled("telepathy")) telepathy(event, player, origin);
        if (customLevel(tool,"flower") > 0 && origin.getType() == Material.GRASS_BLOCK && ready(player,"flower",30)) origin.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, origin.getLocation().add(.5,1,.5), 8,.4,.2,.4,0);
        if (customLevel(tool,"auto_farm") > 0 && origin.getBlockData() instanceof Ageable crop && crop.getAge() >= crop.getMaximumAge()) replantCrop(origin);
        int treasure=customLevel(tool,"lucky_treasure"); if(treasure>0 && ready(player,"lucky_treasure",25) && Math.random()<Math.min(.20, treasure*.035)) { player.giveExp(treasure*3); player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,origin.getLocation().add(.5,.5,.5),10,.25,.25,.25,0); }
        int luck=customLevel(tool,"luck"); if(luck>0&&Math.random()<Math.min(.15,luck*.03)) event.setExpToDrop(event.getExpToDrop()+luck);
        int experience=Math.max(customLevel(tool,"experience"),customLevel(tool,"levels")); if(experience>0) event.setExpToDrop((int)Math.ceil(event.getExpToDrop()*(1+experience*.15)));
    }

    private boolean autosmelt(BlockBreakEvent event, Player player, Block block) {
        ItemStack drop = new ItemStack(block.getType());
        Material result = switch (block.getType()) {
            case IRON_ORE, DEEPSLATE_IRON_ORE -> Material.IRON_INGOT;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE -> Material.GOLD_INGOT;
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> Material.COPPER_INGOT;
            default -> null;
        };
        if (result == null) return false;
        event.setDropItems(false); player.getInventory().addItem(new ItemStack(result)).values().forEach(i -> player.getWorld().dropItemNaturally(block.getLocation(), i));
        return true;
    }

    private void telepathy(BlockBreakEvent event, Player player, Block block) {
        event.setDropItems(false);
        for (ItemStack item : block.getDrops(player.getInventory().getItemInMainHand(), player)) player.getInventory().addItem(item).values().forEach(i -> player.getWorld().dropItemNaturally(block.getLocation(), i));
    }

    private void mineVein(Player player, Block origin, ItemStack tool, int level) {
        int max = Math.min(64, level * getConfig().getInt("custom-enchants.vein.max-blocks-per-level", 12));
        Material type = origin.getType(); Queue<Block> queue = new ArrayDeque<>(); Set<Location> seen = new HashSet<>(); queue.add(origin);
        int broken = 0;
        while (!queue.isEmpty() && broken < max) {
            Block block = queue.remove(); if (!seen.add(block.getLocation())) continue;
            for (int x=-1;x<=1;x++) for (int y=-1;y<=1;y++) for (int z=-1;z<=1;z++) { Block near = block.getRelative(x,y,z); if (near.getType() == type && !seen.contains(near.getLocation())) queue.add(near); }
            if (block.equals(origin)) continue;
            veinBreaking.add(block.getLocation()); block.breakNaturally(tool, true); broken++;
        }
    }
    private void mineChunk(Player player, Block origin, ItemStack tool, int level) { int radius=Math.min(1,level); int remaining=8; for(int x=-radius;x<=radius&&remaining>0;x++) for(int z=-radius;z<=radius&&remaining>0;z++) { if(x==0&&z==0)continue; Block block=origin.getRelative(x,0,z); if(block.getType().isAir() || !block.getType().isBlock())continue; veinBreaking.add(block.getLocation()); block.breakNaturally(tool,true); remaining--; } }
    private void replantCrop(Block block) { Material type=block.getType(); getServer().getScheduler().runTaskLater(this,()->{ if(block.getType().isAir()) block.setType(type); },1L); }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) { Player player=event.getPlayer(); int pocket=highest(player,"food_pocket"); if(pocket>0) { player.setFoodLevel(Math.min(20,player.getFoodLevel()+pocket)); player.setSaturation(Math.min(20f,player.getSaturation()+pocket*.5f)); } }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (!getConfig().getBoolean("fishing.enabled", true)) return;
        Player player=event.getPlayer(); ItemStack rod=player.getInventory().getItemInMainHand().getType()==Material.FISHING_ROD ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();
        if (rod.getType()!=Material.FISHING_ROD) return;
        if (event.getState()==PlayerFishEvent.State.BITE && customLevel(rod,"auto_reel")>0 && ready(player,"auto_reel",10)) { player.sendActionBar(Component.text("✦ Ikan menggigit!",NamedTextColor.AQUA)); player.playSound(player.getLocation(),Sound.ENTITY_FISHING_BOBBER_SPLASH,.8f,1.5f); return; }
        if (event.getState()==PlayerFishEvent.State.CAUGHT_ENTITY && event.getCaught() instanceof LivingEntity caught) { int guardian=customLevel(rod,"guardian_hook"); if(guardian>0&&ready(player,"guardian_hook",45)){caught.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,20*(2+guardian),Math.min(2,guardian-1))); caught.getWorld().spawnParticle(Particle.BUBBLE,caught.getLocation().add(0,1,0),16,.3,.45,.3,.05);} return; }
        if (event.getState()!=PlayerFishEvent.State.CAUGHT_FISH || !(event.getCaught() instanceof Item caught)) return;
        int deep=customLevel(rod,"deep_hook"); if(deep>0) player.giveExp((int)Math.round(modifier("catch-xp",2,1,8).value(deep)));
        int heal=customLevel(rod,"fisherman_heal"); if(heal>0&&ready(player,"fisherman_heal",40)){player.setHealth(Math.min(player.getMaxHealth(),player.getHealth()+modifier("heal",2,1,6).value(heal))); player.getWorld().spawnParticle(Particle.HEART,player.getLocation().add(0,1,0),2,.2,.25,.2,0);}
        int chanceLevel=Math.max(customLevel(rod,"double_catch"),customLevel(rod,"angler_luck"));
        if(chanceLevel>0&&Math.random()<modifier("double-catch-chance",.08,.04,.20).value(chanceLevel)){ItemStack copy=caught.getItemStack().clone(); player.getInventory().addItem(copy).values().forEach(left->player.getWorld().dropItemNaturally(player.getLocation(),left)); player.playSound(player.getLocation(),Sound.ENTITY_EXPERIENCE_ORB_PICKUP,.45f,1.7f);}
        boolean ocean=isBiome(player,"OCEAN"), river=isBiome(player,"RIVER");
        int oceanLevel=customLevel(rod,"ocean_blessing"), riverLevel=customLevel(rod,"river_spirit"), treasure=customLevel(rod,"treasure_hook");
        if (river&&riverLevel>0) player.giveExp(2*riverLevel);
        double treasureChance=(treasure>0?modifier("treasure-chance",.03,.02,.10).value(treasure):0)+(ocean?oceanLevel*.015:0);
        if(treasureChance>0&&Math.random()<treasureChance){ItemStack bonus=new ItemStack(Material.NAUTILUS_SHELL,1); player.getInventory().addItem(bonus).values().forEach(left->player.getWorld().dropItemNaturally(player.getLocation(),left)); player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,player.getLocation().add(0,1,0),12,.3,.45,.3,.02);}
        int relic=customLevel(rod,"sunken_relic"); if(relic>0&&Math.random()<.002*relic) giveFragment(player,"Sunken Relic Fragment",NamedTextColor.GOLD);
        if(customLevel(rod,"storm_angler")>0&&player.getWorld().hasStorm()) player.giveExp(3);
        if(customLevel(rod,"mermaid_tears")>0&&ready(player,"mermaid_tears",80)) player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,60,0));
        int abyss=customLevel(rod,"abyssal_hook"); if(abyss>0&&player.getLocation().getY()<45&&Math.random()<.01*abyss) giveFragment(player,"Abyssal Fragment",NamedTextColor.DARK_PURPLE);
        int leviathan=customLevel(rod,"leviathan_line"); if(leviathan>0&&Math.random()<.025*leviathan){caught.getItemStack().setAmount(Math.min(caught.getItemStack().getMaxStackSize(),caught.getItemStack().getAmount()+1));player.playSound(player.getLocation(),Sound.ENTITY_ELDER_GUARDIAN_AMBIENT,.25f,1.6f);}
        if(customLevel(rod,"veliora_secret")>0&&Math.random()<getConfig().getDouble("fishing.secret-catch-chance",.0005)) giveFragment(player,"Veliora Secret Fragment",NamedTextColor.DARK_PURPLE);
    }
    @EventHandler(ignoreCancelled = true)
    public void onStructureLoot(LootGenerateEvent event) {
        if (!distributionPolicy.enabled(DistributionPolicy.Source.STRUCTURE_LOOT)) return;
        String table=event.getLootTable().getKey().toString();
        if (!distributionPolicy.lootTables().contains(table) || Math.random() >= distributionPolicy.chance(DistributionPolicy.Source.STRUCTURE_LOOT)) return;
        boolean alreadyInjected=event.getLoot().stream().anyMatch(item -> item.getItemMeta()!=null && item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(this,"distribution_book"),PersistentDataType.BYTE));
        if (alreadyInjected) return;
        ItemStack book=rollFishingBook(distributionPolicy.cap(DistributionPolicy.Source.STRUCTURE_LOOT));
        ItemMeta meta=book.getItemMeta(); meta.getPersistentDataContainer().set(new NamespacedKey(this,"distribution_book"),PersistentDataType.BYTE,(byte)1); book.setItemMeta(meta);
        event.getLoot().add(book);
    }
    @EventHandler(ignoreCancelled = true)
    public void onVillagerTrade(VillagerAcquireTradeEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (villager.getPersistentDataContainer().has(villagerTradeMarkerKey,PersistentDataType.BYTE)) return;
        if (villager.getProfession()==Villager.Profession.LIBRARIAN && villager.getVillagerLevel()>=4 && distributionPolicy.enabled(DistributionPolicy.Source.LIBRARIAN) && Math.random()<distributionPolicy.chance(DistributionPolicy.Source.LIBRARIAN)) {
            FishingRarity cap=villager.getVillagerLevel()>=5?distributionPolicy.cap(DistributionPolicy.Source.LIBRARIAN):FishingRarity.RARE;
            event.setRecipe(specialRecipe(rollLibrarianBook(cap),villager.getVillagerLevel()>=5?getConfig().getInt("distribution.librarian.master-price",48):getConfig().getInt("distribution.librarian.expert-price",32),Material.BOOK));
            villager.getPersistentDataContainer().set(villagerTradeMarkerKey,PersistentDataType.BYTE,(byte)1);
        }
        if (villager.getProfession()==Villager.Profession.FISHERMAN && villager.getVillagerLevel()>=5 && distributionPolicy.enabled(DistributionPolicy.Source.FISHERMAN) && Math.random()<distributionPolicy.chance(DistributionPolicy.Source.FISHERMAN)) {
            event.setRecipe(specialRecipe(rollFishingBook(distributionPolicy.cap(DistributionPolicy.Source.FISHERMAN)),getConfig().getInt("distribution.fisherman.master-price",32),Material.FISHING_ROD));
            villager.getPersistentDataContainer().set(villagerTradeMarkerKey,PersistentDataType.BYTE,(byte)1);
        }
    }

    private void applyPassiveEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyPassive(player, "speed", PotionEffectType.SPEED, 30, 0);
            applyPassive(player, "jump", PotionEffectType.JUMP_BOOST, 30, 0);
            applyPassive(player, "regeneration", PotionEffectType.REGENERATION, 30, 0);
            applyPassive(player, "night_vision", PotionEffectType.NIGHT_VISION, 30, 0);
            applyPassive(player, "water_breathing", PotionEffectType.WATER_BREATHING, 30, 0);
            applyPassive(player, "dolphins_grace", PotionEffectType.DOLPHINS_GRACE, 30, 0);
            applyPassive(player, "haste_aura", PotionEffectType.HASTE, 30, 0);
            applyPassive(player, "slow_fall", PotionEffectType.SLOW_FALLING, 30, 0);
            applyPassive(player, "tank", PotionEffectType.RESISTANCE, 30, 0);
            applyPassive(player, "fire_boots", PotionEffectType.FIRE_RESISTANCE, 30, 0);
            applyPassive(player, "molten", PotionEffectType.FIRE_RESISTANCE, 30, 0);
            applyPassive(player, "saturation", PotionEffectType.SATURATION, 1, 0);
            applyPassive(player, "barrier", PotionEffectType.ABSORPTION, 30, 0);
            applyPassive(player, "absorb", PotionEffectType.ABSORPTION, 30, 0);
            applyPassive(player, "heal", PotionEffectType.REGENERATION, 30, 0);
            applyPassive(player, "regain", PotionEffectType.REGENERATION, 30, 0);
            applyPassive(player, "light_spirit", PotionEffectType.NIGHT_VISION, 30, 0);
            applyPassive(player, "axolotl_buff", PotionEffectType.CONDUIT_POWER, 30, 0);
            if(has(player,"stella")&&player.getWorld().getTime()>12500) applyPassive(player,"stella",PotionEffectType.LUCK,30,0);
            if(has(player,"time_travel")&&player.isSprinting()) applyPassive(player,"time_travel",PotionEffectType.SPEED,12,0);
            if (has(player, "auto_repair") && repairReady.remove(player.getUniqueId()) != null) repairHeldItem(player, highest(player, "auto_repair"));
            if (has(player,"anti_stun")) { player.removePotionEffect(PotionEffectType.SLOWNESS); player.removePotionEffect(PotionEffectType.BLINDNESS); }
        }
    }
    private void applyPassive(Player player, String id, PotionEffectType effect, int seconds, int baseAmplifier) { int level=highest(player,id); if(level>0 && enabled(id)) player.addPotionEffect(new PotionEffect(effect, seconds*20, baseAmplifier + Math.min(2, level-1), true, false, true)); }
    private void repairHeldItem(Player player, int level) { ItemStack item=player.getInventory().getItemInMainHand(); if(item.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable damage && damage.hasDamage()) { damage.setDamage(Math.max(0,damage.getDamage()-Math.max(1,level))); item.setItemMeta(damage); } }
    private void trapTarget(LivingEntity target) { Location at=target.getLocation().getBlock().getLocation(); if(at.getBlock().getType().isAir()) { at.getBlock().setType(Material.COBWEB); getServer().getScheduler().runTaskLater(this,()->{if(at.getBlock().getType()==Material.COBWEB)at.getBlock().setType(Material.AIR);},60L); } }
    private void castGrimoire(Player caster, LivingEntity target, int level) { switch (getRandom().nextInt(3)) { case 0 -> target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * (2 + level), 0)); case 1 -> target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * (2 + level), 0)); default -> { caster.setHealth(Math.min(caster.getMaxHealth(), caster.getHealth() + level * .5)); target.setFireTicks(20 * level); } } caster.getWorld().spawnParticle(Particle.ENCHANT, target.getLocation().add(0,1,0), 18,.35,.5,.35,.1); }
    private void castHail(LivingEntity target, int level) { Location at=target.getLocation().add(0,1,0); target.getWorld().spawnParticle(Particle.SNOWFLAKE, at, 35 + level * 10,.7,.9,.7,.04); target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * (2 + level), Math.min(2,level-1))); target.damage(Math.min(3, level * .75)); }
    private Random getRandom() { return ThreadLocalRandom.current(); }
    private LevelModifier modifier(String key,double base,double perLevel,double capacity) { return LevelModifier.from(getConfig().getConfigurationSection("fishing.modifiers."+key),base,perLevel,capacity,LevelModifier.Action.ADD); }
    private boolean isBiome(Player player,String token) { return player.getLocation().getBlock().getBiome().getKey().getKey().toUpperCase(Locale.ROOT).contains(token); }
    private void giveFragment(Player player,String name,NamedTextColor color) { ItemStack fragment=new ItemStack(Material.PRISMARINE_SHARD); ItemMeta meta=fragment.getItemMeta(); meta.displayName(Component.text(name,color)); meta.lore(List.of(Component.text("Koleksi rahasia Veliora",NamedTextColor.DARK_GRAY))); fragment.setItemMeta(meta); player.getInventory().addItem(fragment).values().forEach(left->player.getWorld().dropItemNaturally(player.getLocation(),left)); player.getWorld().spawnParticle(Particle.ENCHANT,player.getLocation().add(0,1,0),24,.35,.5,.35,.05); player.playSound(player.getLocation(),Sound.UI_TOAST_CHALLENGE_COMPLETE,.5f,1.3f); }
    private FishingRarity rollFishingRarity() { return rollFishingRarity(FishingRarity.SECRET); }
    private FishingRarity rollFishingRarity(FishingRarity cap) { double total=0; for(FishingRarity rarity:FishingRarity.values()){if(rarity.ordinal()>cap.ordinal())break;total+=getConfig().getDouble("fishing.rarity."+rarity.id, switch(rarity){case COMMON->55;case RARE->28;case EPIC->12;case LEGENDARY->4.5;case MYTHIC->.45;case SECRET->.05;});} double roll=Math.random()*total, current=0; for(FishingRarity rarity:FishingRarity.values()){if(rarity.ordinal()>cap.ordinal())break;current+=getConfig().getDouble("fishing.rarity."+rarity.id,0);if(roll<current)return rarity;} return FishingRarity.COMMON; }
    private ItemStack rollFishingBook() { return rollFishingBook(FishingRarity.SECRET); }
    private ItemStack rollFishingBook(FishingRarity cap) { FishingRarity rarity=rollFishingRarity(cap); String id=rarity.enchantments.get(getRandom().nextInt(rarity.enchantments.size())); int level=rarity==FishingRarity.COMMON||rarity==FishingRarity.RARE||rarity==FishingRarity.EPIC?1+getRandom().nextInt(Math.min(3,defaultMaxLevel(LegacyEnchant.find(id).orElseThrow()))):1; return createBook(id,level,rarity); }
    private ItemStack rollLibrarianBook(FishingRarity cap) { List<String> ids=switch(cap){case COMMON->List.of("speed","haste_aura","night_vision","water_breathing");case RARE->List.of("life_steal","bleed","poison","critical","regeneration");default->List.of("barrier","tank","protection","fire_boots","shield_resistance");}; String id=ids.get(getRandom().nextInt(ids.size())); return createBook(id,1); }
    private MerchantRecipe specialRecipe(ItemStack result,int emeralds,Material material) { MerchantRecipe recipe=new MerchantRecipe(result,1); recipe.addIngredient(new ItemStack(Material.EMERALD,Math.min(64,Math.max(1,emeralds)))); recipe.addIngredient(new ItemStack(material)); return recipe; }
    private void openFishingMenu(Player player) {
        Inventory inventory=Bukkit.createInventory(new FishingMenuHolder(),54,FISHING_MENU_TITLE);
        ItemStack border=new ItemStack(Material.GRAY_STAINED_GLASS_PANE); ItemMeta borderMeta=border.getItemMeta(); borderMeta.displayName(Component.empty()); border.setItemMeta(borderMeta);
        for(int slot=0;slot<54;slot++) inventory.setItem(slot,border);
        int slot=10;
        for(FishingRarity rarity:FishingRarity.values()) for(String id:rarity.enchantments) {
            ItemStack book=createBook(id,1,rarity); inventory.setItem(slot++,book); if(slot%9==8)slot+=2;
        }
        player.openInventory(inventory);
    }
    private boolean has(Player player, String id) { return highest(player,id)>0; }
    private int highest(Player player, String id) { int highest=customLevel(player.getInventory().getItemInMainHand(),id); highest=Math.max(highest,customLevel(player.getInventory().getItemInOffHand(),id)); for(ItemStack item:player.getInventory().getArmorContents()) highest=Math.max(highest,customLevel(item,id)); return highest; }

    private String canonicalId(String id) { return LegacyEnchant.find(id).map(LegacyEnchant::id).orElse(id.toLowerCase(Locale.ROOT)); }
    private void migrateLegacyId(String oldId, String newId) {
        String oldPath="custom-enchants."+oldId, newPath="custom-enchants."+newId;
        if (getConfig().isConfigurationSection(oldPath)) {
            if (!getConfig().isConfigurationSection(newPath)) getConfig().set(newPath,getConfig().getConfigurationSection(oldPath).getValues(true));
            getConfig().set(oldPath,null);
        }
    }
    private boolean enabled(String id) { return getConfig().getBoolean("custom-enchants." + canonicalId(id) + ".enabled", true); }
    private int defaultMaxLevel(LegacyEnchant enchant) {
        return switch (enchant) {
            case AUTO_SMELT, TELEPATHY, AUTO_FARM, PHOENIX, SECOND_LIFE, DEATH_ANGEL -> 1;
            case ABYSSAL_HOOK, LEVIATHAN_LINE, VELIORA_SECRET -> 1;
            case LIFE_STEAL, SHIELD_RESISTANCE -> 5;
            case SUNKEN_RELIC, STORM_ANGLER, MERMAID_TEARS -> 2;
            default -> 3;
        };
    }
    private int cap(String id, int fallback) { return Math.max(1, getConfig().getInt("overlevel.caps." + id, fallback)); }
    private long tick() { return getServer().getCurrentTick(); }
    private boolean ready(Player player, String id, long fallbackTicks) { id=canonicalId(id); long configured=getConfig().getLong("custom-enchants."+id+".cooldown-ticks", -1); long ticks=configured>=0?configured:fallbackTicks; long now=tick(); String key=player.getUniqueId()+":"+id; long until=cooldowns.getOrDefault(key,0L); if(until>now)return false; cooldowns.put(key,now+Math.max(0,ticks)); return true; }
    private int enchantLevel(ItemStack item, Enchantment enchant) { if(item.getType()==Material.ENCHANTED_BOOK && item.getItemMeta() instanceof EnchantmentStorageMeta meta) return meta.getStoredEnchantLevel(enchant); return item.getEnchantmentLevel(enchant); }
    private void setEnchant(ItemStack item, Enchantment enchant, int level) { ItemMeta meta=item.getItemMeta(); if(meta instanceof EnchantmentStorageMeta book) { book.addStoredEnchant(enchant, level, true); item.setItemMeta(book); } else item.addUnsafeEnchantment(enchant, level); }
    private int customLevel(ItemStack item, String id) {
        if(item == null || item.getType().isAir() || item.getItemMeta()==null) return 0;
        String canonical=canonicalId(id);
        int level=item.getItemMeta().getPersistentDataContainer().getOrDefault(customKey(canonical), PersistentDataType.INTEGER, 0);
        if(level==0 && canonical.equals("life_steal")) level=item.getItemMeta().getPersistentDataContainer().getOrDefault(customKey("lifesteal"),PersistentDataType.INTEGER,0);
        if(level==0 && canonical.equals("auto_smelt")) level=item.getItemMeta().getPersistentDataContainer().getOrDefault(customKey("autosmelt"),PersistentDataType.INTEGER,0);
        return level > 0 && !hasExcellentEquivalent(item,canonical) ? level : 0;
    }
    private boolean hasExcellentEquivalent(ItemStack item, String velioraId) {
        if (!excellentEnchantsPresent || !getConfig().getBoolean("excellent-enchants-bridge.enabled", true)) return false;
        Set<String> equivalents = switch (velioraId) {
            case "speed" -> Set.of("speedy", "nimble");
            case "haste_aura" -> Set.of("haste");
            case "night_vision" -> Set.of("nightvision");
            case "saturation" -> Set.of("saturation");
            case "water_breathing" -> Set.of("waterbreathing");
            case "fire_boots", "molten" -> Set.of("fireshield", "flamewalker");
            case "protection", "guarded", "obsidian_plate", "ductile", "sturdy" -> Set.of("hardened", "elementalprotection");
            case "poisonous_thorns" -> Set.of("rebound");
            case "auto_farm" -> Set.of("replanter");
            default -> Set.of();
        };
        if (equivalents.isEmpty()) return false;
        return item.getEnchantments().keySet().stream().anyMatch(enchant -> equivalents.contains(enchant.getKey().getKey().replace("_", "").toLowerCase(Locale.ROOT)));
    }
    private int excellentLevel(ItemStack item, String id) {
        if (!excellentEnchantsPresent || item == null) return 0;
        String requested=id.replace("_", "").toLowerCase(Locale.ROOT);
        return item.getEnchantments().entrySet().stream()
            .filter(entry -> entry.getKey().getKey().getKey().replace("_", "").equalsIgnoreCase(requested))
            .mapToInt(Map.Entry::getValue).max().orElse(0);
    }
    private void applyExcellentKillHeal(Player player, ItemStack item, String excellentId, String cooldownId) {
        if (!getConfig().getBoolean("excellent-vampire.enabled", true)) return;
        int level=Math.min(getConfig().getInt("excellent-vampire.max-level",3), excellentLevel(item,excellentId));
        if (level <= 0 || !ready(player,cooldownId,getConfig().getLong("excellent-vampire.cooldown-ticks",60))) return;
        double healed=level*getConfig().getDouble("excellent-vampire.heal-per-level",2.0);
        player.setHealth(Math.min(player.getMaxHealth(),player.getHealth()+healed));
        player.getWorld().spawnParticle(Particle.HEART,player.getLocation().add(0,1,0),Math.min(8,level*2),.3,.35,.3,0);
        player.playSound(player.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,.35f,1.6f);
    }
    private ItemStack createBook(String id, int level) { return createBook(id,level,rarityFor(id)); }
    private ItemStack createBook(String id, int level, FishingRarity rarity) {
        id=canonicalId(id);
        LegacyEnchant enchant=LegacyEnchant.find(id).orElseThrow();
        NamedTextColor color=rarity==null?categoryColor(enchant.category()):rarity.color;
        ItemStack book=new ItemStack(Material.ENCHANTED_BOOK); ItemMeta meta=book.getItemMeta();
        meta.getPersistentDataContainer().set(customKey, PersistentDataType.STRING, id);
        meta.getPersistentDataContainer().set(customKey(id),PersistentDataType.INTEGER,level);
        meta.displayName(Component.text(pretty(id)+" "+roman(level),color));
        List<Component> lore=new ArrayList<>();
        lore.add(Component.text(description(id),NamedTextColor.GRAY));
        lore.add(Component.text("",NamedTextColor.WHITE));
        lore.add(Component.text("Untuk: "+categoryLabel(enchant.category()),NamedTextColor.DARK_AQUA));
        lore.add(Component.text("Level: "+roman(level)+" / "+roman(defaultMaxLevel(enchant)),color));
        if(rarity!=null) lore.add(Component.text("Rarity: "+pretty(rarity.id),rarity.color));
        lore.add(Component.text("",NamedTextColor.WHITE));
        lore.add(Component.text("Gabungkan di anvil dengan item yang sesuai.",NamedTextColor.DARK_GRAY));
        meta.lore(lore); book.setItemMeta(meta); return book;
    }
    private FishingRarity rarityFor(String id) { return Arrays.stream(FishingRarity.values()).filter(rarity -> rarity.enchantments.contains(id)).findFirst().orElse(null); }
    private NamedTextColor categoryColor(LegacyEnchant.Category category) { return switch(category) { case WEAPON -> NamedTextColor.RED; case TOOL -> NamedTextColor.GOLD; case ARMOR -> NamedTextColor.AQUA; case BOW -> NamedTextColor.GREEN; case SHIELD -> NamedTextColor.LIGHT_PURPLE; case MACE -> NamedTextColor.DARK_PURPLE; case FISHING_ROD -> NamedTextColor.BLUE; }; }
    private String categoryLabel(LegacyEnchant.Category category) { return switch(category) { case WEAPON -> "Pedang, kapak, mace, atau trident"; case TOOL -> "Pickaxe, kapak, sekop, atau hoe"; case ARMOR -> "Armor atau elytra"; case BOW -> "Bow atau crossbow"; case SHIELD -> "Shield"; case MACE -> "Mace"; case FISHING_ROD -> "Fishing Rod"; }; }
    private String description(String id) { return switch(id) {
        case "life_steal" -> "Serangan memulihkan sedikit health.";
        case "bleed" -> "Memberi efek wither singkat pada target.";
        case "poison" -> "Memberi racun singkat pada target.";
        case "blind", "debuff" -> "Memberi kebutaan singkat pada target.";
        case "burning" -> "Membakar target saat terkena serangan.";
        case "critical" -> "Memiliki peluang damage critical lebih besar.";
        case "lightning" -> "Memanggil kilat visual dengan cooldown.";
        case "freeze" -> "Menambah efek beku pada target.";
        case "wind_strike", "sudden_blow" -> "Mendorong target menjauh dengan cooldown.";
        case "sharpen", "blast" -> "Menambah damage serangan.";
        case "omnivamp", "soul_eater" -> "Menyembuhkan pemakai berdasarkan damage.";
        case "cobweb" -> "Menjebak target dalam cobweb sementara.";
        case "storm" -> "Memanggil kilat dan memperlambat target.";
        case "nulled" -> "Menghapus efek potion milik target.";
        case "craving" -> "Mengurangi rasa lapar pemain lawan.";
        case "emnity" -> "Memberi kelemahan singkat pada target.";
        case "grimoire" -> "Mengacak sihir slow, weak, api, atau heal.";
        case "hail_storm" -> "Menyebabkan snow burst, slow, dan damage kecil.";
        case "illusion" -> "Memberi invisibility singkat setelah serangan.";
        case "implant" -> "Memberi EXP tambahan ketika menyerang.";
        case "steal" -> "Memindahkan sebagian hunger dari lawan.";
        case "wind_burst" -> "Melontarkan target dan mengurangi fall damage.";
        case "blinding_arrow" -> "Panah memberi kebutaan singkat.";
        case "frost_arrow" -> "Panah memberi efek beku.";
        case "focus_fire" -> "Panah menghasilkan damage tambahan.";
        case "shield_resistance" -> "Peluang mengurangi cooldown shield akibat kapak.";
        case "protection", "guarded", "obsidian_plate", "ductile", "sturdy" -> "Mengurangi damage yang diterima saat dipakai.";
        case "emergency_defence" -> "Defense bertambah saat health rendah.";
        case "poisonous_thorns" -> "Penyerang menerima racun saat memukulmu.";
        case "auto_repair" -> "Memperbaiki item yang dipakai setiap 10 detik saat rusak.";
        case "unbreaking" -> "Memiliki peluang durability tidak berkurang.";
        case "auto_smelt" -> "Ore tertentu langsung menjadi ingot saat ditambang.";
        case "telepathy" -> "Drop block langsung masuk ke inventory.";
        case "auto_farm" -> "Tanaman matang ditanam kembali otomatis.";
        case "lucky_treasure" -> "Memiliki peluang mendapat EXP bonus saat menambang.";
        case "experience", "levels" -> "Meningkatkan EXP dari block yang ditambang.";
        case "auto_reel" -> "Memberi tanda saat ikan menggigit.";
        case "guardian_hook" -> "Memperlambat mob yang tertarik pancing.";
        case "deep_hook" -> "Memberi EXP tambahan ketika memancing.";
        case "fisherman_heal" -> "Memulihkan health saat mendapat tangkapan.";
        case "double_catch", "angler_luck" -> "Memiliki peluang mendapat tangkapan ganda.";
        case "treasure_hook" -> "Memiliki peluang mendapat harta laut tambahan.";
        case "ocean_blessing" -> "Meningkatkan peluang harta ketika di ocean.";
        case "river_spirit" -> "Memberi EXP tambahan ketika memancing di river.";
        case "sunken_relic" -> "Peluang sangat kecil memperoleh Sunken Relic.";
        case "storm_angler" -> "Memberi EXP tambahan ketika badai.";
        case "mermaid_tears" -> "Memberi regen singkat saat memancing.";
        case "veliora_secret" -> "Peluang sangat kecil memperoleh Secret Fragment.";
        default -> "Kemampuan khusus Veliora untuk perlengkapanmu.";
    }; }
    private NamespacedKey customKey(String id) { return new NamespacedKey(this, "ce_" + id); }
    private boolean isCustomBook(ItemStack item) { return item.getType() == Material.ENCHANTED_BOOK && item.getItemMeta() != null && item.getItemMeta().getPersistentDataContainer().has(customKey, PersistentDataType.STRING); }
    private boolean applyCustomBook(ItemStack left, ItemStack right, ItemStack result) {
        if (right.getType() != Material.ENCHANTED_BOOK || right.getItemMeta() == null) return false;
        String rawId = right.getItemMeta().getPersistentDataContainer().get(customKey, PersistentDataType.STRING);
        if (rawId == null || LegacyEnchant.find(rawId).isEmpty()) return false;
        String id=canonicalId(rawId);
        if (left.getType()==Material.ENCHANTED_BOOK && isCustomBook(left)) {
            String leftId=left.getItemMeta().getPersistentDataContainer().get(customKey,PersistentDataType.STRING);
            if (leftId==null || !canonicalId(leftId).equals(id)) return false;
            int incoming=customLevel(right,id), current=customLevel(left,id);
            int merged=Math.min(getConfig().getInt("custom-enchants."+id+".max-level",3),incoming==current?current+1:Math.max(incoming,current));
            if(merged<=current)return false;
            ItemStack book=createBook(id,merged); result.setType(book.getType()); result.setItemMeta(book.getItemMeta()); return true;
        }
        if (!canApply(left.getType(), id)) return false;
        int incoming = customLevel(right, id); if (incoming < 1) return false;
        int current=customLevel(left,id);
        int merged = Math.min(getConfig().getInt("custom-enchants." + id + ".max-level", 3), incoming == current ? current + 1 : Math.max(incoming, current));
        if (merged <= current) return false;
        ItemMeta meta = result.getItemMeta(); if (meta == null) return false;
        meta.getPersistentDataContainer().set(customKey(id), PersistentDataType.INTEGER, merged);
        List<Component> lore = new ArrayList<>(Optional.ofNullable(meta.lore()).orElse(List.of()));
        lore.removeIf(line -> {
            String plain=PlainTextComponentSerializer.plainText().serialize(line);
            return plain.startsWith("✦ " + pretty(id)) || plain.startsWith(pretty(id) + " ");
        });
        lore.add(Component.text(pretty(id) + " " + roman(merged), NamedTextColor.AQUA));
        meta.lore(lore); result.setItemMeta(meta); return true;
    }
    private void applyCustomEnchant(ItemStack item,String id,int level) {
        id=canonicalId(id); ItemMeta meta=item.getItemMeta(); if(meta==null)return;
        meta.getPersistentDataContainer().set(customKey(id),PersistentDataType.INTEGER,level);
        if(item.getType()==Material.BOOK || item.getType()==Material.ENCHANTED_BOOK) meta.getPersistentDataContainer().set(customKey,PersistentDataType.STRING,id);
        List<Component> lore=new ArrayList<>(Optional.ofNullable(meta.lore()).orElse(List.of()));
        String title=pretty(id); lore.removeIf(line->PlainTextComponentSerializer.plainText().serialize(line).startsWith(title+" "));
        lore.add(Component.text(title+" "+roman(level),categoryColor(LegacyEnchant.find(id).orElseThrow().category())));
        meta.lore(lore); item.setItemMeta(meta);
    }
    private boolean canApply(Material material, String id) { return LegacyEnchant.find(id).map(type -> type.accepts(material)).orElse(false); }
    private String pretty(String id) { return Arrays.stream(id.split("_")).map(s -> Character.toUpperCase(s.charAt(0))+s.substring(1)).reduce((a,b)->a+" "+b).orElse(id); }
    private String roman(int value) { String[] r={"","I","II","III","IV","V","VI","VII","VIII","IX","X"}; return value>0&&value<r.length?r[value]:String.valueOf(value); }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("guide")) { sendMemberGuide(sender); return true; }
        if (args.length == 1 && args[0].equalsIgnoreCase("menu") && sender instanceof Player player) {
            if (!player.hasPermission("velioraenchant.use")) { player.sendMessage(Component.text("Kamu tidak memiliki izin.",NamedTextColor.RED)); return true; }
            openFishingMenu(player); return true;
        }
        if (!sender.hasPermission("velioraenchant.admin")) { sender.sendMessage(Component.text("Command admin. Gunakan /enchants untuk panduan.",NamedTextColor.RED)); return true; }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) { reloadConfig(); distributionPolicy=new DistributionPolicy(getConfig().getConfigurationSection("distribution")); sender.sendMessage(Component.text("VelioraEnchant reloaded.",NamedTextColor.GREEN)); return true; }
        if (args.length == 2 && (args[0].equalsIgnoreCase("rodroll") || args[0].equalsIgnoreCase("fishroll"))) { Player target=Bukkit.getPlayerExact(args[1]); if(target==null){sender.sendMessage(Component.text("Player tidak online.",NamedTextColor.RED));return true;} target.getInventory().addItem(rollFishingBook()).values().forEach(item->target.getWorld().dropItemNaturally(target.getLocation(),item)); sender.sendMessage(Component.text("Fishing rod enchant book di-roll.",NamedTextColor.GREEN)); return true; }
        if (args.length == 4 && args[0].equalsIgnoreCase("give")) { Player target=Bukkit.getPlayerExact(args[1]); if(target==null){sender.sendMessage(Component.text("Player tidak online.",NamedTextColor.RED));return true;} int level; try{level=Integer.parseInt(args[3]);}catch(NumberFormatException e){return false;} String id=canonicalId(args[2]); if(LegacyEnchant.find(id).isEmpty()){sender.sendMessage(Component.text("Enchant tidak dikenal.",NamedTextColor.RED));return true;} level=Math.clamp(level,1,getConfig().getInt("custom-enchants."+id+".max-level",3)); target.getInventory().addItem(createBook(id,level)).values().forEach(i->target.getWorld().dropItemNaturally(target.getLocation(),i)); sender.sendMessage(Component.text("Book diberikan.",NamedTextColor.GREEN));return true; }
        sender.sendMessage(Component.text("/venchant give <player> <enchant> <level> | /venchant rodroll <player> | /venchant reload",NamedTextColor.YELLOW)); return true;
    }
    private void sendMemberGuide(CommandSender sender) {
        sender.sendMessage(Component.text("Veliora Custom Enchant",NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Dapatkan buku dari loot structure, Librarian, Fisherman, atau hadiah server.",NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Cara pakai: taruh equipment di slot kiri anvil dan buku custom di slot kanan.",NamedTextColor.WHITE));
        sender.sendMessage(Component.text("Buku hanya bisa dipasang pada jenis item yang tertulis di tooltip.",NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Gabungkan dua level sama untuk menaikkan level, contoh I + I menjadi II.",NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Gunakan /enchants menu untuk melihat katalog enchant fishing.",NamedTextColor.AQUA));
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) { if(args.length==1)return sender.hasPermission("velioraenchant.admin")?List.of("help","menu","give","reload","rodroll"):List.of("help","menu"); if(args.length==2&&sender.hasPermission("velioraenchant.admin")&&(args[0].equalsIgnoreCase("give")||args[0].equalsIgnoreCase("rodroll")))return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(); if(args.length==3&&sender.hasPermission("velioraenchant.admin")&&args[0].equalsIgnoreCase("give"))return LegacyEnchant.ids(); return List.of(); }
    private static final class FishingMenuHolder implements InventoryHolder { @Override public Inventory getInventory() { return null; } }
}
