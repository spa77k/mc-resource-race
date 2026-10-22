# ResourceRace

English | [日本語](README.ja.md) | [简体中文](README.zh-CN.md) | [한국어](README.ko.md)

A Paper plugin that turns a freshly reset resource world into a race. Right after the weekly reset you start a race from the console, and the first players to collect the target item get spa coins.

- One race at a time. Starting a new race ends the running one first.
- The target item is drawn from the candidates configured for that world.
- Rewards are paid once per rank through Vault, and the paid state is saved, so a restart never pays the same person twice.
- Ranks are announced in chat, and the winner also sees a title.
- Only items picked up inside the target world during the race are marked and counted.
- Console and RCON only for starting and stopping. Players can view the race with `/race`.

## Requirements

- Paper 1.21.4 or later (tested on Paper 26.2)
- Java 21 or later (whatever your server version needs)
- [Vault](https://www.spigotmc.org/resources/vault.34315/) and an economy plugin (EssentialsX is assumed)

## Installation

1. Download `resourcerace-1.0.0.jar` from [Releases](https://github.com/spa77k/ResourceRace/releases).
2. Put it in your server's `plugins/` folder next to Vault.
3. Restart the server. `plugins/ResourceRace/config.yml` is created.
4. List your resource worlds under `worlds`, then restart the server.
5. After the reset, run `race start <world>` from the console or RCON.

## Commands

| Command | Who | What it does |
| --- | --- | --- |
| `/race` | everyone | Shows the target, world, deadline, your progress and the standings |
| `/race start <world>` | `resourcerace.admin` | Starts a race. Ends a running race first |
| `/race stop` | `resourcerace.admin` | Ends the running race |
| `/race reload` | `resourcerace.admin` | Reloads `config.yml` |

`resourcerace.admin` defaults to op. The command works from the console and RCON, and the world does not have to be loaded yet.

### Output

| Output | Meaning |
| --- | --- |
| `RESOURCE_RACE_OK started world=<world> item=<MATERIAL> amount=<n> race=<race-id>` | The race started |
| `RESOURCE_RACE_ERROR world-not-configured` | The world is not in `worlds` |
| `RESOURCE_RACE_ERROR no-targets` | Every candidate for the world was invalid |
| `RESOURCE_RACE_OK stopped` | The race was ended |
| `RESOURCE_RACE_ERROR no-race` | There was no race to end |
| `RESOURCE_RACE_OK reloaded` | `config.yml` was reloaded |

## Configuration

`plugins/ResourceRace/config.yml`

| Key | Default | Description |
| --- | --- | --- |
| `worlds` | three resource worlds | Worlds you can race in. Each world has a `display-name` and a list of `targets`. |
| `worlds.<world>.targets[].item` | | Material name of a candidate. |
| `worlds.<world>.targets[].amount` | | How many to collect. |
| `rewards` | `[1000, 500, 300]` | Spa coins per rank. The number of entries is the number of ranks. |
| `duration-hours` | `167` | How long a race runs, in hours. Keep it a little under 7 days so it does not overlap the next reset. |

When a race starts, one candidate is drawn from that world's `targets`. Candidates with an unknown Material name or an `amount` below 1 are warned about and skipped at load time.

The race and its standings are saved in `plugins/ResourceRace/state.yml`.

## How items are marked

An item is marked when it is acquired inside the target world while the race is running. Only marked items of the target Material, from the current race and owned by you, count toward your progress. Your inventory storage and offhand count; your ender chest does not.

Counted:

- Drops from blocks you broke (unless the block is a container, or you placed that block yourself).
- Drops from mobs and other non-player entities.
- Loot generated in chests and other containers.
- Items you catch while fishing.
- Items from piglin bartering.
- Items taken from, or dropped by breaking, a naturally generated item frame.
- Furnace output when the input carries a mark.
- Crafting output when every non-empty ingredient carries the same owner's mark.

Furnaces and crafting only carry over the ingredients' mark, so they mark the output even outside the race world. Rankings are checked in any world.

Not counted:

- Anything obtained outside the target world.
- Drops from blocks you placed yourself.
- Drops from containers you broke.
- Drops from mobs that picked up any item.
- Items in an item frame you placed or filled yourself.
- Items with no mark, another player's mark, or a mark from an older race.
- Items in your ender chest.

Marks from older races are removed when you log in, when you open an inventory, and when the race ends, so ordinary items can stack again afterwards.

## License

[MIT](LICENSE)
