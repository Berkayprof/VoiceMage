package com.voicemage;

import com.voicemage.config.VoicemageConfig;
import com.voicemage.spell.SpellManager;
import com.voicemage.spell.TemporaryEntities;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Join;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Voicemage implements ModInitializer {
   public static final String MOD_ID = "voicemage";
   public static final Logger LOGGER = LoggerFactory.getLogger("voicemage");
   public static final SpellManager SPELLS = new SpellManager();

   public void onInitialize() {
      VoicemageConfig.load();
      VoicemageNetworking.register();
      ServerTickEvents.END_SERVER_TICK.register(SPELLS::tick);
      ServerPlayConnectionEvents.JOIN.register((Join)(handler, sender, server) -> {
         SPELLS.join(handler.getPlayer());
         VoicemageNetworking.syncSettings(handler.getPlayer());
      });
      VoicemageCommands.register();
      ServerPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, server) -> SPELLS.leave(handler.getPlayer()));
      ServerLifecycleEvents.SERVER_STARTED.register(SPELLS::started);
      ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(source.is(DamageTypes.IN_WALL) && SPELLS.isFrozen(entity)));
      PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> !(level instanceof ServerLevel serverLevel && SPELLS.handleTemporaryBreak(serverLevel, pos, state)));
      ServerLifecycleEvents.SERVER_STOPPING.register(SPELLS::shutdown);
      ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> TemporaryEntities.discardIfStray(entity, level.getServer()));
      LOGGER.info("voicemage 2.14.0 loaded: fourteen server-authoritative voice spells ready.");
   }
}
