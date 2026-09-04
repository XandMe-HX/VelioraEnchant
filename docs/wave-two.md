# Wave two — VelioraEnchant 1.5.0

Empat belas enchant tambahan; lima enchant wave one tetap tersedia.
Judul buku memakai nama dan angka Romawi, tanpa simbol atau tag rarity.
Semua terdaftar pada kandidat enchanting table dan validasi item/anvil yang sama dengan katalog lainnya.

| ID / nama | Item | Maks | Efek |
|---|---|---|---|
| emberguard / Emberguard | Chestplate | III | Damage api/lava berkurang 5%/level, bukan immunity |
| soft_landing / Soft Landing | Boots | III | Selamat fall damage minimal 2: Speed I 2–4 detik; CD 10 detik |
| trailblazer / Trailblazer | Boots | II | Speed I saat berjalan di dirt path; tidak refresh selama 10 detik setelah combat |
| clear_mind / Clear Mind | Helmet | II | Blindness/Darkness berdurasi terbatas dipersingkat 15–30%; CD 30 detik |
| pursuit / Pursuit | Sword | III | Pukulan penuh berhasil: Speed I 2–4 detik; CD 10 detik |
| crippling_shot / Crippling Shot | Bow | II | Tarikan penuh: peluang 10–20% Slowness I 2–3 detik pada target; CD 8 detik |
| recoil_step / Recoil Step | Crossbow | II | Tembak sambil sneak di tanah: dorongan horizontal kecil ke belakang; CD 10 detik |
| tidal_stride / Tidal Stride | Trident | III | Hit melee/lempar saat pemain di air: Dolphins Grace 1–3 detik; CD 10 detik |
| measured_work / Measured Work | Pickaxe, axe, shovel | III | 12 break sukses: Haste I 3–5 detik; satu hitungan/detik, CD 15 detik |
| cultivator / Cultivator | Hoe | III | Panen wheat/carrot/potato/beetroot matang: peluang 10–30% memulihkan 1 hunger; CD 10 detik |
| gentle_shear / Gentle Shear | Shears | III | Peluang 10–30% menghindari durability loss, termasuk penggunaan shears lain |
| deepwater_pact / Deepwater Pact | Rod | III | Di biome deep ocean tempat pemain berdiri: bobot ikan langka +2% relatif/level |
| relic_seeker / Relic Seeker | Rod | III | Peluang satu roll relic yang sudah ada +3% relatif/level |
| secret_whisper / Secret Whisper | Rod | III | Bobot Secret +1% relatif/level; gate tier tidak dilewati |

## Fishing

Integrasi khusus ikan/relic memerlukan VelioraSuite 1.6.8.
Tanpa Suite, ketiga enchant fishing meningkatkan peluang penggantian satu ikan biasa dengan treasure vanilla di open water.
Total bonus relatif dengan Patient Angler dibatasi 20% pada baseline treasure 5% (tambahan maksimal 1 poin persentase).
Tidak menciptakan item Secret/relic buatan Suite ketika Suite tidak terpasang.
Deepwater Pact memeriksa biome pemain saat roll, bukan memindai kedalaman kolom air.
Rarity buku: Deepwater Pact Legendary, Relic Seeker Mythic, Secret Whisper Secret.
Syarat rod dan bobot nol tetap dipertahankan. Bonus bobot bukan persentase hasil akhir.

## Pengaturan dan pemasangan

Backup, stop server, ganti JAR kedua plugin, lalu start. Jangan hot-reload JAR.
Config lama dipertahankan; default enabled/max-level/cooldown-ticks setiap ID ditambahkan saat startup.
Atur custom-enchants.<id>.enabled, max-level, cooldown-ticks; -1 memakai cooldown bawaan.
Contoh admin: /ce give XandMe emberguard 3 lalu gabungkan buku dengan chestplate di anvil.
Enchant yang lebih kuat memiliki bobot table lebih rendah; bukan jaminan enchant tertentu.

## Batas dan verifikasi

Efek memakai event, tidak ada pemindaian chunk atau task per-tick tambahan.
Trailblazer memakai Speed singkat sehingga sisa efek dapat bertahan paling lama 2 detik setelah meninggalkan path/masuk combat.
Tidak menimpa potion yang lebih kuat/panjang. Clear Mind tidak menghapus efek infinite.
Tes otomatis memeriksa katalog/item, rarity pool, batas bobot dan gate fishing. Build bukan bukti tes gameplay.
Masih perlu tes server Java/Bedrock untuk protection plugin, projectile, anvil, dan fishing minigame.
