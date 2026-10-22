package com.voicemage.spell;

import com.mojang.serialization.Codec;
import com.voicemage.Voicemage;
import com.voicemage.config.VoicemageConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

final class ManaSavedData extends SavedData {
   private static final Codec<ManaSavedData> CODEC = Codec.STRING.listOf().fieldOf("players").xmap(ManaSavedData::new, ManaSavedData::encode).codec();
   static final SavedDataType<ManaSavedData> TYPE = new SavedDataType(
      Identifier.fromNamespaceAndPath("voicemage", "player_mana"), ManaSavedData::new, CODEC, DataFixTypes.SAVED_DATA_STRUCTURE_FEATURE_INDICES
   );
   private final Map<UUID, Double> mana = new HashMap<>();

   private ManaSavedData() {
   }

   private ManaSavedData(List<String> encoded) {
      for (String entry : encoded) {
         int separator = entry.indexOf(124);
         if (separator > 0) {
            try {
               this.mana.put(UUID.fromString(entry.substring(0, separator)), Double.parseDouble(entry.substring(separator + 1)));
            } catch (IllegalArgumentException ignored) {
               Voicemage.LOGGER.warn("Ignored malformed saved mana entry: {}", entry);
            }
         }
      }
   }

   double get(UUID player) {
      return Math.max(0.0, Math.min(VoicemageConfig.INSTANCE.maximumMana, this.mana.getOrDefault(player, VoicemageConfig.INSTANCE.maximumMana)));
   }

   void put(UUID player, double value) {
      double normalized = Math.max(0.0, Math.min(VoicemageConfig.INSTANCE.maximumMana, value));
      Double previous = this.mana.put(player, normalized);
      if (previous == null || Math.abs(previous - normalized) >= 0.001) {
         this.setDirty();
      }
   }

   private List<String> encode() {
      List<String> encoded = new ArrayList<>(this.mana.size());
      this.mana.forEach((player, value) -> encoded.add(player + "|" + value));
      return encoded;
   }
}
