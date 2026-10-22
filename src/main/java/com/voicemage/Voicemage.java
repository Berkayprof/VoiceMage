package com.voicemage;

import com.voicemage.config.VoicemageConfig;
import com.voicemage.spell.SpellManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Join;
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
      ServerLifecycleEvents.SERVER_STOPPING.register(SPELLS::shutdown);
      LOGGER.info("voicemage 2.13.2 loaded: fourteen server-authoritative voice spells ready.");
   }
}
