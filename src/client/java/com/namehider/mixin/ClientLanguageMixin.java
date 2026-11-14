package com.namehider.mixin;

import com.namehider.NHLazySeq;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Последний шаг перед рисованием любого компонента (чат, скорборд, голограммы, неймтеги):
 * здесь у нас весь текст целиком, поэтому находятся и ники, разбитые на несколько кусков
 * (градиент по буквам), и ники, пришедшие строкой-аргументом перевода.
 * Возвращаем "ленивый" текст: он пересчитывается сам, когда меняется список ников или настройки.
 * Если в этой версии игры метод называется иначе, мод просто пропустит подмену (require = 0).
 */
@Pseudo
@Mixin(targets = "net.minecraft.client.resources.language.ClientLanguage")
public abstract class ClientLanguageMixin {

    @Inject(
        method = "getVisualOrder(Lnet/minecraft/network/chat/FormattedText;)Lnet/minecraft/util/FormattedCharSequence;",
        at = @At("HEAD"), cancellable = true, require = 0)
    private void namehider$visualOrder(FormattedText text, CallbackInfoReturnable<FormattedCharSequence> cir) {
        if (text == null || NHLazySeq.inside()) return;
        cir.setReturnValue(new NHLazySeq(text, (Language) (Object) this));
    }
}
