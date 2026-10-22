package com.voicemage.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record ManaSyncPayload(int manaTenths, int maxManaTenths, int spellId, int result) implements CustomPacketPayload {
   public static final int QUIET = 0;
   public static final int CAST = 1;
   public static final int NO_MANA = 2;
   public static final int COOLDOWN = 3;
   public static final int FAILED = 4;
   public static final Type<ManaSyncPayload> TYPE = new Type(Identifier.fromNamespaceAndPath("voicemage", "mana_sync"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ManaSyncPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      ManaSyncPayload::manaTenths,
      ByteBufCodecs.VAR_INT,
      ManaSyncPayload::maxManaTenths,
      ByteBufCodecs.VAR_INT,
      ManaSyncPayload::spellId,
      ByteBufCodecs.VAR_INT,
      ManaSyncPayload::result,
      ManaSyncPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
