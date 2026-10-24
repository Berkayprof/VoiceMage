package com.voicemage.spell;

import com.voicemage.VoicemageNetworking;
import com.voicemage.config.VoicemageConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SpellManager {
   private final Map<UUID, SpellManager.PlayerMana> players = new HashMap<>();
   private final SpellEffects effects = new SpellEffects();
   private MinecraftServer activeServer;

   public double maxMana() {
      return Math.floor(VoicemageConfig.INSTANCE.maximumMana);
   }

   public double mana(ServerPlayer player) {
      return this.state(player).mana;
   }

   /** @return true if a player broke one of our temporary blocks and it was removed cleanly (cancel the vanilla break) */
   /** @return true while the creature is frozen by Freeze (it can not suffocate in the ice) */
   public boolean isFrozen(LivingEntity entity) {
      return this.effects.isFrozen(entity);
   }

   public boolean handleTemporaryBreak(ServerLevel level, BlockPos pos, BlockState state) {
      return this.effects.breakTemporary(level, pos, state);
   }

   public void started(MinecraftServer server) {
      this.effects.recoverJournal(server);
   }

   public void restoreMana(ServerPlayer player, int amount) {
      SpellManager.PlayerMana state = this.state(player);
      state.mana = Math.min(this.maxMana(), state.mana + Math.max(0, amount));
      if (state.mana >= this.maxMana()) {
         state.regenerationProgress = 0.0;
      }

      saved(player.level().getServer()).put(player.getUUID(), state.mana);
      this.sync(player, state, -1, 0);
   }

   public void join(ServerPlayer player) {
      SpellManager.PlayerMana state = this.state(player);
      state.lastSentTenths = -1;
      this.sync(player, state, -1, 0);
   }

   public void tick(MinecraftServer server) {
      if (this.activeServer != server) {
         this.players.clear();
         this.activeServer = server;
      }

      this.effects.tick(server);

      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
         SpellManager.PlayerMana state = this.state(player);
         double maximum = this.maxMana();
         if (state.mana > maximum) {
            state.mana = maximum;
         }

         if (state.mana < maximum) {
            state.regenerationProgress = state.regenerationProgress + VoicemageConfig.INSTANCE.manaRegenerationPerSecond / 20.0;
            int gained = (int)Math.floor(state.regenerationProgress + 1.0E-9);
            state.regenerationProgress = Math.max(0.0, state.regenerationProgress - gained);
            state.mana = Math.min(maximum, state.mana + gained);
         }

         if (state.mana >= maximum) {
            state.regenerationProgress = 0.0;
         }

         int tenths = (int)Math.round(state.mana * 10.0);
         if (server.getTickCount() % 4 == 0 && tenths != state.lastSentTenths) {
            this.sync(player, state, -1, 0);
         }
      }

      if (server.getTickCount() % 20 == 0) {
         this.saveAll(server);
      }
   }

   public void cast(ServerPlayer player, int networkId) {
      Spell spell = Spell.byNetworkId(networkId);
      if (spell != null && player.isAlive() && !player.isSpectator()) {
         SpellManager.PlayerMana state = this.state(player);
         long now = player.level().getGameTime();
         if (now - state.lastCastTick < VoicemageConfig.INSTANCE.minimumCastIntervalTicks) {
            this.sync(player, state, spell.networkId(), 3);
         } else {
            int cost = VoicemageConfig.INSTANCE.manaCost(spell);
            if (state.mana + 1.0E-4 < cost) {
               this.sync(player, state, spell.networkId(), 2);
            } else if (!this.effects.cast(player, spell)) {
               this.sync(player, state, spell.networkId(), 4);
            } else {
               state.mana -= cost;
               state.lastCastTick = now;
               saved(player.level().getServer()).put(player.getUUID(), state.mana);
               this.sync(player, state, spell.networkId(), 1);
            }
         }
      }
   }

   public void leave(ServerPlayer player) {
      SpellManager.PlayerMana state = this.players.remove(player.getUUID());
      if (state != null) {
         saved(player.level().getServer()).put(player.getUUID(), state.mana);
      }
   }

   public void shutdown(MinecraftServer server) {
      this.saveAll(server);
      this.effects.restoreAll(server);
      this.players.clear();
      this.activeServer = null;
   }

   public void saveAll(MinecraftServer server) {
      ManaSavedData data = saved(server);
      this.players.forEach((player, state) -> data.put(player, state.mana));
   }

   private SpellManager.PlayerMana state(ServerPlayer player) {
      return this.players.computeIfAbsent(player.getUUID(), id -> new SpellManager.PlayerMana(saved(player.level().getServer()).get(id)));
   }

   private static ManaSavedData saved(MinecraftServer server) {
      return (ManaSavedData)server.overworld().getDataStorage().computeIfAbsent(ManaSavedData.TYPE);
   }

   private void sync(ServerPlayer player, SpellManager.PlayerMana state, int spellId, int result) {
      state.lastSentTenths = (int)Math.round(state.mana * 10.0);
      VoicemageNetworking.sync(player, state.mana, spellId, result);
   }

   private static final class PlayerMana {
      private double mana;
      private double regenerationProgress;
      private long lastCastTick = -4611686018427387904L;
      private int lastSentTenths = -1;

      private PlayerMana(double mana) {
         this.mana = Math.floor(mana + 1.0E-9);
      }
   }
}
