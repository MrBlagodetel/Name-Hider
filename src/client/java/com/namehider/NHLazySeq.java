package com.namehider;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

/**
 * Готовый к рисованию текст, который сам пересчитывается, когда меняется набор скрываемых ников.
 * Игра хранит такой текст очень долго (в компонентах, табе, голограммах, табличках), и без этого
 * ник, показанный или скрытый один раз, не менялся бы до перезахода.
 */
public final class NHLazySeq implements FormattedCharSequence {
    private static final ThreadLocal<int[]> INSIDE = ThreadLocal.withInitial(() -> new int[1]);

    private final FormattedText src;
    private final Language lang;
    private FormattedCharSequence inner;
    private int gen = -1;

    public NHLazySeq(FormattedText src, Language lang) {
        this.src = src;
        this.lang = lang;
    }

    /** Идёт пересчёт: внутри него язык должен работать как обычно. */
    public static boolean inside() {
        return INSIDE.get()[0] > 0;
    }

    @Override
    public boolean accept(FormattedCharSink sink) {
        int g = NHText.generation();
        FormattedCharSequence in = inner;
        if (in == null || gen != g) {
            in = compute();
            inner = in;
            gen = g;
        }
        return in.accept(sink);
    }

    private FormattedCharSequence compute() {
        int[] d = INSIDE.get();
        d[0]++;
        try {
            return lang.getVisualOrder(NHText.rewriteText(src));
        } finally {
            d[0]--;
        }
    }
}
