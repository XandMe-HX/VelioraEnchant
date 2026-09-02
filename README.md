# VelioraEnchant

Custom enchantment plugin for Veliora Gardens, rebuilt for modern Paper. Version 1.1.0 stores its own enchant IDs in item persistent data and runs every effect from VelioraEnchant listeners.

## Included

- Safe over-level vanilla enchant merging in an anvil.
- V2 mechanics: Wind Burst and Shield Resistance.
- Selected V1 mechanics ported safely: Lifesteal, Auto Smelt, Telepathy, and Vein Miner.
- Custom books apply through an anvil and preserve multiple custom enchantments on an item.
- Two identical custom books combine in an anvil (I + I becomes II up to the configured cap).
- Enchanting tables can roll one compatible Veliora enchant after the vanilla enchant succeeds.
- Legacy `lifesteal` and `autosmelt` books/configs are migrated to `life_steal` and `auto_smelt` without invalidating old books.

Requires Paper 1.21.8+ and Java 21. Configure every cap, cooldown, and multiplier in `config.yml`.

## Commands

`/venchant`, `/ce`, `/enchants`, `/customenchant`, `/customenchants`, `/overenchant`, `/enchantshop`, and `/es` use the same safe command handler.

Use `/ce give <player> <enchant> <level>` to give a book, or `/ce reload` after editing the configuration.

Use `/ce menu` to browse the fishing-rod catalogue safely. It is a read-only menu: books cannot be taken from it. Roll a fishing book with `/ce rodroll <player>`, then combine it with a fishing rod in an anvil.

## Fishing rarity

Common 55%, Rare 28%, Epic 12%, Legendary 4.5%, Mythic 0.45%, Secret 0.05%. These values and every per-level modifier are configurable in `config.yml`.

## Distribution safety

Structure loot and villager trades are disabled by default. Structure loot only injects one book into whitelisted vanilla structure loot tables. A Librarian or Fisherman can receive only one Veliora custom offer in its lifetime, and all normal distribution sources are hard-capped at Epic; Mythic and Secret are never distributed through loot tables or villager trades.

`vanish` is intentionally not available in VelioraEnchant because permanent invisibility is not balanced for the server.
