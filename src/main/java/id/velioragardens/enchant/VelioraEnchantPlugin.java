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
import org.bukkit.event.player.PlayerItemDamageEvent;
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

public final class VelioraEnchantPlugin extends JavaPlugin implements Listener, TabExecutor {
    private NamespacedKey customKey;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> windUntil = new ConcurrentHashMap<>();
    private final Set<Location> veinBreaking = ConcurrentHashMap.newKeySet();
    private final Map<String, Enchantment> overlevel = new LinkedHashMap<>();

    @Override public void onEnable() {
        saveDefaultConfig();
        for (LegacyEnchant enchant : LegacyEnchant.values()) {
            String path = "custom-enchants." + enchant.id();
            getConfig().addDefault(path + ".enabled", true);
            getConfig().addDefault(path + ".max-level", 3);
        }
        getConfig().options().copyDefaults(true);
        saveConfig();
        customKey = new NamespacedKey(this, "custom_enchant");
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
        getServer().getScheduler().runTaskTimer(this, this::applyPassiveEffects, 20L, 20L);
    }

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
        if (changed) event.setResult(edited);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof LivingEntity target)) return;
        ItemStack weapon = player.getInventory().getItemInMainHand();
        int lifesteal = customLevel(weapon, "lifesteal");
        if (lifesteal > 0 && enabled("lifesteal") && ready(player, "lifesteal", getConfig().getLong("custom-enchants.lifesteal.cooldown-ticks", 20))) {
            double heal = getConfig().getDouble("custom-enchants.lifesteal.heal-per-level", .35) * lifesteal;
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

    @EventHandler(ignoreCancelled = true)
    public void onArmorDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        double reduction = 0;
        reduction += highest(player,"protection") * .025;
        reduction += highest(player,"guarded") * .02;
        reduction += highest(player,"obsidian_plate") * .025;
        reduction += highest(player,"ductile") * .015;
        reduction += highest(player,"sturdy") * .015;
        if (player.getHealth() / player.getMaxHealth() < .35) reduction += highest(player,"emergency_defence") * .04;
        if (reduction > 0) event.setDamage(event.getDamage() * Math.max(.35, 1 - Math.min(.55, reduction)));
        int thorns = highest(player,"poisonous_thorns");
        if (thorns > 0 && event instanceof EntityDamageByEntityEvent hit && hit.getDamager() instanceof LivingEntity attacker && ready(player,"poisonous_thorns",35)) attacker.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * thorns, 0));
    }

    @EventHandler(ignoreCancelled = true)
    public void onDurability(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem(); int unbreaking=Math.max(customLevel(item,"unbreaking"),customLevel(item,"ductile"));
        if (unbreaking > 0 && Math.random() < Math.min(.75, unbreaking * .12)) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer(); ItemStack tool = player.getInventory().getItemInMainHand(); Block origin = event.getBlock();
        boolean smelted = customLevel(tool, "autosmelt") > 0 && enabled("autosmelt") && autosmelt(event, player, origin);
        if (!smelted && customLevel(tool, "telepathy") > 0 && enabled("telepathy")) telepathy(event, player, origin);
        int vein = customLevel(tool, "vein");
        if (vein > 0 && enabled("vein") && !veinBreaking.remove(origin.getLocation())) mineVein(player, origin, tool, vein);
        int trees = customLevel(tool, "deforestation");
        if (trees > 0 && enabled("deforestation") && origin.getType().name().endsWith("_LOG") && !veinBreaking.remove(origin.getLocation())) mineVein(player, origin, tool, trees);
        if (customLevel(tool,"flower") > 0 && origin.getType() == Material.GRASS_BLOCK && ready(player,"flower",30)) origin.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, origin.getLocation().add(.5,1,.5), 8,.4,.2,.4,0);
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
            if (has(player, "auto_repair")) repairHeldItem(player, highest(player, "auto_repair"));
            if (has(player,"anti_stun")) { player.removePotionEffect(PotionEffectType.SLOWNESS); player.removePotionEffect(PotionEffectType.BLINDNESS); }
            if (has(player,"miner_radar")) markNearbyOres(player, highest(player,"miner_radar"));
        }
    }
    private void applyPassive(Player player, String id, PotionEffectType effect, int seconds, int baseAmplifier) { int level=highest(player,id); if(level>0 && enabled(id)) player.addPotionEffect(new PotionEffect(effect, seconds*20, baseAmplifier + Math.min(2, level-1), true, false, true)); }
    private void repairHeldItem(Player player, int level) { ItemStack item=player.getInventory().getItemInMainHand(); if(item.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable damage && damage.hasDamage()) { damage.setDamage(Math.max(0,damage.getDamage()-Math.max(1,level))); item.setItemMeta(damage); } }
    private void trapTarget(LivingEntity target) { Location at=target.getLocation().getBlock().getLocation(); if(at.getBlock().getType().isAir()) { at.getBlock().setType(Material.COBWEB); getServer().getScheduler().runTaskLater(this,()->{if(at.getBlock().getType()==Material.COBWEB)at.getBlock().setType(Material.AIR);},60L); } }
    private void markNearbyOres(Player player, int level) { Location base=player.getLocation(); int radius=Math.min(10,3+level*2); int found=0; for(int x=-radius;x<=radius&&found<12;x++)for(int y=-3;y<=3&&found<12;y++)for(int z=-radius;z<=radius&&found<12;z++){ Block block=base.clone().add(x,y,z).getBlock(); if(block.getType().name().endsWith("_ORE")){player.spawnParticle(Particle.END_ROD,block.getLocation().add(.5,.5,.5),1,0,0,0,0);found++;}} }
    private boolean has(Player player, String id) { return highest(player,id)>0; }
    private int highest(Player player, String id) { int highest=customLevel(player.getInventory().getItemInMainHand(),id); highest=Math.max(highest,customLevel(player.getInventory().getItemInOffHand(),id)); for(ItemStack item:player.getInventory().getArmorContents()) highest=Math.max(highest,customLevel(item,id)); return highest; }

    private boolean enabled(String id) { return getConfig().getBoolean("custom-enchants." + id + ".enabled", true); }
    private int cap(String id, int fallback) { return Math.max(1, getConfig().getInt("overlevel.caps." + id, fallback)); }
    private long tick() { return getServer().getCurrentTick(); }
    private boolean ready(Player player, String id, long ticks) { long now=tick(), until=cooldowns.getOrDefault(player.getUniqueId(), 0L); if(until>now) return false; cooldowns.put(player.getUniqueId(), now+ticks); return true; }
    private int enchantLevel(ItemStack item, Enchantment enchant) { if(item.getType()==Material.ENCHANTED_BOOK && item.getItemMeta() instanceof EnchantmentStorageMeta meta) return meta.getStoredEnchantLevel(enchant); return item.getEnchantmentLevel(enchant); }
    private void setEnchant(ItemStack item, Enchantment enchant, int level) { ItemMeta meta=item.getItemMeta(); if(meta instanceof EnchantmentStorageMeta book) { book.addStoredEnchant(enchant, level, true); item.setItemMeta(book); } else item.addUnsafeEnchantment(enchant, level); }
    private int customLevel(ItemStack item, String id) { if(item == null || item.getType().isAir() || item.getItemMeta()==null) return 0; return item.getItemMeta().getPersistentDataContainer().getOrDefault(customKey(id), PersistentDataType.INTEGER, 0); }
    private ItemStack createBook(String id, int level) { ItemStack book=new ItemStack(Material.ENCHANTED_BOOK); ItemMeta meta=book.getItemMeta(); meta.getPersistentDataContainer().set(customKey, PersistentDataType.STRING, id); meta.getPersistentDataContainer().set(customKey(id),PersistentDataType.INTEGER,level); meta.displayName(Component.text(pretty(id)+" "+roman(level), NamedTextColor.AQUA)); meta.lore(List.of(Component.text("Gabungkan di anvil dengan item yang sesuai.",NamedTextColor.DARK_GRAY))); book.setItemMeta(meta); return book; }
    private NamespacedKey customKey(String id) { return new NamespacedKey(this, "ce_" + id); }
    private boolean isCustomBook(ItemStack item) { return item.getType() == Material.ENCHANTED_BOOK && item.getItemMeta() != null && item.getItemMeta().getPersistentDataContainer().has(customKey, PersistentDataType.STRING); }
    private boolean applyCustomBook(ItemStack left, ItemStack right, ItemStack result) {
        if (right.getType() != Material.ENCHANTED_BOOK || right.getItemMeta() == null) return false;
        String id = right.getItemMeta().getPersistentDataContainer().get(customKey, PersistentDataType.STRING);
        if (id == null || LegacyEnchant.find(id).isEmpty() || !canApply(left.getType(), id)) return false;
        int incoming = customLevel(right, id); if (incoming < 1) return false;
        int merged = Math.min(getConfig().getInt("custom-enchants." + id + ".max-level", 1), Math.max(incoming, customLevel(left, id)));
        ItemMeta meta = result.getItemMeta(); if (meta == null) return false;
        meta.getPersistentDataContainer().set(customKey(id), PersistentDataType.INTEGER, merged);
        List<Component> lore = new ArrayList<>(Optional.ofNullable(meta.lore()).orElse(List.of()));
        lore.removeIf(line -> PlainTextComponentSerializer.plainText().serialize(line).startsWith("✦ " + pretty(id)));
        lore.add(Component.text("✦ " + pretty(id) + " " + roman(merged), NamedTextColor.AQUA));
        meta.lore(lore); result.setItemMeta(meta); return true;
    }
    private boolean canApply(Material material, String id) { return LegacyEnchant.find(id).map(type -> type.accepts(material)).orElse(false); }
    private String pretty(String id) { return Arrays.stream(id.split("_")).map(s -> Character.toUpperCase(s.charAt(0))+s.substring(1)).reduce((a,b)->a+" "+b).orElse(id); }
    private String roman(int value) { String[] r={"","I","II","III","IV","V","VI","VII","VIII","IX","X"}; return value>0&&value<r.length?r[value]:String.valueOf(value); }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) { reloadConfig(); sender.sendMessage(Component.text("VelioraEnchant reloaded.",NamedTextColor.GREEN)); return true; }
        if (args.length == 4 && args[0].equalsIgnoreCase("give")) { Player target=Bukkit.getPlayerExact(args[1]); if(target==null){sender.sendMessage(Component.text("Player tidak online.",NamedTextColor.RED));return true;} int level; try{level=Integer.parseInt(args[3]);}catch(NumberFormatException e){return false;} String id=args[2].toLowerCase(Locale.ROOT); if(LegacyEnchant.find(id).isEmpty()){sender.sendMessage(Component.text("Enchant tidak dikenal.",NamedTextColor.RED));return true;} level=Math.clamp(level,1,getConfig().getInt("custom-enchants."+id+".max-level",3)); target.getInventory().addItem(createBook(id,level)).values().forEach(i->target.getWorld().dropItemNaturally(target.getLocation(),i)); sender.sendMessage(Component.text("Book diberikan.",NamedTextColor.GREEN));return true; }
        sender.sendMessage(Component.text("/venchant give <player> <enchant> <level> | /venchant reload",NamedTextColor.YELLOW)); return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) { if(args.length==1)return List.of("give","reload"); if(args.length==2&&args[0].equalsIgnoreCase("give"))return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(); if(args.length==3)return LegacyEnchant.ids(); return List.of(); }
}
