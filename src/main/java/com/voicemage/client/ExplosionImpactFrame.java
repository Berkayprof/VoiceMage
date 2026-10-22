package com.voicemage.client;

import com.voicemage.network.SpellVisualPayload;
import com.voicemage.spell.Spell;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public final class ExplosionImpactFrame {
   private static final int TOTAL_TICKS = 10;
   private static final int TEXTURE_WIDTH = 320;
   private static final int TEXTURE_HEIGHT = 180;
   private static final Identifier[] FRAMES = new Identifier[]{
      texture("explosion_impact_0.png"), texture("explosion_impact_1.png"), texture("explosion_impact_2.png")
   };
   private static int ticksRemaining;
   private static float intensity;

   private ExplosionImpactFrame() {
   }

   public static void accept(Minecraft client, SpellVisualPayload payload) {
      if (payload.spellId() == Spell.EXPLOSION.networkId() && payload.phaseTicks() == 0 && client.level != null) {
         trigger(client, new Vec3(payload.x(), payload.y(), payload.z()));
      }
   }

   private static void trigger(Minecraft client, Vec3 impact) {
      if (client.player != null) {
         double distance = client.player.position().distanceTo(impact);
         if (!(distance > 176.0)) {
            intensity = (float)Math.clamp(1.0 - (distance - 36.0) / 210.0, 0.34, 1.0);
            ticksRemaining = 10;
         }
      }
   }

   public static void tick(Minecraft client) {
      if (ticksRemaining > 0) {
         ticksRemaining--;
      }
   }

   public static void clear() {
      ticksRemaining = 0;
      intensity = 0.0F;
   }

   public static void render(GuiGraphicsExtractor graphics, Minecraft client) {
      if (ticksRemaining > 0 && client.player != null) {
         float strength = intensity * ((Double)client.options.screenEffectScale().get()).floatValue();
         if (!(strength <= 0.01F)) {
            int age = 10 - ticksRemaining;
            int width = graphics.guiWidth();
            int height = graphics.guiHeight();
            int background;
            int frame;
            int frameAlpha;
            if (age == 0) {
               background = argb(strength, 255, 250, 226);
               frame = 0;
               frameAlpha = alpha(strength);
            } else if (age <= 2) {
               background = argb(strength, 5, 0, 1);
               frame = age - 1;
               frameAlpha = alpha(strength);
            } else if (age <= 4) {
               background = argb(strength * 0.96F, 22, 0, 2);
               frame = 2;
               frameAlpha = alpha(strength);
            } else if (age == 5) {
               background = argb(strength * 0.72F, 255, 236, 198);
               frame = 1;
               frameAlpha = alpha(strength * 0.82F);
            } else {
               float fade = (10 - age) / 4.0F;
               background = argb(strength * fade * 0.48F, 86, 0, 4);
               frame = 2;
               frameAlpha = alpha(strength * fade * 0.62F);
            }

            graphics.fill(0, 0, width, height, background);

            int shakeX = switch (age) {
               case 1 -> -3;
               case 2 -> 3;
               case 3 -> -2;
               default -> 0;
            };

            int shakeY = switch (age) {
               case 1 -> 2;
               case 2 -> -2;
               case 3 -> 1;
               default -> 0;
            };
            graphics.blit(RenderPipelines.GUI_TEXTURED, FRAMES[frame], shakeX, shakeY, 0.0F, 0.0F, width, height, 320, 180, 320, 180, frameAlpha);
         }
      }
   }

   private static Identifier texture(String name) {
      return Identifier.fromNamespaceAndPath("voicemage", "textures/gui/" + name);
   }

   private static int alpha(float amount) {
      return Math.clamp(Math.round(amount * 255.0F), 0, 255) << 24 | 16777215;
   }

   private static int argb(float amount, int red, int green, int blue) {
      int a = Math.clamp(Math.round(amount * 255.0F), 0, 255);
      return a << 24 | red << 16 | green << 8 | blue;
   }
}
