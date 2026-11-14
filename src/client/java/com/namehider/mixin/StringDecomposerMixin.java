package com.namehider.mixin;

import com.namehider.NHText;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Весь текст, нарисованный обычной строкой (экраны, F3, списки и т.п.), проходит здесь.
 * Если мод не найдёт эти методы, он просто пропустит подмену (require = 0).
 */
@Mixin(StringDecomposer.class)
public abstract class StringDecomposerMixin {

    @ModifyVariable(
        method = "iterateFormatted(Ljava/lang/String;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
        at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private static String namehider$str3(String text) {
        return NHText.replaceFormatted(text);
    }

    @ModifyVariable(
        method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
        at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private static String namehider$str5(String text) {
        return NHText.replaceFormatted(text);
    }
}
