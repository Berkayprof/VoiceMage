package com.voicemage;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.voicemage.config.VoicemageConfig;
import com.voicemage.spell.Spell;
import java.util.Locale;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class VoicemageCommands {
   private VoicemageCommands() {
   }

   public static void register() {
      CommandRegistrationCallback.EVENT.register((CommandRegistrationCallback)(dispatcher, access, environment) -> {
         dispatcher.register(spellListCommand());
         dispatcher.register(voicemageCommand());
      });
   }

   private static LiteralArgumentBuilder<CommandSourceStack> spellListCommand() {
      return (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("spells").executes(context -> {
         StringBuilder message = new StringBuilder("voicemage spells");

         for (Spell spell : Spell.values()) {
            message.append('\n').append(spell.displayName()).append(" — ").append(VoicemageConfig.INSTANCE.manaCost(spell)).append(" mana");
         }

         ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal(message.toString()), false);
         return Spell.values().length;
      });
   }

   private static LiteralArgumentBuilder<CommandSourceStack> voicemageCommand() {
      LiteralArgumentBuilder<CommandSourceStack> root = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("voicemage")
         .executes(
            context -> {
               VoicemageConfig config = VoicemageConfig.INSTANCE;
               String chantRule = config.explosionChantRequired ? "Explosion requires a full chant." : "Say Explosion to cast it.";
               ((CommandSourceStack)context.getSource())
                  .sendSuccess(
                     () -> Component.literal(
                        "voicemage — Fireball, Pitfall, Heal, Lightning, Earth Wall, Blink, Magic Missile, Meteor, Wind Blast, Freeze, Clone, Polymorph, Shrink, and Explosion. Maximum mana "
                           + config.maximumMana
                           + ", regeneration "
                           + config.manaRegenerationPerSecond
                           + "/second. "
                           + chantRule
                     ),
                     false
                  );
               return 1;
            }
         );
      root.then(((LiteralArgumentBuilder)Commands.literal("reload").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).executes(context -> {
         boolean loaded = VoicemageConfig.load();
         VoicemageNetworking.broadcastSettings(((CommandSourceStack)context.getSource()).getServer());
         if (!loaded) {
            ((CommandSourceStack)context.getSource())
               .sendFailure(Component.literal("config/voicemage.json could not be read. The previous settings stay active; a backup copy of the file was saved next to it."));
            return 0;
         }

         ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("Reloaded config/voicemage.json."), false);
         return 1;
      }));
      root.then(
         ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("regen").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .executes(context -> {
                  double rate = VoicemageConfig.INSTANCE.manaRegenerationPerSecond;
                  ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("Mana regeneration is " + rate + " mana/second."), false);
                  return 1;
               }))
            .then(Commands.argument("manaPerSecond", DoubleArgumentType.doubleArg(0.0, 100.0)).executes(context -> {
               double rate = DoubleArgumentType.getDouble(context, "manaPerSecond");
               VoicemageConfig.setManaRegenerationPerSecond(rate);
               ((CommandSourceStack)context.getSource())
                  .sendSuccess(() -> Component.literal("Mana regeneration set to " + rate + " mana/second and saved."), true);
               return 1;
            }))
      );

      for (Spell spell : Spell.values()) {
         root.then(spellCostCommand(spell));
      }

      root.then(chantCommand());
      return root;
   }

   private static LiteralArgumentBuilder<CommandSourceStack> spellCostCommand(Spell spell) {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                  spell.name().toLowerCase(Locale.ROOT)
               )
               .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .executes(
               context -> {
                  ((CommandSourceStack)context.getSource())
                     .sendSuccess(() -> Component.literal(spell.displayName() + ": " + VoicemageConfig.INSTANCE.manaCost(spell) + " mana"), false);
                  return 1;
               }
            ))
         .then(Commands.argument("cost", IntegerArgumentType.integer(0, 100)).executes(context -> {
            int cost = IntegerArgumentType.getInteger(context, "cost");
            VoicemageConfig.setManaCost(spell, cost);
            ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal(spell.displayName() + " now costs " + cost + " mana."), true);
            return 1;
         }));
   }

   private static LiteralArgumentBuilder<CommandSourceStack> chantCommand() {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("chant")
               .executes(
                  context -> {
                     ((CommandSourceStack)context.getSource())
                        .sendSuccess(() -> Component.literal("Explosion full chant required: " + VoicemageConfig.INSTANCE.explosionChantRequired), false);
                     return 1;
                  }
               ))
            .then(
               Commands.literal("show")
                  .executes(
                     context -> {
                        ((CommandSourceStack)context.getSource())
                           .sendSuccess(
                              () -> Component.literal(
                                 "Explosion chants\n\nO ancient flame, awaken from your endless slumber. Let the skies burn and turn my enemies to ash, leaving nothing behind. EXPLOSION!\n\nDarkness blacker than black and darker than dark,\nI beseech thee, combine with my deep crimson.\nReturn all creation to cinders,\nand come from the abyss!\nEXPLOSION!"
                              ),
                              false
                           );
                        return 2;
                     }
                  )
            ))
         .then(
            ((RequiredArgumentBuilder)Commands.argument("required", BoolArgumentType.bool()).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .executes(
                  context -> {
                     boolean required = BoolArgumentType.getBool(context, "required");
                     VoicemageConfig.INSTANCE.explosionChantRequired = required;
                     VoicemageConfig.save();
                     VoicemageNetworking.broadcastSettings(((CommandSourceStack)context.getSource()).getServer());
                     ((CommandSourceStack)context.getSource())
                        .sendSuccess(
                           () -> Component.literal(required ? "Explosion now requires either full chant." : "Explosion can now be cast by saying Explosion."),
                           true
                        );
                     return 1;
                  }
               )
         );
   }
}
