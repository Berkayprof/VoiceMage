package com.voicemage.spell;

import java.util.Locale;

public enum Spell {
   FIREBALL(0, "fireball", "Fireball"),
   PITFALL(1, "pitfall", "Pitfall"),
   HEAL(2, "heal", "Heal"),
   LIGHTNING(3, "lightning", "Lightning"),
   EARTH_WALL(4, "earth wall", "Earth Wall"),
   BLINK(5, "blink", "Blink"),
   MAGIC_MISSILE(6, "magic missile", "Magic Missile"),
   METEOR(7, "meteor", "Meteor"),
   WIND_BLAST(8, "wind blast", "Wind Blast"),
   FREEZE(9, "freeze", "Freeze"),
   CLONE(10, "clone", "Clone"),
   POLYMORPH(11, "polymorph", "Polymorph"),
   SHRINK(12, "shrink", "Shrink"),
   EXPLOSION(13, "o ancient flame awaken from your endless slumber let the skys burn and turn my enemies to ash leaving nothing behind explosion", "Explosion");

   public static final String ALTERNATIVE_CHANT = "darkness blacker than black and darker than dark i beseech thee combine with my deep crimson return all creation to cinders and come from the abyss explosion";
   public static final String PRIMARY_CHANT_TEXT = "O ancient flame, awaken from your endless slumber. Let the skys burn and turn my enemies to ash, leaving nothing behind. EXPLOSION!";
   public static final String ALTERNATIVE_CHANT_TEXT = "Darkness blacker than black and darker than dark,\nI beseech thee, combine with my deep crimson.\nReturn all creation to cinders,\nand come frome the abyss!\nEXPLOSION!";
   private final int networkId;
   private final String phrase;
   private final String displayName;

   Spell(int networkId, String phrase, String displayName) {
      this.networkId = networkId;
      this.phrase = phrase;
      this.displayName = displayName;
   }

   public int networkId() {
      return this.networkId;
   }

   public String phrase() {
      return this.phrase;
   }

   public String displayName() {
      return this.displayName;
   }

   public static Spell byNetworkId(int id) {
      for (Spell spell : values()) {
         if (spell.networkId == id) {
            return spell;
         }
      }

      return null;
   }

   public static Spell byPhrase(String text) {
      return byPhrase(text, true);
   }

   public static Spell byPhrase(String text, boolean chantRequired) {
      String normalized = normalize(text);
      if (!normalized.replace("frome", "from")
            .equals(
               "darkness blacker than black and darker than dark i beseech thee combine with my deep crimson return all creation to cinders and come from the abyss explosion"
            )
         && (chantRequired || !normalized.equals("explosion"))) {
         for (Spell spell : values()) {
            if (spell.phrase.equals(normalized)) {
               return spell;
            }
         }
         return switch (normalized) {
            case "fire ball" -> FIREBALL;
            case "pit fall" -> PITFALL;
            case "lightening" -> LIGHTNING;
            case "icewall", "ice wall" -> EARTH_WALL;
            case "earthwall" -> EARTH_WALL;
            case "teleport" -> BLINK;
            case "magic missle" -> MAGIC_MISSILE;
            case "windblast" -> WIND_BLAST;
            case "poly morph" -> POLYMORPH;
            case "o ancient flame awaken from your endless slumber let the skies burn and turn my enemies to ash leaving nothing behind explosion" -> EXPLOSION;
            default -> null;
         };
      } else {
         return EXPLOSION;
      }
   }

   private static String normalize(String text) {
      return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z ]", " ").trim().replaceAll("\\s+", " ");
   }
}
