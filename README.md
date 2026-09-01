# VelioraEnchant

Custom enchantment plugin for Veliora Gardens, rebuilt for modern Paper.

## Included

- Safe over-level vanilla enchant merging in an anvil.
- V2 mechanics: Wind Burst and Shield Resistance.
- Selected V1 mechanics ported safely: Lifesteal, Auto Smelt, Telepathy, and Vein Miner.
- Custom books apply through an anvil and preserve multiple custom enchantments on an item.

Requires Paper 1.21.8+ and Java 21. Configure every cap, cooldown, and multiplier in `config.yml`.

## Commands

`/venchant`, `/ce`, `/enchants`, `/customenchant`, `/customenchants`, `/overenchant`, `/enchantshop`, and `/es` use the same safe command handler.

Use `/ce give <player> <enchant> <level>` to give a book, or `/ce reload` after editing the configuration.

Use `/ce menu` to browse the fishing-rod catalogue safely. It is a read-only menu: books cannot be taken from it. Roll a fishing book with `/ce rodroll <player>`, then combine it with a fishing rod in an anvil.

## Fishing rarity

Common 55%, Rare 28%, Epic 12%, Legendary 4.5%, Mythic 0.45%, Secret 0.05%. These values and every per-level modifier are configurable in `config.yml`.

`vanish` is intentionally not available in VelioraEnchant because permanent invisibility is not balanced for the server.
