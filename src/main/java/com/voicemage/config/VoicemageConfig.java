package com.voicemage.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.voicemage.Voicemage;
import com.voicemage.spell.Spell;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class VoicemageConfig {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   public static final int MAX_SPELL_MANA_COST = 100;
   public static final double MAX_MANA_REGENERATION = 100.0;
   public static VoicemageConfig INSTANCE = new VoicemageConfig();
   public int configVersion = 6;
   public double maximumMana = 100.0;
   public double manaRegenerationPerSecond = 5.0;
   public int minimumCastIntervalTicks = 6;
   public int fireballManaCost = 35;
   public int pitfallManaCost = 60;
   public int healManaCost = 30;
   public int lightningManaCost = 45;
   public int earthWallManaCost = 25;
   public int blinkManaCost = 20;
   public int magicMissileManaCost = 25;
   public int meteorManaCost = 70;
   public int windBlastManaCost = 30;
   public int freezeManaCost = 50;
   public int cloneManaCost = 40;
   public int polymorphManaCost = 55;
   public int shrinkManaCost = 40;
   public int explosionManaCost = 100;
   public boolean explosionChantRequired = true;
   public int fireballExplosionPower = 4;
   public int pitfallSize = 3;
   public int pitfallForwardDistance = 4;
   public double healAmount = 8.0;
   public double lightningDamage = 16.0;
   public double lightningRange = 64.0;
   public int earthWallWidth = 5;
   public int earthWallHeight = 4;
   public int earthWallLifetimeSeconds = 8;
   public double blinkRange = 15.0;
   public double magicMissileDamage = 10.0;
   public double magicMissileRange = 32.0;
   public int meteorFallTicks = 80;
   public double meteorRange = 48.0;
   public float meteorExplosionPower = 8.0F;
   public double windBlastRange = 12.0;
   public double windBlastDamage = 2.0;
   public double windBlastStrength = 2.8;
   public double freezeRadius = 4.0;
   public int freezeDurationTicks = 60;
   public int cloneCount = 3;
   public int cloneDurationTicks = 120;
   public double skillShotRange = 32.0;
   public int polymorphDurationTicks = 100;
   public int shrinkDurationTicks = 120;
   public double shrinkScale = 0.35;
   public double explosionRange = 64.0;
   public int explosionFallTicks = 160;
   public double explosionFallHeight = 128.0;
   public float explosionPower = 96.0F;
   public int explosionCraterDepth = 128;
   public int explosionCraterRadius = 38;
   public boolean pitfallEnabled = true;

   private VoicemageConfig() {
   }

   public int manaCost(Spell spell) {
      return switch (spell) {
         case FIREBALL -> this.fireballManaCost;
         case PITFALL -> this.pitfallManaCost;
         case HEAL -> this.healManaCost;
         case LIGHTNING -> this.lightningManaCost;
         case EARTH_WALL -> this.earthWallManaCost;
         case BLINK -> this.blinkManaCost;
         case MAGIC_MISSILE -> this.magicMissileManaCost;
         case METEOR -> this.meteorManaCost;
         case WIND_BLAST -> this.windBlastManaCost;
         case FREEZE -> this.freezeManaCost;
         case CLONE -> this.cloneManaCost;
         case POLYMORPH -> this.polymorphManaCost;
         case SHRINK -> this.shrinkManaCost;
         case EXPLOSION -> this.explosionManaCost;
      };
   }

   public static void setManaCost(Spell spell, int cost) {
      int value = clamp(cost, 0, 100);
      switch (spell) {
         case FIREBALL:
            INSTANCE.fireballManaCost = value;
            break;
         case PITFALL:
            INSTANCE.pitfallManaCost = value;
            break;
         case HEAL:
            INSTANCE.healManaCost = value;
            break;
         case LIGHTNING:
            INSTANCE.lightningManaCost = value;
            break;
         case EARTH_WALL:
            INSTANCE.earthWallManaCost = value;
            break;
         case BLINK:
            INSTANCE.blinkManaCost = value;
            break;
         case MAGIC_MISSILE:
            INSTANCE.magicMissileManaCost = value;
            break;
         case METEOR:
            INSTANCE.meteorManaCost = value;
            break;
         case WIND_BLAST:
            INSTANCE.windBlastManaCost = value;
            break;
         case FREEZE:
            INSTANCE.freezeManaCost = value;
            break;
         case CLONE:
            INSTANCE.cloneManaCost = value;
            break;
         case POLYMORPH:
            INSTANCE.polymorphManaCost = value;
            break;
         case SHRINK:
            INSTANCE.shrinkManaCost = value;
            break;
         case EXPLOSION:
            INSTANCE.explosionManaCost = value;
      }

      save();
   }

   public static void load() {
      Path path = FabricLoader.getInstance().getConfigDir().resolve("voicemage.json");
      VoicemageConfig loaded = null;
      if (Files.isRegularFile(path)) {
         try (Reader reader = Files.newBufferedReader(path)) {
            loaded = (VoicemageConfig)GSON.fromJson(reader, VoicemageConfig.class);
         } catch (Exception exception) {
            Voicemage.LOGGER.error("Could not read {}; using safe defaults", path, exception);
         }
      }

      if (loaded != null && loaded.configVersion < 4) {
         if (loaded.explosionPower <= 32.0F) {
            loaded.explosionPower = 64.0F;
         }

         loaded.configVersion = 4;
      }

      if (loaded != null && loaded.configVersion < 5) {
         if (loaded.explosionPower <= 64.0F) {
            loaded.explosionPower = 96.0F;
         }

         if (loaded.explosionFallTicks <= 20) {
            loaded.explosionFallTicks = 70;
         }

         if (loaded.explosionCraterDepth <= 0) {
            loaded.explosionCraterDepth = 128;
         }

         if (loaded.explosionCraterRadius <= 0) {
            loaded.explosionCraterRadius = 38;
         }

         loaded.configVersion = 5;
      }

      if (loaded != null && loaded.configVersion < 6) {
         if (loaded.explosionFallTicks <= 70) {
            loaded.explosionFallTicks = 160;
         }

         if (loaded.explosionFallHeight <= 0.0) {
            loaded.explosionFallHeight = 128.0;
         }

         loaded.configVersion = 6;
      }

      INSTANCE = loaded == null ? new VoicemageConfig() : loaded;
      INSTANCE.sanitize();
      save();
   }

   public static void setManaRegenerationPerSecond(double value) {
      INSTANCE.manaRegenerationPerSecond = clamp(value, 0.0, 100.0);
      save();
   }

   public static void save() {
      Path path = FabricLoader.getInstance().getConfigDir().resolve("voicemage.json");

      try {
         Files.createDirectories(path.getParent());

         try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(INSTANCE, writer);
         }
      } catch (IOException exception) {
         Voicemage.LOGGER.warn("Could not write config {}", path, exception);
      }
   }

   private void sanitize() {
      this.maximumMana = clamp(this.maximumMana, 1.0, 10000.0);
      this.manaRegenerationPerSecond = clamp(this.manaRegenerationPerSecond, 0.0, 100.0);
      this.minimumCastIntervalTicks = clamp(this.minimumCastIntervalTicks, 0, 200);
      this.fireballManaCost = clamp(this.fireballManaCost, 0, 100);
      this.pitfallManaCost = clamp(this.pitfallManaCost, 0, 100);
      this.healManaCost = clamp(this.healManaCost, 0, 100);
      this.lightningManaCost = clamp(this.lightningManaCost, 0, 100);
      this.earthWallManaCost = clamp(this.earthWallManaCost, 0, 100);
      this.blinkManaCost = clamp(this.blinkManaCost, 0, 100);
      this.magicMissileManaCost = clamp(this.magicMissileManaCost, 0, 100);
      this.meteorManaCost = clamp(this.meteorManaCost, 0, 100);
      this.windBlastManaCost = clamp(this.windBlastManaCost, 0, 100);
      this.freezeManaCost = clamp(this.freezeManaCost, 0, 100);
      this.cloneManaCost = clamp(this.cloneManaCost, 0, 100);
      this.polymorphManaCost = clamp(this.polymorphManaCost, 0, 100);
      this.shrinkManaCost = clamp(this.shrinkManaCost, 0, 100);
      this.explosionManaCost = clamp(this.explosionManaCost, 0, 100);
      this.fireballExplosionPower = clamp(this.fireballExplosionPower, 1, 12);
      this.pitfallSize = odd(clamp(this.pitfallSize, 1, 7));
      this.pitfallForwardDistance = clamp(this.pitfallForwardDistance, 2, 12);
      this.healAmount = clamp(this.healAmount, 1.0, 100.0);
      this.lightningDamage = clamp(this.lightningDamage, 1.0, 1000.0);
      this.lightningRange = clamp(this.lightningRange, 4.0, 128.0);
      this.earthWallWidth = odd(clamp(this.earthWallWidth, 1, 11));
      this.earthWallHeight = clamp(this.earthWallHeight, 1, 8);
      this.earthWallLifetimeSeconds = clamp(this.earthWallLifetimeSeconds, 1, 300);
      this.blinkRange = clamp(this.blinkRange, 2.0, 40.0);
      this.magicMissileDamage = clamp(this.magicMissileDamage, 0.0, 100.0);
      this.magicMissileRange = clamp(this.magicMissileRange, 4.0, 128.0);
      this.meteorFallTicks = clamp(this.meteorFallTicks, 40, 200);
      this.meteorRange = clamp(this.meteorRange, 4.0, 128.0);
      this.meteorExplosionPower = (float)clamp(this.meteorExplosionPower, 1.0, 24.0);
      this.windBlastRange = clamp(this.windBlastRange, 2.0, 32.0);
      this.windBlastDamage = clamp(this.windBlastDamage, 0.0, 100.0);
      this.windBlastStrength = clamp(this.windBlastStrength, 0.1, 10.0);
      this.freezeRadius = clamp(this.freezeRadius, 1.0, 12.0);
      this.freezeDurationTicks = clamp(this.freezeDurationTicks, 10, 600);
      this.cloneCount = clamp(this.cloneCount, 2, 3);
      this.cloneDurationTicks = clamp(this.cloneDurationTicks, 20, 600);
      this.skillShotRange = clamp(this.skillShotRange, 4.0, 128.0);
      this.polymorphDurationTicks = clamp(this.polymorphDurationTicks, 20, 600);
      this.shrinkDurationTicks = clamp(this.shrinkDurationTicks, 20, 1200);
      this.shrinkScale = clamp(this.shrinkScale, 0.1, 0.9);
      this.explosionRange = clamp(this.explosionRange, 8.0, 128.0);
      this.explosionFallTicks = clamp(this.explosionFallTicks, 40, 400);
      this.explosionFallHeight = clamp(this.explosionFallHeight, 32.0, 256.0);
      this.explosionPower = (float)clamp(this.explosionPower, 8.0, 160.0);
      this.explosionCraterDepth = clamp(this.explosionCraterDepth, 16, 384);
      this.explosionCraterRadius = clamp(this.explosionCraterRadius, 8, 64);
   }

   private static int odd(int value) {
      return (value & 1) == 0 ? value - 1 : value;
   }

   private static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }

   private static double clamp(double value, double min, double max) {
      return Math.max(min, Math.min(max, value));
   }
}
