package com.voicemage.mixin;

import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Mannequin.class)
public interface MannequinAccessor {
   @Invoker("setProfile")
   void voicemage$setProfile(ResolvableProfile var1);

   @Invoker("setImmovable")
   void voicemage$setImmovable(boolean var1);
}
