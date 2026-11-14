package com.namehider.mixin;

import com.namehider.NHMatch;
import com.namehider.NHText;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

/** Заменяет ники в любом видимом тексте-компоненте (чат, таб, неймтег, скорборд...). */
@Mixin(targets = "net.minecraft.network.chat.contents.PlainTextContents$LiteralContents")
public abstract class LiteralContentsMixin {

    @Inject(
        method = "visit(Lnet/minecraft/network/chat/FormattedText$ContentConsumer;)Ljava/util/Optional;",
        at = @At("HEAD"), cancellable = true)
    private <T> void namehider$visit(FormattedText.ContentConsumer<T> out,
                                     CallbackInfoReturnable<Optional<T>> cir) {
        String text = ((PlainTextContents.LiteralContents) (Object) this).text();
        String hidden = NHText.replaceAll(text);
        if (!hidden.equals(text)) cir.setReturnValue(out.accept(hidden));
    }

    @Inject(
        method = "visit(Lnet/minecraft/network/chat/FormattedText$StyledContentConsumer;Lnet/minecraft/network/chat/Style;)Ljava/util/Optional;",
        at = @At("HEAD"), cancellable = true)
    private <T> void namehider$visitStyled(FormattedText.StyledContentConsumer<T> out, Style style,
                                           CallbackInfoReturnable<Optional<T>> cir) {
        String text = ((PlainTextContents.LiteralContents) (Object) this).text();
        List<NHMatch.Hit> hits = NHText.find(text);
        if (hits.isEmpty()) return;

        int last = 0;
        for (NHMatch.Hit h : hits) {
            if (h.start() > last) {
                Optional<T> r = out.accept(style, text.substring(last, h.start()));
                if (r.isPresent()) { cir.setReturnValue(r); return; }
            }
            Style s = NHText.obfuscate(h.own()) ? style.withObfuscated(true) : style;
            Optional<T> r = out.accept(s, NHText.replacement(h.own()));
            if (r.isPresent()) { cir.setReturnValue(r); return; }
            last = h.end();
        }
        if (last < text.length()) {
            Optional<T> r = out.accept(style, text.substring(last));
            if (r.isPresent()) { cir.setReturnValue(r); return; }
        }
        cir.setReturnValue(Optional.empty());
    }
}
