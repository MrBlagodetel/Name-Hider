package com.namehider.mixin;

import com.namehider.NHApply;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Любая отрисовка головы/тела берёт текстуру отсюда, поэтому головы (список игроков, меню,
 * головы-предметы) меняются вместе со скином. Если метод не найден, подмена пропускается (require = 0).
 */
@Mixin(PlayerSkin.class)
public abstract class PlayerSkinMixin {

    @Inject(method = "body", at = @At("RETURN"), cancellable = true, require = 0)
    private void namehider$body(CallbackInfoReturnable<ClientAsset.Texture> cir) {
        ClientAsset.Texture orig = cir.getReturnValue();
        ClientAsset.Texture swapped = NHApply.swapBody(orig);
        if (swapped != orig) cir.setReturnValue(swapped);
    }
}
