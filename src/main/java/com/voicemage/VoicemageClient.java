package com.voicemage;

import com.mojang.blaze3d.platform.InputConstants.Type;
import com.voicemage.client.ExplosionImpactFrame;
import com.voicemage.client.FallingSpellVisuals;
import com.voicemage.client.ManaHud;
import com.voicemage.client.VoiceRecognitionService;
import com.voicemage.network.CastSpellPayload;
import com.voicemage.network.ChantSettingsPayload;
import com.voicemage.network.FallingSpellPayload;
import com.voicemage.network.ManaSyncPayload;
import com.voicemage.network.SpellVisualPayload;
import com.voicemage.spell.Spell;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStopping;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Join;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.resources.Identifier;

public final class VoicemageClient implements ClientModInitializer {
   private static VoiceRecognitionService voice;
   private static KeyMapping toggleVoice;
   private static boolean voiceEnabled = true;
   private static int manaTenths = 1000;
   private static int maxManaTenths = 1000;
   private static int feedbackTicks;
   private static String feedback = "";

   public void onInitializeClient() {
      voice = new VoiceRecognitionService(VoicemageClient::heardSpell);
      ClientPlayNetworking.registerGlobalReceiver(
         ChantSettingsPayload.TYPE, (payload, context) -> context.client().execute(() -> voice.setExplosionChantRequired(payload.required()))
      );
      Category category = Category.register(Identifier.fromNamespaceAndPath("voicemage", "controls"));
      toggleVoice = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.voicemage.toggle_voice", Type.KEYSYM, 86, category));
      ClientPlayNetworking.registerGlobalReceiver(ManaSyncPayload.TYPE, (payload, context) -> context.client().execute(() -> acceptMana(payload)));
      ClientPlayNetworking.registerGlobalReceiver(SpellVisualPayload.TYPE, (payload, context) -> context.client().execute(() -> {
         FallingSpellVisuals.impact(payload);
         ExplosionImpactFrame.accept(context.client(), payload);
      }));
      ClientPlayNetworking.registerGlobalReceiver(
         FallingSpellPayload.TYPE, (payload, context) -> context.client().execute(() -> FallingSpellVisuals.accept(context.client(), payload))
      );
      ClientPlayConnectionEvents.JOIN.register((Join)(handler, sender, client) -> {
         if (voiceEnabled) {
            voice.start();
         }
      });
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> {
         voice.stop();
         voice.setExplosionChantRequired(true);
         ExplosionImpactFrame.clear();
         FallingSpellVisuals.clear();
      });
      ClientLifecycleEvents.CLIENT_STOPPING.register((ClientStopping)client -> voice.stop());
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         ExplosionImpactFrame.tick(client);
         FallingSpellVisuals.tick(client);
         if (feedbackTicks > 0) {
            feedbackTicks--;
         }

         for (; toggleVoice.consumeClick(); feedbackTicks = 60) {
            voiceEnabled = !voiceEnabled;
            if (voiceEnabled && client.player != null) {
               voice.start();
               feedback = "Voice recognition starting";
            } else {
               voice.stop();
               feedback = "Voice recognition muted";
            }
         }
      });
      HudElementRegistry.addLast(
         Identifier.fromNamespaceAndPath("voicemage", "mana_bar"),
         (graphics, delta) -> ManaHud.render(
            graphics, Minecraft.getInstance(), manaTenths, maxManaTenths, voice.displayStatus(), feedback, feedbackTicks, voice.isListening()
         )
      );
      HudElementRegistry.addLast(
         Identifier.fromNamespaceAndPath("voicemage", "explosion_impact_frame"),
         (graphics, delta) -> ExplosionImpactFrame.render(graphics, Minecraft.getInstance())
      );
   }

   private static void heardSpell(Spell spell, String transcript) {
      Minecraft client = Minecraft.getInstance();
      client.execute(() -> {
         if (client.player != null && client.getConnection() != null) {
            feedback = "Heard: " + displayName(spell);
            feedbackTicks = 35;
            ClientPlayNetworking.send(new CastSpellPayload(spell.networkId()));
         }
      });
   }

   private static void acceptMana(ManaSyncPayload payload) {
      manaTenths = payload.manaTenths();
      maxManaTenths = payload.maxManaTenths();
      if (payload.result() != 0) {
         Spell spell = Spell.byNetworkId(payload.spellId());
         String name = spell == null ? "Spell" : displayName(spell);

         feedback = switch (payload.result()) {
            case 1 -> name + " cast!";
            case 2 -> "Not enough mana for " + name;
            case 3 -> "Incantation too fast";
            case 4 -> name + " needs a valid target or path";
            default -> "";
         };
         feedbackTicks = payload.result() == 1 ? 45 : 65;
      }
   }

   private static String displayName(Spell spell) {
      return spell.displayName();
   }
}
