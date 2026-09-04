# Five new enchants — VelioraEnchant 1.4.0

| ID / name | Item | Maximum | Rarity label / table weight | Default effect |
|---|---|---|---|---|
| `second_wind` / Second Wind | Chestplate only | III | Legendary / 0.75 | Crossing from above 25% HP to at most 25%, while surviving, grants Regeneration I for 3/4/5 seconds. 90-second cooldown. Not a resurrection. |
| `steadfast` / Steadfast | Leggings only | III | Rare / 5 | While sneaking, attack/damage knockback is reduced by 10/20/30%. No explosion, push or shield-block reduction. |
| `riposte` / Riposte | Shield | III | Epic / 2.5 | A successful directional shield block arms one attack for 4 seconds. A full-strength melee hit gets +0.5/1/1.5 raw damage (before armor). 10-second cooldown. |
| `careful_hands` / Careful Hands | Pickaxe, axe, shovel, hoe | III | Rare / 5 | Preserve the last 1/2/3 durability; block breaking, right-click tool actions and melee attacks stop at that reserve. Repair to resume. Does not repair the item or multiply drops. |
| `patient_angler` / Patient Angler | Fishing rod | V | Epic / 2.5 | After 5 successful non-rare catches, add `(misses - 4) * level * 0.2%` relative rare-weight bonus, capped at 10%. Reset after a rare result. |

Table weights are relative to compatible candidates, **not fixed drop percentages**.
New IDs participate in the existing enchanting table candidate selection and anvil system.
Patient Angler also participates in the existing Epic fishing-book roll/menu. Other four
are not added to the fishing-book pool. Names have no rarity brackets or decorative symbols.
Do not install old and new copies of the same plugin together.

## Admin test books

Replace `PLAYER` with the player's actual name:

```text
/ce give PLAYER second_wind 3
/ce give PLAYER steadfast 3
/ce give PLAYER riposte 3
/ce give PLAYER careful_hands 3
/ce give PLAYER patient_angler 5
```

Put equipment left and the corresponding book right in an anvil. Equal-level books
can be combined up to the maximum. Wrong armor slots are rejected. The new runtime
effects clamp malformed levels to III/V even if an item was edited externally.
`custom-enchants.<id>.enabled` and `.max-level` are configurable. Second Wind and Riposte
also honor `.cooldown-ticks`; `-1` means their built-in defaults. Existing config values
are preserved; defaults are added at normal plugin startup. Restart for the new JAR;
`/ce reload` reloads configuration only.

## Patient Angler integration

- Without Suite: only a legitimate open-water catch of an ordinary vanilla fish can
  be replaced by one vanilla treasure-table item. Replacement chance is `0.05 * bonus`,
  at most **0.5% per ordinary-fish catch**, never an extra item. Existing treasure counts
  as a rare result. This is a standalone fallback, not a complete rewrite of vanilla luck.
- With the updated Suite fishing hook: ORNAMENTAL/EPIC/LEGENDARY weights gain at most
  10% relative bonus; MITOLOGI/SECRET gain at most 2.5%. Zero/gated weights stay zero.
  Existing luck, rod requirements and the minigame remain in charge. Only successfully
  awarded minigame/direct catches advance/reset the streak; failed minigames do not.
- Streaks and defensive cooldowns use player persistent data, so reconnecting does not
  clear them. Streak is capped at 100; record updates have a two-second guard.
- An old Suite version cannot use the new hook. Update **both JARs** for Suite fishing.
  VelioraEnchant itself still runs without Suite. Neither feature guarantees a Secret.

## Verification and limits

Pure rule tests cover equipment restrictions, durability reserves, low-health crossing,
knockback cap and patience cap. Suite tests cover rarity gates and relative multipliers.
No live Java/Bedrock server test has been performed. Test non-OP Java/Bedrock players,
shield direction, combat protection cancellation, anvil XP/material cost, fishing in
open water, minigame failure/success and custom-durability tools before production use.
Careful Hands cannot control another plugin that directly deletes items or edits damage
without firing Bukkit events. Existing unrelated enchant mechanics are unchanged.

These additions do not introduce repeating scans or periodic tasks. Second Wind and
standalone catch recording use one deferred task per qualifying event.
