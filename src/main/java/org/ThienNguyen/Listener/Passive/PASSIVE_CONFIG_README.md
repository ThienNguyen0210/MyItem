# Passive Ability YAML Config — Reference for AI Config Generation

This document describes the YAML schema for a Minecraft plugin's "Passive Ability" system, which attaches special effects to items via `.yml` files (no coding required). Use this reference to generate valid passive config files — **not** for plugin development/source work.

## How it works

1. A game event fires (hit, kill, take damage, break block, or death).
2. The plugin scans the player's 6 equipment slots (helmet, chestplate, leggings, boots, main hand, off hand) for attached passive IDs.
3. `condition` is checked — any failure skips the passive for that event.
4. A random roll is compared against `chance`.
5. `cooldown` is checked (per-player, per-passive).
6. All `actions` execute in order.

**Performance:** the plugin caches the list of passive IDs on a player's gear and only rescans when the player changes an item — attaching passives has no meaningful performance cost even under heavy combat.

## File structure

Each passive lives in its own `.yml` file inside:

```
plugins/YourPlugin/Listener/Passives/
  execute_5pct.yml
  speed_on_hit.yml
  kill_drop_gold.yml
  ...
```

The filename doesn't matter — the internal `id` key is what's used. Reload with `/reload` or the plugin's own reload command.

Passives are attached to an item by writing the `id`(s) into the item's persistent data under the key `passive_ids`, via the plugin's item editor. Multiple passives are comma-separated: `execute_5pct,speed_on_hit`.

## Top-level keys

| Key | Required | Description |
|---|---|---|
| `id` | **Yes** | Unique identifier. Must match the ID stored on the item. No spaces. |
| `display-name` | No | Name shown in tooltips/debug. Supports `&`-color codes. |
| `trigger` | **Yes** | Event that fires the passive. See Triggers below. |
| `chance` | No | 0–100 probability. Default `100`. Supports expressions. |
| `cooldown` | No | Per-player cooldown in seconds. Default `0` (none). Starts counting *after* actions finish executing. |
| `condition` | No | Condition block (see below). |
| `actions` | **Yes** | List of actions to execute. |

## Triggers

All uppercase.

| Trigger | Fires when |
|---|---|
| `ON_HIT` | Player deals damage to an entity |
| `ON_TAKE_DAMAGE` | Player takes damage from any source |
| `ON_KILL` | Player lands the killing blow |
| `HP_THRESHOLD` | Victim's HP drops below a % threshold (requires a separate listener — verify with server admin before use) |
| `ON_DEATH` | The passive's owning player dies, any cause |
| `ON_BLOCK_BREAK` | Player breaks a block |

Notes:
- **ON_DEATH**: `actor` = the player who died (owner). `victim` in context = whoever killed them (may be `null` for environmental deaths). Use `target: VICTIM` in a `DAMAGE` action for "revenge on death" effects.
- **ON_BLOCK_BREAK**: no victim/combat damage exists; conditions like `must-be-crit` or `{victim_hp}` fail safely. Respects WorldGuard/claims — if the BlockBreakEvent is cancelled, the passive does not run.

## Conditions

All conditions under `condition:` must pass; any single failure skips the passive.

**`target-type`** — filters the affected entity type:

| Value | Behavior |
|---|---|
| `BOTH` | Any entity, player or mob (default) |
| `PLAYER` | Only when victim is another player |
| `MOB` | Only when victim is a non-player mob |
| `SELF` | Only when actor and victim are the same player (self-damage) |

**`must-be-crit`**: `true` to only fire on critical hits.

**`target-hp-percent-below`**: only fires if victim's current HP is below this % of max HP. Supports expressions.

**`expressions`**: list of arbitrary comparisons; all must be true. Supported operators: `>=` `<=` `==` `!=` `>` `<`.

Internal placeholders usable in expressions/conditions:

| Placeholder | Value |
|---|---|
| `{damage}` | Damage dealt in this event |
| `{actor_level}` | Player's current XP level |
| `{actor_hp}` | Player's current HP |
| `{actor_max_hp}` | Player's max HP |
| `{actor_missing_hp}` | Player's missing HP (Max HP − Current HP) |
| `{victim_hp}` | Victim's current HP |
| `{victim_max_hp}` | Victim's max HP |
| `{victim_missing_hp}` | Victim's missing HP (Max HP − Current HP) |
| `{victim_hp_percent}` | Victim's HP as % of max |
| `%any_papi%` | Any PlaceholderAPI placeholder |

Example:
```yaml
condition:
  target-type: BOTH
  must-be-crit: true
  target-hp-percent-below: "20"
  expressions:
    - "%player_level% >= 10"
    - "{damage} > 5.0"
```

> **Known limitation:** each entry in `expressions` supports exactly one `LEFT OP RIGHT` comparison — a single operator per string, checked in this order: `>=`, `<=`, `==`, `!=`, `>`, `<`. Expressions do **not** support comparing two placeholders against each other in a reliable way, and do not support chaining multiple comparisons or logical AND/OR inside one string (e.g. `"10 <= %x% <= 20"` or `"%a% > %b% && %c% < 5"` are not valid). To require multiple conditions, add separate entries to the `expressions` list — they are already ANDed together. Keep each entry to one placeholder/expression on the left and one literal (or simple expression) on the right.

## Expressions & PlaceholderAPI

`chance`, `duration-seconds`, `amount`, and most numeric keys accept math expressions instead of fixed numbers, including PlaceholderAPI and internal placeholders:

```yaml
chance: "50"                    # fixed 50%
chance: "%player_level% * 2"    # 2% per level, auto-clamped 0–100
chance: "{damage} * 3"          # higher damage = higher chance
amount: "%player_level% * 0.5"
duration-seconds: "{damage} / 2"
```
Chance auto-clamps to 0–100 (a result of 200 is treated as 100).

## Chance & Cooldown

- `chance`: integer or expression. `"100"` = always fires, `"0"` = never fires. The roll happens **after** conditions are checked and **before** the cooldown check.
- `cooldown`: seconds, per-player, per-passive. `0` = no cooldown. The timer starts counting only **after** the passive finishes executing all of its actions.

## Actions

Declared as a YAML list under `actions:`. Every action needs a `type`. Most support `target: SELF | VICTIM`.

Action types: `DAMAGE`, `EFFECT`, `BUFF_STAT`, `HEAL`, `FLAME`, `LAUNCH`, `EXPLODE`, `SUMMON_TNT`, `DROP_ITEM`, `COMMAND`, `MESSAGE`, `SOUND`, `PARTICLE_ANIMATION`, `PARTICLE_PROJECTILE`, `BREAK_AREA`, `DELAY`, `REPEAT`, `HIT_COUNTER`, `STACK_COUNTER`, `ADD_VALUE`, `CHECK_VALUE`, `SUMMON_VANILLA`, `SUMMON_MM`, `TITLE`, `STATUS`, `MYTHIC_SKILL`, `LIGHTNING`, `TARGET_FILTER`, `REVIVE`.

### DAMAGE
| Key | Default | Description |
|---|---|---|
| target | VICTIM | SELF or VICTIM |
| amount | 0 | Damage amount. Expression-capable |
| damage-type | TRUE | NORMAL = fires Bukkit event, reduced by armor. TRUE = true damage, sets HP directly |

### EFFECT
Applies a vanilla potion effect.
| Key | Default | Description |
|---|---|---|
| target | SELF | SELF or VICTIM |
| effect | — | Bukkit PotionEffectType name (SPEED, STRENGTH, SLOWNESS, POISON, REGENERATION, etc.) |
| seconds | 5 | Duration. Expression-capable |
| level | 1 | Effect amplifier level (1 = level I). Expression-capable |
| ambient | false | Beacon-style subtle particles |
| particles | true | false hides potion particles |

> EFFECT uses vanilla potion effects. BUFF_STAT (below) changes internal plugin stats instead. For real movement speed, use EFFECT with `effect: SPEED`.

### BUFF_STAT
Temporarily buffs an internal plugin stat (added immediately, reverted after duration). Only affects players.
| Key | Default | Description |
|---|---|---|
| target | SELF | SELF or VICTIM (must resolve to a player) |
| stat | — | See Stat List below |
| amount | 0 | Buff amount (negative = nerf). Expression-capable |
| duration-seconds | 5 | Seconds until buff is removed. Expression-capable |

### HEAL
| Key | Default | Description |
|---|---|---|
| target | SELF | SELF or VICTIM |
| amount | 0 | Flat heal. Expression-capable |
| percent | 0 | % of max HP; takes priority over `amount` if > 0. Expression-capable |

### FLAME
Sets target on fire, dealing damage per second while burning.
| Key | Default | Description |
|---|---|---|
| target | VICTIM | SELF or VICTIM |
| damage-per-second | 0 | Expression-capable |
| duration-seconds | 5 | Expression-capable |
| visual-fire | true | false hides visible flame but damage still applies |

### LAUNCH
Knocks the entity along a free vector based on `reference`'s facing direction.
| Key | Default | Description |
|---|---|---|
| target | VICTIM | Entity being launched |
| reference | SELF | Entity used to compute direction |
| forward | 0 | Forward force (negative = pushback). Expression-capable |
| side | 0 | Sideways force. Expression-capable |
| up | 0 | Upward force. Expression-capable |
| reset-velocity | true | false adds to current velocity instead of overwriting |

### EXPLODE
| Key | Default | Description |
|---|---|---|
| target | VICTIM | SELF, VICTIM, or BLOCK (ON_BLOCK_BREAK only) |
| power | 4.0 | Vanilla explosion power (knockback/sound). Expression-capable |
| radius | 4.0 | Entity search radius for damage. Expression-capable |
| amount | 0 | Damage per entity hit. Expression-capable |
| damage-type | TRUE | NORMAL or TRUE |
| include-self | false | true = passive owner also takes damage |
| break-blocks | false | true = explosion breaks blocks (respects WorldGuard) |
| particle-scale | 1.0 | Multiplier for explosion particle size/spread |

### SUMMON_TNT
Spawns one or more primed TNT entities at the target location.
| Key | Default | Description |
|---|---|---|
| target | VICTIM | SELF/ACTOR, VICTIM, or BLOCK (ON_BLOCK_BREAK only) |
| mode | FIXED | FALL (drops from above), SHOOT (launches outward in a random direction), or FIXED (appears in place) |
| amount | 1 | Number of TNT entities to spawn. Expression-capable |
| power | 4.0 | Explosion power, only used if `destroy-blocks` is true. Expression-capable |
| fuse-ticks | 80 | Ticks before detonation (80 = 4s). Expression-capable |
| destroy-blocks | false | true = explosion breaks blocks; false = entities only |
| fall-height | 10.0 | (FALL mode) blocks above the origin the TNT spawns at. Expression-capable |
| shoot-speed | 1.0 | (SHOOT mode) launch speed. Expression-capable |
| shoot-spread-angle | 45.0 | (SHOOT mode) max upward launch angle in degrees, randomized per TNT |
| radius | 1.0 | (FIXED mode) scatter radius when `amount` > 1 |

```yaml
- type: SUMMON_TNT
  target: VICTIM
  mode: SHOOT
  amount: "3"
  fuse-ticks: "60"
  destroy-blocks: false
  shoot-speed: "1.2"
  shoot-spread-angle: "60"
```

> The TNT's source is set to the passive's owner, so kill credit/attribution works normally. `destroy-blocks: false` still lets the blast damage/knock back entities — it only suppresses terrain destruction.

### DROP_ITEM
| Key | Default | Description |
|---|---|---|
| target | VICTIM | SELF or VICTIM |
| material | GOLD_NUGGET | Bukkit material name |
| amount | 1 | Supports ranges (`"1-3"`), expressions, PAPI; ranges also support expressions (`"1-%player_level%"`) |

### COMMAND
General-purpose escape hatch for anything without a dedicated mechanic (give items, economy, other plugins, etc).
| Key | Default | Description |
|---|---|---|
| target | SELF | Entity substituted for `{player}` |
| mode | OP | OP = console runs with full permissions. PLAYER = target runs it, limited by their permissions |
| command | — | Command to run, no leading `/` |

Placeholders in `command`: `{player}` (target name), `{actor}` (owner name), `{damage}` (1 decimal).

### MESSAGE
Sends a private chat message. Only works if target resolves to a player.
| Key | Default | Description |
|---|---|---|
| target | SELF | SELF or VICTIM |
| message | — | Supports `&`-color codes |

Placeholders: `{actor_name}`, `{victim_name}`, `{damage}`.

> The canonical content key for this action is `message` (per the table above). When generating configs, always use `message` — don't substitute `text`.

### SOUND
| Key | Default | Description |
|---|---|---|
| target | SELF | SELF or VICTIM (location to play at) |
| sound | — | Bukkit Sound enum name |
| volume | 1.0 | |
| pitch | 1.0 | 0.5–2.0 |

### PARTICLE_ANIMATION
Circular/spherical particle effect around a target over time. Supports offsetting the effect's center relative to the target's height and facing direction.
| Key | Default | Description |
|---|---|---|
| target | SELF | SELF or VICTIM |
| particle | FLAME | Bukkit particle type |
| shape | CIRCLE | CIRCLE (flat ring) or SPHERE (3D) |
| radius | 1.0 | Expression-capable |
| height | 1.0 | Vertical (Y-axis) offset measured from the target's feet. Expression-capable |
| forward-offset | 0.0 | Distance to shift the center forward (positive) or backward (negative), relative to the target's facing direction. Expression-capable |
| side-offset | 0.0 | Distance to shift the center right (positive) or left (negative), relative to the target's facing direction. Expression-capable |
| points-per-tick | 12 | Expression-capable |
| particle-per-point | 1 | Expression-capable |
| duration-seconds | 3 | Expression-capable |
| update-interval-ticks | 4 | Lower = smoother but more expensive |
| rotate | true | false = static effect |
| dust-color | #FF5500 | Hex color, used when particle is DUST |
| block-material | OAK_LEAVES | Used when particle is BLOCK or FALLING_DUST |

> **Offset mechanics:** `forward-offset` and `side-offset` are recalculated every tick from the target's current horizontal facing (yaw). If the target turns or moves while the effect is running, the effect immediately follows the new facing direction instead of staying locked to the original one.

### PARTICLE_PROJECTILE
Fires a particle projectile toward the victim; on hit, deals damage and can trigger impact/trail effects.

Flight/control:
| Key | Default | Description |
|---|---|---|
| particle | FLAME | Particle type along flight path |
| particle-per-step | 1 | Expression-capable |
| speed | 1.0 | Blocks/tick. Expression-capable |
| amount | 0 | Damage on hit. Expression-capable |
| damage-type | NORMAL | NORMAL or TRUE |
| hit-radius | 1.0 | Hitbox radius. Expression-capable |
| hit-actor-self | false | true = projectile can hit its shooter |
| immediate-impact | false | true = skip flight, impact immediately at location |
| dust-color | #FFAA00 | For DUST particle |
| block-material | MAGMA_BLOCK | For BLOCK/FALLING_DUST |

Flight shape & sweep:
| Key | Default | Description |
|---|---|---|
| flight-shape | NONE | NONE or TORNADO |
| flight-radius | 0.45 | Expression-capable |
| flight-rotation-speed | 0.55 | Expression-capable |
| flight-rings | 2 | Ring count (TORNADO) |
| flight-points-per-ring | 6 | |
| flight-grow | true | Whether effect grows over time |
| flight-grow-steps | 20 | Steps to reach max size. Expression-capable |
| sweep-attack | false | Enable SWEEP_ATTACK effect along flight path |
| sweep-attack-interval | 4 | Tick interval for sweep spawn |

Impact effect:
| Key | Default | Description |
|---|---|---|
| impact-shape | NONE | NONE, CIRCLE, SPHERE, BURST, or TORNADO |
| impact-radius | 3.0 | Expression-capable |
| impact-duration-ticks | 15 | Expression-capable |
| impact-damage | same as `amount` | Damage per impact tick. Expression-capable |

> Impact damage decays over time (`0.35 / (1 + tick/5.0)`). Each entity takes impact damage at most once per 3 ticks. Impact shape only affects visuals — the damage area is always a sphere controlled by `impact-radius`.

### BREAK_AREA
Digs out extra blocks around the block just broken. Only valid with `ON_BLOCK_BREAK`. Respects WorldGuard/claims; drops respect tool enchants.
| Key | Default | Description |
|---|---|---|
| size | 3 | Edge length of dig area (3 = 3×3). Odd numbers recommended. Expression-capable |
| depth | 1 | Additional depth on Y axis. Expression-capable |
| use-tool-drops | true | true = drops respect Fortune/Silk Touch; false = default drops |
| excluded-materials | BEDROCK,BARRIER,... | Comma-separated blocks that can't be dug |

### DELAY
Waits N seconds, then runs `children`.
| Key | Default | Description |
|---|---|---|
| seconds | 1 | Wait time. Expression-capable |
| children | — | Action list, same syntax as `actions:` |

> Don't nest a DELAY as a plain action inside another DELAY/REPEAT — DELAY manages its own children, and nested children only run once after the wait.

### REPEAT
Repeats the entire `children` list, spaced `interval-seconds` apart, `times` total.
| Key | Default | Description |
|---|---|---|
| times | 1 | Expression-capable |
| interval-seconds | 1 | Expression-capable |
| children | — | Action list to repeat |

### HIT_COUNTER
Counts event occurrences and only fires `children` once the `every` threshold is reached.
| Key | Default | Description |
|---|---|---|
| every | 5 | Triggers needed to fire children. Expression-capable |
| reset-after | same as `every` | Max triggers before counter auto-resets |
| children | — | Actions to run at threshold |

### STACK_COUNTER
Tracks a per-player accumulating stack count. At defined `milestones`, runs the matching children. Supports idle-timeout auto-reset as well as auto-reset on special events (death, quit).
| Key | Default | Description |
|---|---|---|
| max-stacks | 5 | Max stacks. Expression/PAPI-capable |
| decay-seconds | 5 | Idle seconds before stack resets to 0. Expression-capable |
| trigger-at-max | false | true = re-triggering at max re-runs the max milestone (extends duration); false = stack resets to 0 ("overload") |
| on-quit | true | If true, resets the stack to 0 as soon as the player disconnects |
| on-death | true | If true, resets the stack to 0 as soon as the player dies |
| on-empty | — | Action list to run whenever the stack decays back to 0 |
| milestones | — | Map of stack-count → child action list |

```yaml
- type: STACK_COUNTER
  max-stacks: "5"
  decay-seconds: "3"
  trigger-at-max: false
  on-quit: true
  on-death: true
  on-empty:
    - type: MESSAGE
      target: SELF
      message: "&7Your stacks faded away..."
  milestones:
    "3":
      - type: EFFECT
        target: SELF
        effect: SPEED
        seconds: "3"
    "5":
      - type: EXPLODE
        target: VICTIM
        power: "4.0"
```
Each trigger refreshes the `decay-seconds` timer; reaching an exact milestone count fires its children; if the decay timer expires without a new trigger, the stack resets to 0 and `on-empty` fires; per-player state is also cleaned up automatically on logout/death when `on-quit`/`on-death` are enabled.

### ADD_VALUE
Adds a numeric amount to a keyed value stored on the target, creating it at `0` if not already present. Each trigger also resets the expiry countdown — a lighter-weight alternative to `STACK_COUNTER` when you don't need milestones, just a raw accumulating number that `CHECK_VALUE` can later read.
| Key | Default | Description |
|---|---|---|
| target | VICTIM | SELF/ACTOR or VICTIM |
| key | — | Identifier for the stored value. Shared across any passive that reads/writes the same key on the same entity |
| amount | 1 | Amount to add (negative to subtract). Expression-capable |
| duration | 0 | Seconds until the value expires and resets to nothing. `0` = never expires on its own. Expression-capable |

```yaml
- type: ADD_VALUE
  target: ACTOR
  key: "combo_stacks"
  amount: "1"
  duration: "8"
```

> Re-triggering before expiry adds to the existing total and restarts the `duration` countdown — it does not overwrite or reset the count to `amount`.

### CHECK_VALUE
Reads a keyed value previously set by `ADD_VALUE`, compares it against a threshold, and only runs the child `actions` if the comparison passes. If the key doesn't exist (never set, or expired), the mechanic simply returns without running anything.
| Key | Default | Description |
|---|---|---|
| target | VICTIM | SELF/ACTOR or VICTIM — must match the target used in the matching `ADD_VALUE` |
| key | — | Identifier to look up |
| operator | >= | Comparison operator: `>=`, `>`, `<=`, `<`, `==`, `!=` |
| value | 1 | Threshold to compare the stored value against. Expression-capable |
| consume | false | true = clear the stored value once the check passes (e.g. spend the stacks) |
| actions | — | Child action list to run if the comparison passes |

```yaml
- type: CHECK_VALUE
  target: ACTOR
  key: "combo_stacks"
  operator: ">="
  value: "5"
  consume: true
  actions:
    - type: DAMAGE
      target: VICTIM
      amount: "15"
      damage-type: TRUE
```

> Pairs with `ADD_VALUE` for combo/stack-threshold effects. `consume: true` clears the key entirely on success (use this for "spend the stacks on payoff" designs); leave it `false` if the value should keep accumulating past the threshold.

### SUMMON_VANILLA
Summons a vanilla Minecraft entity at the target location.
| Key | Default | Description |
|---|---|---|
| mob | — | EntityType name (e.g. ZOMBIE, SKELETON) |
| health | entity default | Expression-capable |
| damage | entity default | Attack damage. Expression-capable |
| speed | 1.0 | Movement speed multiplier |
| target | VICTIM | SELF or VICTIM |
| on-death | — | Optional action list run when the summoned mob dies |

### SUMMON_MM
Summons a MythicMobs entity with level/attribute overrides.
| Key | Default | Description |
|---|---|---|
| mob | — | MythicMob ID (case-sensitive) |
| level | 1 | Expression/PAPI-capable |
| health | default | Override max HP. Expression-capable |
| damage | default | Override damage. Expression-capable |
| speed | 1.0 | Multiplier (2 = 200%) |
| target | VICTIM | SELF or VICTIM |
| on-death | — | Optional action list run when the mob dies |

### TITLE
Sends a Title/Subtitle to the target player. Leave content blank to hide that part.
| Key | Default | Description |
|---|---|---|
| target | VICTIM | SELF or VICTIM (recipient) |
| title | — | Main title text; supports target's PAPI |
| subtitle | — | Subtitle text; supports target's PAPI |
| fade-in | 10 | Ticks. Expression-capable (evaluated against actor) |
| stay | 70 | Ticks. Expression-capable (actor) |
| fade-out | 20 | Ticks. Expression-capable (actor) |

> Text placeholders resolve against `target` (recipient); timing values resolve against `actor` (skill owner).

### STATUS
Applies a temporary special status: `STUN`, `ROOT`, `DISARM`, or `INVINCIBLE`. Automatically cleans up prior tasks on re-apply.
| Key | Default | Description |
|---|---|---|
| target | VICTIM | SELF or VICTIM |
| status | — | STUN, ROOT, DISARM, or INVINCIBLE |
| duration | 3 | Seconds. Number, expression, or PAPI |

- `STUN`: fully locks the target down — Slowness 255, walk speed locked to 0, a per-tick task that also locks look direction (yaw/pitch), and blocks the target from dealing damage or attacking entirely.
- `ROOT`: locks movement **only** (Slowness 255 + walk speed 0). The target can still freely look around and attack.
- `DISARM`: prevents the target from dealing damage (checked via metadata in the damage event).
- `INVINCIBLE`: temporary invulnerability, blocks all incoming damage.

### MYTHIC_SKILL
Casts a MythicMobs skill (defined under `MythicMobs/Skills/<file>.yml`) directly when the passive fires.

**External dependency:** requires the MythicMobs plugin on the server. If it's missing, the mechanic logs a single warning at startup and safely no-ops without crashing the server.

**On damage scaling:** MythicMobs doesn't expose a way to alter skill damage directly via external API. `damage-multiplier` is passed through the `power` parameter of MythicMobs' `castSkill()` call, so it only has an effect if the target skill file's mechanic has `power: true` enabled.

| Key | Default | Description |
|---|---|---|
| target | VICTIM | SELF or VICTIM — the entity the skill is cast from/at |
| skill | — | Skill name, matching an ID in `MythicMobs/Skills/` |
| damage-multiplier | 1.0 | Damage multiplier for the skill. Expression/PAPI-capable (e.g. `"%player_level% * 0.1 + 1"`) |

```yaml
- type: MYTHIC_SKILL
  target: VICTIM
  skill: "fireball_explosion"
  damage-multiplier: "%player_level% * 0.2 + 1.0"
```

### LIGHTNING
Strikes the target with lightning, with optional damage, burning, and a visual-only mode.
| Key | Default | Description |
|---|---|---|
| target | VICTIM | ACTOR or VICTIM |
| damage | 0.0 | Extra damage dealt to the target. Expression/PAPI-capable |
| causes-burning | false | true/false — whether the strike sets the target on fire |
| fire-ticks | 100 | Burn duration in ticks if `causes-burning` is true (100 ticks = 5s) |
| visual-only | false | true = only play the lightning visual/sound, without real damage or fire |

```yaml
- type: LIGHTNING
  target: VICTIM
  damage: "%player_level% * 0.5 + 4.0"
  causes-burning: true
  fire-ticks: "100"
  visual-only: false
```

> When `visual-only` is false, a real vanilla lightning bolt is used. If `causes-burning` is false, the plugin immediately extinguishes the target after the strike so it isn't set on fire unintentionally.

### TARGET_FILTER
Checks and filters the target's type (player, vanilla mob, or MythicMobs mob). Only if the target matches an entry in `types` do the child `actions` run.
| Key | Default | Description |
|---|---|---|
| target | VICTIM | SELF or VICTIM — entity to type-check |
| types | — | List of allowed target types/keywords (see below) |
| actions | — | Child action list to run if the target passes the filter |

Supported values/syntax for `types`:

| Value / syntax | Example | Description |
|---|---|---|
| `PLAYER` | `PLAYER` | Matches if the target is a Player |
| `ALL` | `ALL` | Matches any vanilla mob type |
| `ALL_MM` | `ALL_MM` | Matches any MythicMobs-spawned mob |
| `MM_<ID>` | `MM_SKELETON_KING` | Matches a specific MythicMobs internal name exactly (prefix `MM_` + mob ID). Case-insensitive |
| `<ENTITY_TYPE>` | `ZOMBIE`, `ENDERMAN` | Matches a vanilla Spigot/Paper EntityType name |

```yaml
- type: TARGET_FILTER
  target: VICTIM
  types:
    - "PLAYER"
    - "ZOMBIE"
    - "MM_SKELETON_KING"
  actions:
    - type: LIGHTNING
      target: VICTIM
    - type: MESSAGE
      target: SELF
      message: "&aHit a valid target!"
```

- Check order: Player → MythicMobs (if installed) → vanilla mobs.
- For MythicMobs targets, `ALL_MM` / `MM_<ID>` are checked before falling back to vanilla-type matching.
- If `types` is empty/missing or `actions` is empty, the mechanic logs a warning and does nothing.

### REVIVE
A "guardian" mechanic that protects the player from a killing blow. When the player would take fatal damage, that damage is cancelled, the player is saved, and a chain of child `actions` fires immediately.
| Key | Default | Description |
|---|---|---|
| target | SELF | SELF or VICTIM — who receives the protection (players only; ignored for mobs) |
| duration-seconds | 10 | How long the protection stays active. Set to `0` or omit for **permanent** protection until it triggers. Expression/PAPI-capable |
| revive-health-percent | 50 | % of max HP restored the instant the player is saved. Set to `0` or omit to leave current HP unchanged. Expression/PAPI-capable |
| actions | — | Child actions that run automatically the moment the player is revived |

Minimal example (permanent protection + brief invincibility, no timer task, no heal):
```yaml
- type: REVIVE
  target: SELF
  actions:
    - type: STATUS
      target: SELF
      status: "INVINCIBLE"
      duration: "3"
    - type: MESSAGE
      target: SELF
      message: "&d&lYour guardian spirit saved you!"
```

Full example (10s protection window + auto-heal 50% + effect):
```yaml
- type: REVIVE
  target: SELF
  duration-seconds: "10"
  revive-health-percent: "50"
  actions:
    - type: EFFECT
      target: SELF
      effect: REGENERATION
      seconds: "5"
    - type: MESSAGE
      target: SELF
      message: "&d&lYou were revived!"
```

Mechanics & notes:
- **Consumed on trigger:** the protection disappears automatically the first time it saves the player.
- **Performance:** when `duration-seconds` is unset (or `0`), no countdown task is created at all, minimizing overhead.
- **Safe re-trigger:** re-applying REVIVE while protection is already active simply refreshes the existing timer instead of stacking.
- **Auto cleanup:** if the player disconnects, the countdown task and protection state are cleaned up automatically.
- **Players only:** this mechanic is ignored if `target` resolves to a vanilla/MythicMobs mob.

## Impact Shapes (PARTICLE_PROJECTILE)

Configured via `impact-shape`. Only affects the visual — the damage area is always a sphere controlled by `impact-radius`.

| Shape | Description |
|---|---|
| `NONE` | No visual; damage still applies if `impact-damage` is set |
| `CIRCLE` | Expanding ring that rises and fades |
| `SPHERE` | Evenly expanding 3D sphere |
| `BURST` | Particles fired outward in all directions like an explosion |
| `TORNADO` | Rising spiral column for the effect's duration |

## Stat List (for BUFF_STAT)

`damage`, `pve_damage`, `pvp_damage`, `all_damage`, `bow_damage`, `magic_damage`, `true_damage`, `death_damage`, `critical_chance`, `critical_damage`, `critical_damage_reduction`, `lifesteal`, `penetration`, `armor_pen`, `accuracy`, `damage_reduction`, `armor`, `pve_defense`, `pvp_defense`, `all_defense`, `magic_defense`, `dodge_rate`, `block_rate`, `thorns`, `knockback_resistance`, `max_mana`, `mana_regen`, `health_regen`, `exp_bonus`, `movement_speed`, `health`

## Full Examples

**Speed on hit (80% chance):**
```yaml
id: speed_on_hit
display-name: "&b[Swift Strike]"
trigger: ON_HIT
condition:
  target-type: BOTH
chance: "80"
cooldown: 3
actions:
  - type: EFFECT
    target: SELF
    effect: SPEED
    seconds: "3"
    level: "2"
    particles: false
```

**Execute below 5% HP:**
```yaml
id: execute_5pct
display-name: "&c[Execute]"
trigger: ON_HIT
condition:
  target-hp-percent-below: "5"
  target-type: BOTH
chance: "100"
cooldown: 0
actions:
  - type: DAMAGE
    target: VICTIM
    amount: 99999
    damage-type: TRUE
  - type: MESSAGE
    target: SELF
    message: "&c[Execute] &7You finished off &f{victim_name}&7!"
  - type: SOUND
    target: SELF
    sound: ENTITY_PLAYER_LEVELUP
    pitch: 2.0
```

**Fire orb on crit, projectile + sphere impact:**
```yaml
id: fire_orb
display-name: "&6[Fire Orb]"
trigger: ON_HIT
condition:
  must-be-crit: true
  target-type: BOTH
chance: "30"
cooldown: 3
actions:
  - type: PARTICLE_PROJECTILE
    particle: FLAME
    particle-per-step: "3"
    speed: "1.5"
    amount: "8"
    damage-type: TRUE
    hit-radius: "1.2"
    impact-shape: SPHERE
    impact-particle: LAVA
    impact-radius: "4.0"
    impact-duration-ticks: "20"
    impact-damage: "3"
    impact-damage-type: TRUE
```

**Revenge on death — damage the killer:**
```yaml
id: revenge_on_death
display-name: "&4[Revenge]"
trigger: ON_DEATH
condition:
  target-type: PLAYER   # only when killed by a player
chance: "100"
cooldown: 0
actions:
  - type: DAMAGE
    target: VICTIM       # victim = the killer
    amount: "%player_level% * 3"
    damage-type: TRUE
  - type: MESSAGE
    target: VICTIM
    message: "&4[Revenge] &7{actor_name} struck back from beyond the grave!"
```

**Delayed explosion (2s after kill):**
```yaml
id: delayed_explode
display-name: "&e[Slow Burn]"
trigger: ON_KILL
chance: "100"
cooldown: 5
actions:
  - type: MESSAGE
    target: SELF
    message: "&e[Slow Burn] &7Charging up..."
  - type: DELAY
    seconds: "2"
    children:
      - type: EXPLODE
        target: SELF
        power: "3.0"
        radius: "5.0"
        amount: "%player_level% * 2"
        damage-type: TRUE
        include-self: false
      - type: SOUND
        target: SELF
        sound: ENTITY_GENERIC_EXPLODE
```

**TNT pickaxe — 3×3 dig on block break:**
```yaml
id: tnt_pickaxe
display-name: "&6[TNT Pickaxe]"
trigger: ON_BLOCK_BREAK
chance: "100"
cooldown: 0
actions:
  - type: BREAK_AREA
    size: "3"
    depth: "1"
    use-tool-drops: true
    excluded-materials: "BEDROCK,BARRIER"
```

**Repeating poison — 4 ticks, 1s apart:**
```yaml
id: repeat_poison
display-name: "&2[Repeat Poison]"
trigger: ON_HIT
condition:
  target-type: BOTH
  expressions:
    - "%player_level% >= 20"
chance: "25"
cooldown: 10
actions:
  - type: REPEAT
    times: "4"
    interval-seconds: "1"
    children:
      - type: EFFECT
        target: VICTIM
        effect: POISON
        seconds: "2"
        level: "2"
      - type: PARTICLE_ANIMATION
        target: VICTIM
        particle: WITCH
        shape: CIRCLE
        radius: "0.8"
        duration-seconds: "1"
```

**Gold drop — 25% on mob kill:**
```yaml
id: kill_drop_gold
trigger: ON_KILL
condition:
  target-type: MOB
chance: "25"
cooldown: 0
actions:
  - type: DROP_ITEM
    target: VICTIM
    material: GOLD_NUGGET
    amount: "1-3"
```

**Guardian angel — auto-revive with heal (STACK_COUNTER-free, single passive):**
```yaml
id: guardian_angel
display-name: "&d[Guardian Angel]"
trigger: ON_TAKE_DAMAGE
chance: "100"
cooldown: 60
actions:
  - type: REVIVE
    target: SELF
    duration-seconds: "10"
    revive-health-percent: "50"
    actions:
      - type: EFFECT
        target: SELF
        effect: REGENERATION
        seconds: "5"
      - type: MESSAGE
        target: SELF
        message: "&d&lYou were revived!"
```

**Thunder step — filtered lightning strike on zombies/players only:**
```yaml
id: thunder_step
display-name: "&e[Thunder Step]"
trigger: ON_HIT
chance: "40"
cooldown: 5
actions:
  - type: TARGET_FILTER
    target: VICTIM
    types:
      - "PLAYER"
      - "ZOMBIE"
    actions:
      - type: LIGHTNING
        target: VICTIM
        damage: "%player_level% * 0.5 + 4.0"
        causes-burning: true
        fire-ticks: "100"
```

**Combo finisher — 5 hits in 8s triggers a bonus strike:**
```yaml
id: combo_finisher
display-name: "&c[Combo Finisher]"
trigger: ON_HIT
chance: "100"
cooldown: 0
actions:
  - type: ADD_VALUE
    target: ACTOR
    key: "combo_stacks"
    amount: "1"
    duration: "8"
  - type: CHECK_VALUE
    target: ACTOR
    key: "combo_stacks"
    operator: ">="
    value: "5"
    consume: true
    actions:
      - type: DAMAGE
        target: VICTIM
        amount: "15"
        damage-type: TRUE
      - type: SOUND
        target: ACTOR
        sound: ENTITY_PLAYER_LEVELUP
      - type: MESSAGE
        target: SELF
        message: "&c[Combo Finisher] &7Unleashed!"
```

**TNT barrage on kill — scatter shrapnel TNT that doesn't touch terrain:**
```yaml
id: tnt_barrage
display-name: "&6[TNT Barrage]"
trigger: ON_KILL
chance: "50"
cooldown: 8
actions:
  - type: SUMMON_TNT
    target: VICTIM
    mode: SHOOT
    amount: "4"
    fuse-ticks: "40"
    destroy-blocks: false
    shoot-speed: "1.3"
    shoot-spread-angle: "50"
```

---

### Instructions for the AI generating a config

When asked to create a passive ability config from this reference:
1. Always include `id`, `trigger`, and `actions` (required).
2. Pick the correct `trigger` for the described event.
3. Use `condition` to scope who/what it affects.
4. Prefer expressions (`%player_level% * X`) for scaling effects over flat numbers when the user implies scaling.
5. Wrap numeric YAML values that use expressions or placeholders in quotes (e.g. `"5"`, `"%player_level% * 2"`).
6. Only use keys documented above for each action `type` — don't invent new keys.
7. For MESSAGE actions, always use the `message` key for the chat text (not `text`).
8. Output a single valid `.yml` block, ready to drop into the `Passives/` folder.
