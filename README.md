# voicemage

**Cast spells by speaking their names.** voicemage is a Fabric mod for Minecraft 26.2 that turns your microphone into a spellbook. Say *"fireball"* and a fireball flies. Say *"meteor"* and the sky answers. Speech recognition runs locally on your machine, and the server decides whether the spell actually happens.

- **14 voice spells**, each with its own mana cost
- **Offline speech recognition** powered by [Vosk](https://github.com/alphacep/vosk-api) (no cloud, no account)
- **Server-authoritative**: clients only say *which* spell they want; the server checks mana, cooldowns and everything else
- **Mana system** with an on-screen mana bar and configurable regeneration
- **Fully configurable** through commands and `config/voicemage.json`

---

## Requirements

| | |
|---|---|
| Minecraft | 26.2 |
| Java | 25 or newer |
| Fabric Loader | 0.19.3 or newer |
| Fabric API | required |
| Microphone | required on the **client** that wants to cast |

Install the mod on **both the server and the clients**. Players without the mod cannot cast, and the server needs it to run the spells.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 26.2 and the Fabric API.
2. Put `voicemage-26.2-<version>.jar` in your `mods` folder (client and server).
3. Start the game and join a world or server.
4. **First launch only:** voicemage downloads an English speech model (`vosk-model-small-en-us-0.15`, about 40 MB) and stores it in `<game directory>/voicemage/`. After that it works fully offline.
5. Allow microphone access if your operating system asks. When the HUD shows **Voice: listening**, you're ready.

## How to play

1. Look at your target and say the name of a spell out loud.
2. The client recognizes the phrase and sends the spell to the server.
3. If you have enough mana and the spell can work, it fires and the mana is spent.

Press **V** (default) to mute or unmute voice recognition. You can rebind it under *Options → Controls → voicemage*.

The HUD shows your mana bar and a voice status:

| Status | Meaning |
|---|---|
| Voice: listening | Microphone and recognition are active |
| Voice: loading | The speech model is downloading or starting up |
| Voice: unavailable | No microphone found, or the model failed to load |

A spell does nothing (and costs no mana) when it has no valid effect, for example **Heal** at full health, or **Pitfall** with no blocks to remove. If you cast too fast, or lack mana, the cast is rejected and you get feedback on screen.

## Spells

Costs are the defaults and can be changed with commands (see below).

| Say | Mana | Effect |
|---|---:|---|
| **Fireball** | 35 | Launches a large fireball in the direction you're looking. |
| **Pitfall** | 60 | Removes a 3×3 column of blocks ahead of you, all the way down. Can be disabled in the config. |
| **Heal** | 30 | Restores 4 hearts. Does nothing at full health. |
| **Lightning** | 45 | Calls lightning down on what you aim at (up to 64 blocks). |
| **Earth Wall** | 25 | Raises a temporary wall (5 wide, 4 high) that fades after about 8 seconds. |
| **Blink** | 20 | Short-range teleport in the direction you're looking (up to 15 blocks). |
| **Magic Missile** | 25 | A precise bolt at the target you aim at (up to 32 blocks). |
| **Meteor** | 70 | A meteor falls on the spot you aim at and explodes on impact. |
| **Wind Blast** | 30 | A gust of wind that damages and pushes back what's in front of you. |
| **Freeze** | 50 | Freezes nearby creatures in place for a few seconds. |
| **Clone** | 40 | Summons decoy clones of yourself for a few seconds. |
| **Polymorph** | 55 | Turns the target you aim at into a random harmless animal for a few seconds. |
| **Shrink** | 40 | Shrinks the target you aim at to about a third of its size for a few seconds. |
| **Explosion** | 100 | The ultimate spell. Requires a chant (see below). Destroys a huge area. |

Common mis-hearings such as "fire ball", "pit fall", "lightening", "teleport" (Blink) and "poly morph" are recognized too.

### Explosion

**Explosion** costs a full mana bar and, by default, has to be chanted. Either of these works:

> O ancient flame, awaken from your endless slumber. Let the skys burn and turn my enemies to ash, leaving nothing behind. **EXPLOSION!**

> Darkness blacker than black and darker than dark,
> I beseech thee, combine with my deep crimson.
> Return all creation to cinders,
> and come frome the abyss!
> **EXPLOSION!**

Server operators can allow casting by saying just *"Explosion"* with `/voicemage chant false`.

> ⚠️ **Heads up for server owners:** Explosion carves a very large crater (radius 38, depth 128 by default) and **Pitfall** removes blocks down to the bottom of the world. Both can seriously damage builds. Raise the chant requirement, increase the costs, or tune the crater size in the config before you let players loose.

## Commands

Commands marked **Op** need permission level 2 (game master), such as operators and command blocks. Everyone can use the others.

| Command | Access | Description |
|---|---|---|
| `/spells` | Everyone | Lists every spell with its current mana cost. |
| `/voicemage` | Everyone | Shows the spells, maximum mana, regeneration rate and whether the Explosion chant is required. |
| `/voicemage chant` | Everyone | Shows whether the full Explosion chant is required. |
| `/voicemage chant show` | Everyone | Shows both Explosion chants. |
| `/voicemage chant <true\|false>` | Op | `true`: Explosion requires one of the two chants. `false`: saying only "Explosion" is enough. |
| `/voicemage regen` | Op | Shows the mana regeneration rate (mana per second). |
| `/voicemage regen <rate>` | Op | Sets the regeneration rate, from `0` to `100` mana per second. Saved to the config. |
| `/voicemage <spell>` | Op | Shows the mana cost of one spell. |
| `/voicemage <spell> <cost>` | Op | Sets the mana cost of one spell, from `0` to `100`. Saved to the config. |
| `/voicemage reload` | Op | Reloads `config/voicemage.json` and syncs the settings to all players. |

`<spell>` is one of: `fireball`, `pitfall`, `heal`, `lightning`, `earth_wall`, `blink`, `magic_missile`, `meteor`, `wind_blast`, `freeze`, `clone`, `polymorph`, `shrink`, `explosion`.

**Examples**

```text
/voicemage regen 10          # regenerate 10 mana per second
/voicemage fireball 20       # make Fireball cost 20 mana
/voicemage chant false       # just say "Explosion" to cast it
/voicemage reload            # apply manual edits to voicemage.json
```

## Configuration

The config lives in `config/voicemage.json` and is created on first start. Edit it and run `/voicemage reload`, or change the main values in-game with the commands above. Values outside the allowed range are clamped automatically.

| Option | Default | Description |
|---|---:|---|
| `maximumMana` | 100 | Size of the mana pool |
| `manaRegenerationPerSecond` | 5 | Mana regained per second |
| `minimumCastIntervalTicks` | 6 | Minimum delay between two casts (20 ticks = 1 second) |
| `explosionChantRequired` | true | Require a full chant for Explosion |
| `pitfallEnabled` | true | Allow the Pitfall spell |
| `<spell>ManaCost` | see table | Mana cost per spell (e.g. `fireballManaCost`) |
| `fireballExplosionPower` | 4 | Fireball blast strength |
| `healAmount` | 8 | Health restored by Heal (2 = 1 heart) |
| `lightningDamage` / `lightningRange` | 16 / 64 | Lightning damage and range |
| `earthWallWidth` / `earthWallHeight` / `earthWallLifetimeSeconds` | 5 / 4 / 8 | Earth Wall size and lifetime |
| `blinkRange` | 15 | Blink distance in blocks |
| `magicMissileDamage` / `magicMissileRange` | 10 / 32 | Magic Missile damage and range |
| `meteorExplosionPower` / `meteorRange` | 8 / 48 | Meteor blast strength and aim range |
| `windBlastRange` / `windBlastDamage` / `windBlastStrength` | 12 / 2 / 2.8 | Wind Blast reach, damage and push |
| `freezeRadius` / `freezeDurationTicks` | 4 / 60 | Freeze area and duration |
| `cloneCount` / `cloneDurationTicks` | 3 / 120 | Number of clones (2–3) and lifetime |
| `polymorphDurationTicks` / `shrinkDurationTicks` / `shrinkScale` | 100 / 120 / 0.35 | Polymorph and Shrink duration; Shrink size factor |
| `skillShotRange` | 32 | Aim range for Polymorph and Shrink |
| `explosionPower` / `explosionRange` | 96 / 64 | Explosion strength and aim range |
| `explosionCraterRadius` / `explosionCraterDepth` | 38 / 128 | Size of the Explosion crater |

## Troubleshooting

- **"Voice: unavailable"**: check that your operating system has a working default microphone and that Java is allowed to use it. On Linux, make sure PulseAudio or PipeWire exposes an input device.
- **The speech model doesn't download**: the first launch needs internet access to `alphacephei.com`. Check your firewall, or restart the game to try again. The model is stored in `<game directory>/voicemage/`.
- **Spells aren't recognized**: speak clearly and say one spell at a time. The recognizer is English only.
- **Nothing happens on a server**: the mod must be installed on the server too, and your mana must cover the spell's cost. Use `/spells` to see the current costs.

## Building from source

You need **JDK 25**.

```bash
git clone <this repository>
cd voicemage
./gradlew build
```

The mod jar is written to `build/libs/voicemage-26.2-<version>.jar`. To test in a development client:

```bash
./gradlew runClient
```

Project layout:

```text
src/main/java/com/voicemage/
├── Voicemage.java           server/common entrypoint
├── VoicemageClient.java     client entrypoint (voice, HUD, keybind)
├── VoicemageCommands.java   /spells and /voicemage
├── VoicemageNetworking.java packet registration
├── client/                  speech recognition, model installer, HUD and visuals
├── config/                  voicemage.json handling
├── mixin/                   Mannequin accessor (used by Clone)
├── network/                 payloads between client and server
└── spell/                   spells, mana, effects
```

## Credits and licenses

- voicemage is licensed under the [MIT License](LICENSE). Author: Caden.
- Speech recognition uses [Vosk](https://github.com/alphacep/vosk-api) (Apache License 2.0), bundled with the mod.
- See [THIRD_PARTY_NOTICES.md](src/main/resources/META-INF/THIRD_PARTY_NOTICES.md) for details.
