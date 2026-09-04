package id.velioragardens.enchant;
import java.util.*;
import org.bukkit.Material;
final class ExpansionRules {
    record Spec(int max, String description) {}
    static final Map<String,Spec> SPECS=Map.ofEntries(
        Map.entry("haste_tool",new Spec(3,"Memecahkan blok dengan alat memberi Haste I 1–3 detik; cooldown 10 detik.")),
        Map.entry("abrasion",new Spec(3,"Durability armor target berkurang 1–3, tidak sampai pecah; cooldown 10 detik.")),
        Map.entry("adrenaline",new Spec(3,"Diserang mob: Strength I 1–3 detik; cooldown 20 detik.")),
        Map.entry("arctic_freeze",new Spec(3,"Hit memberi Slowness I dan satu luka tambahan tertunda; cooldown 10 detik.")),
        Map.entry("ascend",new Spec(3,"Klik kanan pedang: Levitation I 1–3 detik; cooldown 30 detik.")),
        Map.entry("aura",new Spec(3,"Damage pemain berkurang 3–9% saat ada rekan pemain dekat; radius 3 blok.")),
        Map.entry("blaze_reaper",new Spec(3,"Bonus damage 5–15% pada mob Nether tertentu.")),
        Map.entry("brightness",new Spec(3,"Bonus damage 5–15% pada Warden di cahaya rendah.")),
        Map.entry("caffeinated",new Spec(3,"Hit sambil sprint memberi Haste I singkat; cooldown 15 detik.")),
        Map.entry("carve",new Spec(3,"Hit utama memberi damage area kecil, maksimal 3 target radius 3; cooldown 10 detik.")),
        Map.entry("charge",new Spec(2,"Klik kanan pedang sambil sneak: dorong maju di tanah; cooldown 15 detik.")),
        Map.entry("contagion",new Spec(3,"Hit panah menghasilkan efek cloud visual dan damage area sekali; cooldown 15 detik.")),
        Map.entry("cubism",new Spec(3,"Bonus damage 5–15% pada slime/magma cube, termasuk panah.")),
        Map.entry("double_blow",new Spec(4,"Peluang 5–20% damage trident dua kali; cooldown 15 detik.")),
        Map.entry("end_affinity",new Spec(3,"Damage di End berkurang 3–9%.")),
        Map.entry("enderbane",new Spec(5,"Bonus damage 4–20% pada Enderman/Ender Dragon.")),
        Map.entry("escape",new Spec(2,"Setelah menerima damage: Speed I 1–2 detik; cooldown 15 detik.")),
        Map.entry("explosive",new Spec(5,"Hit panah: damage area kecil tanpa merusak blok; cooldown 15 detik.")),
        Map.entry("feather_step",new Spec(5,"Peluang 5–25% membatalkan fall damage; cooldown 20 detik.")),
        Map.entry("finishing",new Spec(3,"Bonus damage 5–15% saat target di bawah 25% HP.")),
        Map.entry("fire_hook",new Spec(3,"Hook mengenai target: api 1–3 detik; cooldown 10 detik.")),
        Map.entry("first_strike",new Spec(3,"Bonus damage 5–15% terhadap target dengan HP penuh.")),
        Map.entry("getaway",new Spec(3,"Menerima damage saat HP rendah: Speed I 1–3 detik; cooldown 20 detik.")),
        Map.entry("incinerate",new Spec(3,"Bonus damage 5–15% pada spider/cave spider.")),
        Map.entry("multi_shot",new Spec(3,"Panah tambahan 1–3 saat tembakan penuh; cooldown 15 detik; hilang setelah 3 detik.")),
        Map.entry("nether_affinity",new Spec(3,"Damage di Nether berkurang 3–9%; bukan kebal lava.")),
        Map.entry("ninja",new Spec(3,"Bonus damage 5–15% saat sneak.")),
        Map.entry("poisoned_hook",new Spec(3,"Hook mengenai target: Poison I 1–3 detik; cooldown 10 detik.")),
        Map.entry("postpone",new Spec(3,"Peluang 10–30% mengurangi knockback terhadap mob; cooldown 5 detik.")),
        Map.entry("ravenous",new Spec(4,"Hit memulihkan 1 hunger; cooldown 20–14 detik.")),
        Map.entry("rebounding",new Spec(3,"Pantulkan 5–15% damage, maksimum 3 HP; cooldown 15 detik.")),
        Map.entry("repel",new Spec(3,"Hit memberi dorongan horizontal kecil; cooldown 10 detik.")),
        Map.entry("resonate",new Spec(3,"Pantulkan 5–15% damage, maksimum 3 HP; berbagi cooldown 15 detik dengan Rebounding.")),
        Map.entry("rumble",new Spec(3,"Saat diserang: damage area kecil, maksimal 3 musuh; cooldown 15 detik.")),
        Map.entry("scorching",new Spec(3,"Serangan masuk memicu api 1–3 detik pada penyerang; cooldown 10 detik.")),
        Map.entry("sharpness_hook",new Spec(9,"Hook memberi 0.25 HP per level pada target; cooldown 10 detik.")),
        Map.entry("shura",new Spec(3,"Bonus critical damage 5–15% saat HP sendiri di bawah separuh.")),
        Map.entry("skullcrusher",new Spec(3,"Bonus damage 5–15% pada jenis skeleton.")),
        Map.entry("starvation",new Spec(3,"Hit memberi Hunger I 1–3 detik; cooldown 10 detik.")),
        Map.entry("thor",new Spec(3,"Hit memicu petir visual dan bonus damage terbatas; cooldown 15 detik.")),
        Map.entry("zombie_crusher",new Spec(3,"Bonus damage 5–15% pada jenis zombie."))
    );
    static boolean isNew(String id) { return SPECS.containsKey(id); }
    static String alias(String id) {
        return switch(id.toLowerCase(Locale.ROOT).replace(" ","_")) {
            case "blackout" -> "blind"; case "criticals" -> "critical";
            case "flashbang" -> "blinding_arrow"; case "frost" -> "frost_arrow";
            case "infernal_touch" -> "auto_smelt"; case "replenish" -> "auto_farm";
            case "waterborne" -> "water_breathing";
            case "haste","alacrity" -> "haste_tool";
            default -> id;
        };
    }
    static boolean accepts(String id,Material m) {
        String n=m.name();
        if(id.equals("haste_tool"))return n.endsWith("_PICKAXE")||n.endsWith("_AXE")||n.endsWith("_SHOVEL");
        if(Set.of("escape","feather_step").contains(id)) return n.endsWith("_BOOTS");
        if(Set.of("adrenaline","aura","end_affinity","getaway","nether_affinity","rebounding","resonate","rumble","scorching").contains(id))
            return n.endsWith("_HELMET")||n.endsWith("_CHESTPLATE")||n.endsWith("_LEGGINGS")||n.endsWith("_BOOTS");
        if(Set.of("contagion","explosive","multi_shot").contains(id)) return m==Material.BOW;
        if(id.endsWith("_hook")) return m==Material.FISHING_ROD;
        if(id.equals("double_blow")) return m==Material.TRIDENT;
        if(id.equals("cubism")) return n.endsWith("_SWORD")||n.endsWith("_AXE")||m==Material.BOW||m==Material.CROSSBOW;
        if(id.equals("thor")) return n.endsWith("_SWORD")||m==Material.BOW||m==Material.CROSSBOW||m==Material.TRIDENT;
        if(Set.of("arctic_freeze","ascend","brightness","caffeinated","carve","charge","ninja","postpone","repel","shura").contains(id)) return n.endsWith("_SWORD");
        return n.endsWith("_SWORD")||n.endsWith("_AXE");
    }
    static double reflection(double damage,int level) { return Math.min(3,Math.max(0,damage)*.05*Math.clamp(level,0,3)); }
}
