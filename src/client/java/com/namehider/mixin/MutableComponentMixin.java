package com.namehider.mixin;

import com.namehider.NHText;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Игра запоминает готовый текст компонента навсегда. Из-за этого ник, показанный до включения
 * скрытия (или до того, как игрок попал в список), так и оставался видимым. Пока что-то скрывается,
 * держим свой кэш, который сбрасывается при каждой смене списка ников/настроек.
 * Если в этой версии игры метод называется иначе, подмена просто пропускается (require = 0).
 */
@Mixin(MutableComponent.class)
public abstract class MutableComponentMixin {

    @Unique private FormattedCharSequence namehider$seq;
    @Unique private Language namehider$lang;
    @Unique private int namehider$gen = -1;

    @Inject(method = "getVisualOrderText", at = @At("HEAD"), cancellable = true, require = 0)
    private void namehider$visual(CallbackInfoReturnable<FormattedCharSequence> cir) {
        if (!NHText.active()) return;
        Language lang = Language.getInstance();
        int gen = NHText.generation();
        if (namehider$seq == null || namehider$lang != lang || namehider$gen != gen) {
            namehider$seq = lang.getVisualOrder((FormattedText) (Object) this);
            namehider$lang = lang;
            namehider$gen = gen;
        }
        cir.setReturnValue(namehider$seq);
    }
}
