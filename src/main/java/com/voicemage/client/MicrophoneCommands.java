package com.voicemage.client;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.List;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

/** Client-side commands to pick the microphone: /voicemic, /voicemic <number>, /voicemic default. */
public final class MicrophoneCommands {
   private MicrophoneCommands() {
   }

   /** @param onChanged called after the choice was saved so that voice recognition can restart with the new device */
   public static void register(Runnable onChanged) {
      ClientCommandRegistrationCallback.EVENT
         .register(
            (dispatcher, buildContext) -> dispatcher.register(
               ClientCommands.literal("voicemic")
                  .executes(context -> list(context.getSource()))
                  .then(ClientCommands.literal("default").executes(context -> choose(context.getSource(), 0, onChanged)))
                  .then(
                     ClientCommands.argument("number", IntegerArgumentType.integer(1))
                        .executes(context -> choose(context.getSource(), IntegerArgumentType.getInteger(context, "number"), onChanged))
                  )
            )
         );
   }

   private static int list(FabricClientCommandSource source) {
      List<String> devices = MicrophoneCapture.deviceNames();
      String selected = ClientSettings.microphone();
      boolean selectedExists = !selected.isBlank() && devices.contains(selected);
      source.sendFeedback(Component.literal("Microphones (use /voicemic <number> to choose, /voicemic default for the system default):"));
      source.sendFeedback(Component.literal((selectedExists ? "  " : "> ") + "0. System default" + (selectedExists ? "" : "   <- in use")));
      if (devices.isEmpty()) {
         source.sendFeedback(Component.literal("No recording devices were found by Java Sound."));
      }

      for (int i = 0; i < devices.size(); i++) {
         boolean active = selectedExists && devices.get(i).equals(selected);
         source.sendFeedback(Component.literal((active ? "> " : "  ") + (i + 1) + ". " + devices.get(i) + (active ? "   <- in use" : "")));
      }

      if (!selected.isBlank() && !selectedExists) {
         source.sendFeedback(Component.literal("Saved microphone '" + selected + "' is not connected; the system default is used."));
      }

      return 1;
   }

   private static int choose(FabricClientCommandSource source, int number, Runnable onChanged) {
      if (number == 0) {
         ClientSettings.setMicrophone("");
         source.sendFeedback(Component.literal("Microphone set to the system default."));
      } else {
         List<String> devices = MicrophoneCapture.deviceNames();
         if (number > devices.size()) {
            source.sendFeedback(Component.literal("There is no microphone number " + number + ". Type /voicemic to see the list."));
            return 0;
         }

         String name = devices.get(number - 1);
         ClientSettings.setMicrophone(name);
         source.sendFeedback(Component.literal("Microphone set to: " + name));
      }

      onChanged.run();
      return 1;
   }
}
