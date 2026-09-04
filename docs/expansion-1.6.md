# Expansion 1.6.0
Tambahan dari screenshot yang disetujui: 41 ID baru (Haste/Alacrity satu mekanik), plus alias ke enchant lama.
Sembilan nama yang ditolak tidak ditambahkan: Vanish, Allurement, Foraging, Nether Prospector, Blast Mining, Curse of Breaklessness, Curse of Harmlessness, Fuddle, Drain.

## Daftar
| ID | Maks | Mekanik / cooldown minimum |
|---|---|---|
| abrasion | III | Durability armor target berkurang 1–3, tidak sampai pecah; cooldown 10 detik. |
| adrenaline | III | Diserang mob: Strength I 1–3 detik; cooldown 20 detik. |
| arctic_freeze | III | Hit memberi Slowness I dan satu luka tambahan tertunda; cooldown 10 detik. |
| ascend | III | Klik kanan pedang: Levitation I 1–3 detik; cooldown 30 detik. |
| aura | III | Damage pemain berkurang 3–9% saat ada rekan pemain dekat; radius 3 blok. |
| blaze_reaper | III | Bonus damage 5–15% pada mob Nether tertentu. |
| brightness | III | Bonus damage 5–15% pada Warden di cahaya rendah. |
| caffeinated | III | Hit sambil sprint memberi Haste I singkat; cooldown 15 detik. |
| carve | III | Hit utama memberi damage area kecil, maksimal 3 target radius 3; cooldown 10 detik. |
| charge | II | Klik kanan pedang sambil sneak: dorong maju di tanah; cooldown 15 detik. |
| contagion | III | Hit panah menghasilkan efek cloud visual dan damage area sekali; cooldown 15 detik. |
| cubism | III | Bonus damage 5–15% pada slime/magma cube, termasuk panah. |
| double_blow | IV | Peluang 5–20% damage trident dua kali; cooldown 15 detik. |
| end_affinity | III | Damage di End berkurang 3–9%. |
| enderbane | V | Bonus damage 4–20% pada Enderman/Ender Dragon. |
| escape | II | Setelah menerima damage: Speed I 1–2 detik; cooldown 15 detik. |
| explosive | V | Hit panah: damage area kecil tanpa merusak blok; cooldown 15 detik. |
| feather_step | V | Peluang 5–25% membatalkan fall damage; cooldown 20 detik. |
| finishing | III | Bonus damage 5–15% saat target di bawah 25% HP. |
| fire_hook | III | Hook mengenai target: api 1–3 detik; cooldown 10 detik. |
| first_strike | III | Bonus damage 5–15% terhadap target dengan HP penuh. |
| getaway | III | Menerima damage saat HP rendah: Speed I 1–3 detik; cooldown 20 detik. |
| incinerate | III | Bonus damage 5–15% pada spider/cave spider. |
| multi_shot | III | Panah tambahan 1–3 saat tembakan penuh; cooldown 15 detik; hilang setelah 3 detik. |
| nether_affinity | III | Damage di Nether berkurang 3–9%; bukan kebal lava. |
| ninja | III | Bonus damage 5–15% saat sneak. |
| poisoned_hook | III | Hook mengenai target: Poison I 1–3 detik; cooldown 10 detik. |
| postpone | III | Peluang 10–30% mengurangi knockback terhadap mob; cooldown 5 detik. |
| ravenous | IV | Hit memulihkan 1 hunger; cooldown 20–14 detik. |
| rebounding | III | Pantulkan 5–15% damage, maksimum 3 HP; cooldown 15 detik. |
| repel | III | Hit memberi dorongan horizontal kecil; cooldown 10 detik. |
| resonate | III | Pantulkan 5–15% damage, maksimum 3 HP; berbagi cooldown 15 detik dengan Rebounding. |
| rumble | III | Saat diserang: damage area kecil, maksimal 3 musuh; cooldown 15 detik. |
| scorching | III | Serangan masuk memicu api 1–3 detik pada penyerang; cooldown 10 detik. |
| sharpness_hook | IX | Hook memberi 0.25 HP per level pada target; cooldown 10 detik. |
| shura | III | Bonus critical damage 5–15% saat HP sendiri di bawah separuh. |
| skullcrusher | III | Bonus damage 5–15% pada jenis skeleton. |
| starvation | III | Hit memberi Hunger I 1–3 detik; cooldown 10 detik. |
| thor | III | Hit memicu petir visual dan bonus damage terbatas; cooldown 15 detik. |
| zombie_crusher | III | Bonus damage 5–15% pada jenis zombie. |
| haste_tool (Haste/Alacrity) | III | Break sukses dengan alat: Haste I 1–3 detik, CD 10 detik. |

## Nama alternatif
Blackout → Blind; Criticals → Critical; Flashbang → Blinding Arrow; Frost → Frost Arrow.
Infernal Touch → Auto Smelt; Replenish → Auto Farm; Waterborne → Water Breathing.
Haste/Alacrity → Haste Tool. Experience tetap enchant yang sudah ada.
Alias pada command memakai underscore, misalnya /ce give XandMe infernal_touch 1.
Mekanik alias mengikuti enchant lama; bukan salinan identik plugin di screenshot.

## Batas yang disengaja
- Rebounding dan Resonate menggunakan nilai tertinggi, bukan penjumlahan. Maksimum 3 HP, cooldown bersama minimal 15 detik. Berlaku ke penyerang mob maupun player.
- Thor: petir visual, bonus damage maksimal 3 HP, cooldown minimum 15 detik. Bukan petir nyata dan tidak membakar blok.
- Bonus damage bersyarat baru dijumlah lalu dibatasi 30%. Double Blow memiliki roll terpisah dan cooldown 15 detik.
- Contagion: satu pulse damage/visual, bukan cloud persisten.
- Explosive: satu area damage, tanpa ledakan blok.
- Carve/Rumble/Contagion/Explosive: radius 3, maksimum 3 target sekunder, perlu line of sight, damage melalui event Bukkit agar protection plugin dapat menolak.
- Multi-Shot: 1–3 panah tambahan saat full draw, CD 15 detik, umur 3 detik, tidak dapat dipungut, maksimum global 96 panah. Tidak memicu enchant damage Veliora tambahan.
- Arctic Freeze: satu damage tertunda, bukan task berulang; batal jika pemain keluar atau target jauh.
- Secondary damage diberi guard agar tidak memicu rantai pantulan/efek Veliora.
- Fire Hook/Poisoned Hook/Sharpness Hook adalah combat hook, bukan pengganda ikan. Debuff hanya setelah damage hook diterima.
- Aura diperiksa saat damage, tidak memindai dunia setiap tick. Pemain dekat tidak otomatis dianggap anggota team: siapa pun selain penyerang dapat memenuhi kondisi.
- Caffeinated memakai Haste I singkat; bukan perubahan attack-speed attribute permanen.
- Ascend/Charge: hanya di tanah, tidak sedang terbang/naik kendaraan; tidak teleport menembus blok.
- Abrasion tidak menghancurkan armor dan tidak merusak item unbreakable.
- Tidak ada task scan global atau tambahan drop dari ekspansi ini.

## Config / pemasangan
Stop server, backup, ganti VelioraEnchant.jar, start kembali. Versi ini 1.6.0.
Config lama tidak dihapus. Entri enabled/max-level/cooldown-ticks ditambahkan saat startup.
cooldown-ticks -1 = bawaan. Cooldown mekanik ekspansi tidak boleh diturunkan di bawah batas minimum; dapat diperpanjang.
Untuk pantulan, nilai maksimum konfigurasi Rebounding/Resonate dipakai sebagai cooldown bersama.
Nama buku tetap tanpa simbol/label rarity pada judul, deskripsi serta item target terdapat di lore.
Enchant yang kuat diberi bobot table lebih rendah; tidak menjamin enchant tertentu.
Contoh admin: /ce give XandMe thor 3 ; /ce give XandMe sharpness_hook 9.
Level di atas I juga dapat digabung lewat anvil sesuai batas katalog.

## Verifikasi
Tes otomatis: semua ID terdaftar, alias, pengecualian, item target, batas level, batas pantulan; build Maven.
Belum diuji gameplay server Java/Bedrock. Perlu uji khusus claim/PvP, friendly-fire, event plugin lain, anvil, damage hook dan mobilitas.
Cooldown dan batas entity mengurangi risiko beban; bukan jaminan TPS tanpa pengukuran di server.

