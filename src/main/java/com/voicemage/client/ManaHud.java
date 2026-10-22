package com.voicemage.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ManaHud {
   private static final int WIDTH = 124;

   private ManaHud() {
   }

   public static void render(
      GuiGraphicsExtractor graphics,
      Minecraft client,
      int manaTenths,
      int maxManaTenths,
      String voiceStatus,
      String feedback,
      int feedbackTicks,
      boolean listening
   ) {
      if (client.player != null && !client.player.isSpectator()) {
         int center = graphics.guiWidth() / 2;
         int y = 20;
         int x = center - 62;
         double fraction = maxManaTenths <= 0 ? 0.0 : Math.max(0.0, Math.min(1.0, (double)manaTenths / maxManaTenths));
         int filled = (int)Math.round(120.0 * fraction);
         graphics.fill(x, y, x + 124, y + 9, -804253152);
         graphics.fill(x + 2, y + 2, x + 124 - 2, y + 7, -14410430);
         graphics.fill(x + 2, y + 2, x + 2 + filled, y + 7, -9544449);
         if (filled > 2) {
            graphics.fill(x + 2, y + 2, x + 2 + filled, y + 4, -6365953);
         }

         String mana = String.format("Mana %d / %d", manaTenths / 10, maxManaTenths / 10);
         graphics.centeredText(client.font, mana, center, y - 10, -1514241);
         if (feedbackTicks > 0 && !feedback.isEmpty()) {
            int alpha = Math.min(255, feedbackTicks * 255 / 15);
            graphics.centeredText(client.font, feedback, center, y + 13, alpha << 24 | 15591935);
         }

         String status = voiceStatus;
         if (status.length() > 58) {
            status = status.substring(0, 55) + "...";
         }

         graphics.text(client.font, status, 6, graphics.guiHeight() - 12, listening ? -7340125 : -14483, true);
      }
   }
}
