# 🧙 VoiceMage

![Version](https://img.shields.io/badge/version-2.14.0-4169E1)
![Minecraft](https://img.shields.io/badge/Minecraft-26.2-2E8B57?logo=minecraft)
![Loader](https://img.shields.io/badge/Loader-Fabric-DBD0B4)
![Java](https://img.shields.io/badge/Java-25%2B-ED8B00)
![License](https://img.shields.io/badge/License-MIT-2E8B57)
[![Discord](https://img.shields.io/badge/Discord-Join%20Community-5865F2?logo=discord&logoColor=white)](https://discord.gg/cPfjZetypC)

**VoiceMage** is a Fabric mod for Minecraft 26.2 that turns your microphone into a spellbook. Say *"fireball"* and a fireball flies. Say *"meteor"* and the sky answers. Speech recognition runs **locally on your own machine**, so there is no cloud, no account and no audio ever leaves your computer. The server decides what actually happens: mana, cooldowns and spell effects.

Everything is tweakable in `voicemage.json` or live with commands: spell costs, mana regeneration, whether the Explosion needs its legendary chant, and a lot more.

---

## ✨ Features

### 🎙️ Voice Casting

* **🗣️ 14 Spoken Spells:** Say the name of a spell and it fires in the direction you are looking.
* **📴 Offline Recognition:** Powered by [Vosk](https://github.com/alphacep/vosk-api). The speech model (~40 MB) is downloaded once and then works fully offline.
* **🎤 Microphone Picker:** Choose any recording device from a list with `/voicemic`. Devices that can not record at 16 kHz mono (many USB headsets and laptop microphones) are converted automatically.
* **⚡ Instant Mute:** Press **V** to mute or unmute. The speech model stays in memory, so there is no waiting.
* **🧩 Smart Spelling:** Common variants such as "fire ball", "pit fall", "lightening", "magic missle" and "poly morph" are understood too.

### 🔮 Mana & Combat

* **💧 Mana System:** A mana pool with an on-screen bar, configurable size and regeneration.
* **⏱️ Cast Cooldown:** A short delay between casts keeps spamming in check.
* **🎯 Aimed Spells:** Lightning, Magic Missile, Meteor, Freeze, Polymorph, Shrink and Explosion go where you look.
* **🛑 No Wasted Mana:** A spell that has nothing to do (Heal at full health, Pitfall with no blocks, Shrink without a target) costs nothing.

### 🛡️ Built for Servers

* **🖥️ Server Decides:** Mana, cooldowns, range checks and every spell effect are handled on the server.
* **🛠️ Live Admin Commands:** Change mana costs, regeneration and the Explosion chant without restarting.
* **💾 Safe Config:** An unreadable `voicemage.json` is backed up instead of overwritten, and `/voicemage reload` keeps your old settings when the file is broken.
* **🧹 Self-Cleaning:** Temporary walls, ice cages, clones and polymorph animals are removed even after a crash.

---

## 🪄 The Spells

Costs are the defaults and can be changed with commands (see below).

| Say | Mana | Effect |
| :--- | ---: | :--- |
| 🔥 **Fireball** | 35 | Launches a large fireball in the direction you are looking. |
| 🕳️ **Pitfall** | 60 | Removes a 3×3 column of blocks ahead of you, all the way down. Can be disabled in the config. |
| 💚 **Heal** | 30 | Restores 4 hearts. Does nothing at full health. |
| ⚡ **Lightning** | 45 | Calls lightning down on what you aim at (up to 64 blocks). |
| 🪨 **Earth Wall** | 25 | Raises a temporary wall (5 wide, 4 high) that fades after about 8 seconds. |
| 🌀 **Blink** | 20 | Short-range teleport in the direction you are looking (up to 15 blocks). |
| ✨ **Magic Missile** | 25 | A precise bolt at the target you aim at (up to 32 blocks). |
| ☄️ **Meteor** | 70 | A meteor falls on the spot you aim at and explodes on impact. |
| 💨 **Wind Blast** | 30 | A gust of wind that damages and pushes back what is in front of you. |
| ❄️ **Freeze** | 50 | Freezes the creature you aim at in place, inside a temporary cage of ice for a few seconds. Frozen creatures can not suffocate in it, and breaking the ice or letting it melt leaves no water behind. Set `freezeSingleTarget` to `false` to freeze everything around your aim point instead. |
| 👥 **Clone** | 40 | Summons decoy clones of yourself for a few seconds. |
| 🐑 **Polymorph** | 55 | Turns the target you aim at into a random harmless animal for a few seconds. |
| 🔬 **Shrink** | 40 | Shrinks the target you aim at to about a third of its size for a few seconds. |
| 💥 **Explosion** | 100 | The ultimate spell. Needs a chant (see below). Destroys a huge area. |

### 💥 The Explosion Chant

**Explosion** costs a full mana bar and, by default, has to be chanted. Either of these works:

> *O ancient flame, awaken from your endless slumber. Let the skies burn and turn my enemies to ash, leaving nothing behind. **EXPLOSION!***

> *Darkness blacker than black and darker than dark,*
> *I beseech thee, combine with my deep crimson.*
> *Return all creation to cinders,*
> *and come from the abyss!*
> ***EXPLOSION!***

Server operators can allow casting by saying just *"Explosion"* with `/voicemage chant false`.

> ⚠️ **Heads up for server owners:** Explosion carves a very large crater (radius 38, depth 128 by default) and **Pitfall** removes blocks down to the bottom of the world. Both can seriously damage builds. Raise the costs, keep the chant required, or tune the crater size in the config before you let players loose.

---

## 🎮 Commands & Permissions

### Server commands

Commands marked **Op** need permission level 2 (game master), such as operators and command blocks.

| Command | Description | Access |
| :--- | :--- | :--- |
| `/spells` | Lists every spell with its current mana cost | Everyone |
| `/voicemage` | Shows spells, maximum mana, regeneration and whether the Explosion chant is required | Everyone |
| `/voicemage chant` | Shows whether the full Explosion chant is required | Everyone |
| `/voicemage chant show` | Shows both Explosion chants | Everyone |
| `/voicemage chant <true\|false>` | `true`: Explosion needs one of the two chants. `false`: saying only "Explosion" is enough | Op |
| `/voicemage regen` | Shows the mana regeneration rate (mana per second) | Op |
| `/voicemage regen <rate>` | Sets the regeneration rate, from `0` to `100` mana per second | Op |
| `/voicemage <spell>` | Shows the mana cost of one spell | Op |
| `/voicemage <spell> <cost>` | Sets the mana cost of one spell, from `0` to `100` | Op |
| `/voicemage reload` | Reloads `config/voicemage.json` and syncs the settings to all players | Op |

`<spell>` is one of: `fireball`, `pitfall`, `heal`, `lightning`, `earth_wall`, `blink`, `magic_missile`, `meteor`, `wind_blast`, `freeze`, `clone`, `polymorph`, `shrink`, `explosion`.

```text
/voicemage regen 10          # regenerate 10 mana per second
/voicemage fireball 20       # make Fireball cost 20 mana
/voicemage chant false       # just say "Explosion" to cast it
/voicemage reload            # apply manual edits to voicemage.json
```

### Client commands (only affect you)

| Command | Description |
| :--- | :--- |
| `/voicemic` | Lists every recording device Java can see, numbered, and marks the one in use |
| `/voicemic <number>` | Switches to that device and restarts voice recognition |
| `/voicemic default` | Goes back to the system default microphone |

Your choice is saved in `config/voicemage-client.json`. If the saved device is not connected, the system default is used.

### Keybind

| Key | Action |
| :--- | :--- |
| **V** | Mute / unmute voice recognition (rebind under *Options → Controls → voicemage*) |

---

## ⚙️ Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) 0.19.3 or newer for Minecraft 26.2, and the Fabric API.
2. Make sure you run **Java 25** or newer.
3. Drop `voicemage-26.2-<version>.jar` into the `mods` folder of **both the server and every player**.
4. Start the game and join a world or server.
5. **First launch only:** VoiceMage downloads the English speech model (`vosk-model-small-en-us-0.15`, about 40 MB) into `<game directory>/voicemage/`. The download is checked against a built-in size and SHA-256 checksum and thrown away if it does not match.
6. Allow microphone access if your operating system asks. When the HUD shows **Voice: listening**, you are ready.

Players without the mod can not cast, and the server needs the mod to run the spells.

---

## 🧠 How It Works

Every spell goes through the same three steps:

1. **Listen:** Your own client hears you. Vosk only knows the spell names and the chants, so random talk is mostly ignored.
2. **Ask:** The client tells the server *which* spell you want. No audio is ever sent.
3. **Decide:** The server checks that you are alive, off cooldown and have the mana, then runs the effect and takes the mana. If the spell can not work (no target, nothing to heal), it costs nothing.

The HUD shows your mana bar and a voice status:

| Status | Meaning |
| :--- | :--- |
| **Voice: listening** | Microphone and recognition are active |
| **Voice: loading** | The speech model is downloading or starting up |
| **Voice error: ...** | No usable microphone, or the model failed to load. Try `/voicemic` |
| **Voice: muted** | Recognition is switched off. Press **V** to resume |

---

## 🧩 Configuration

The config lives in `config/voicemage.json` and is created on first start. Edit it and run `/voicemage reload`, or change the main values in-game with the commands above. Values outside the allowed range are clamped automatically.

```json
{
  "maximumMana": 100.0,
  "manaRegenerationPerSecond": 5.0,
  "minimumCastIntervalTicks": 6,
  "explosionChantRequired": true,
  "pitfallEnabled": true,
  "fireballManaCost": 35
}
```

If the file can not be read (for example after a typo), VoiceMage keeps a copy as `voicemage.json.broken-<date>-<time>` next to it instead of silently overwriting your settings.

<details>
<summary><b>📋 All options</b></summary>

<br>

| Option | Default | Description |
| :--- | ---: | :--- |
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
| `freezeSingleTarget` | true | Freeze only the creature you aim at (`false`: everything within `freezeRadius` of your aim point) |
| `freezeRadius` / `freezeDurationTicks` | 4 / 60 | Search area around your aim point, and freeze duration |
| `cloneCount` / `cloneDurationTicks` | 3 / 120 | Number of clones (2–3) and lifetime |
| `polymorphDurationTicks` / `shrinkDurationTicks` / `shrinkScale` | 100 / 120 / 0.35 | Polymorph and Shrink duration; Shrink size factor |
| `skillShotRange` | 32 | Aim range for Polymorph and Shrink |
| `explosionPower` / `explosionRange` | 96 / 64 | Explosion strength and aim range |
| `explosionCraterRadius` / `explosionCraterDepth` | 38 / 128 | Size of the Explosion crater |

</details>

---

## 🛠️ Compatibility

* **Minecraft Version:** `26.2`
* **Mod Loader:** Fabric (Loader 0.19.3+) with the Fabric API
* **Java Version:** Java 25+
* **Environment:** Client **and** server (voice runs on the client, spells run on the server)
* **Languages:** The speech model is English only

### Good to know

* The server can not hear your microphone. It decides mana, cooldowns and effects, but whether the **Explosion chant** was spoken is checked by the player's own client.
* The **microphone** needs a working input device that Java can use. On Linux, make sure PulseAudio or PipeWire exposes an input device.
* Temporary things are cleaned up even after a crash: leftover clones and polymorph animals are removed when their chunk loads, and temporary walls and ice are tracked in `voicemage_temporary_blocks.json` in the world folder and removed on the next start.

---

## 🩺 Troubleshooting

* **"Voice error: Microphone ... cannot be opened":** run `/voicemic` and try another device. The log (`latest.log`) says which device and which conversion is in use.
* **The speech model does not download:** the first launch needs internet access to `alphacephei.com`. Check your firewall or restart the game (a failed download is retried once automatically). If the log reports a checksum failure, the file was corrupted or changed and was discarded.
* **Spells are not recognized:** speak clearly and say one spell at a time. The recognizer is English only.
* **Nothing happens on a server:** the mod must be installed on the server too, and your mana must cover the spell's cost. Use `/spells` to see the current costs.

---

## 🏗️ Building From Source

You need **JDK 25**.

```bash
git clone https://github.com/Berkayprof/VoiceMage.git
cd VoiceMage
./gradlew build
```

The mod jar is written to `build/libs/voicemage-26.2-<version>.jar`. To test in a development client:

```bash
./gradlew runClient
```

<details>
<summary><b>📁 Project layout</b></summary>

```text
src/main/java/com/voicemage/
├── Voicemage.java            server/common entrypoint
├── VoicemageClient.java      client entrypoint (voice, HUD, keybind)
├── VoicemageCommands.java    /spells and /voicemage
├── VoicemageNetworking.java  packet registration
├── client/                   speech recognition, microphone, model installer, HUD and visuals
├── config/                   voicemage.json handling
├── mixin/                    Mannequin accessor (used by Clone)
├── network/                  payloads between client and server
└── spell/                    spells, mana, effects, temporary-block cleanup
```

</details>

---

## 📜 Credits & License

* **VoiceMage** was created by **Cayce** and is licensed under the [MIT License](LICENSE). The copyright notice stays with the original author.
* This version was rebuilt from the compiled mod into a Gradle project and is maintained by **Berkayprof**.
* Speech recognition uses [Vosk](https://github.com/alphacep/vosk-api) (Apache License 2.0), bundled with the mod.

---

💬 **Need Support or Have Questions?**
Join our community on Discord: **[discord.gg/cPfjZetypC](https://discord.gg/cPfjZetypC)**

---

*Original mod by **Cayce** · Maintained with ❤️ by **Berkayprof***
