package com.namehider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Чистая логика поиска и замены ников. Здесь нет классов Minecraft,
 * поэтому её можно тестировать отдельно от игры.
 */
public final class NHMatch {
    private NHMatch() {}

    static final char SECTION = '\u00a7';

    /** Слово, похожее на ник: 1-16 символов [A-Za-z0-9_], с границами слева и справа. */
    private static final Pattern TOKEN = Pattern.compile("(?<![A-Za-z0-9_])[A-Za-z0-9_]{1,16}(?![A-Za-z0-9_])");

    public record Hit(int start, int end, boolean own) {}

    public interface Policy {
        String replacement(boolean own);
        boolean obfuscate(boolean own);
    }

    public record Seg<S>(String text, S style) {}

    /** Все вхождения известных ников (kinds: ник в нижнем регистре -> свой ли это ник). */
    public static List<Hit> find(String s, Map<String, Boolean> kinds) {
        if (s == null || s.isEmpty() || kinds.isEmpty()) return List.of();
        List<Hit> out = null;
        Matcher m = TOKEN.matcher(s);
        while (m.find()) {
            Boolean own = kinds.get(m.group().toLowerCase(Locale.ROOT));
            if (own != null) {
                if (out == null) out = new ArrayList<>();
                out.add(new Hit(m.start(), m.end(), own));
            }
        }
        return out == null ? List.of() : out;
    }

    /**
     * Замена ников в строке. Цветовые коды (§a, §l ...) при поиске игнорируются,
     * поэтому находятся и "§aSteve", и "§cS§6t§ee§bv§ae". Если formatted = true,
     * строка потом будет разобрана вместе с кодами §: вставляем §k (мерцающие знаки)
     * и после замены возвращаем то форматирование, которое было в оригинале.
     */
    public static String rewrite(String text, Map<String, Boolean> kinds, boolean formatted, Policy policy) {
        if (text == null || text.isEmpty() || kinds.isEmpty()) return text;

        String plain = text;
        int[] map = null;
        if (text.indexOf(SECTION) >= 0) {
            StringBuilder sb = new StringBuilder(text.length());
            int[] mp = new int[text.length() + 1];
            int n = 0;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == SECTION) { i++; continue; }
                mp[n++] = i;
                sb.append(c);
            }
            plain = sb.toString();
            map = mp;
        }

        List<Hit> hits = find(plain, kinds);
        if (hits.isEmpty()) return text;

        StringBuilder out = new StringBuilder(text.length() + 16);
        int last = 0;
        for (Hit h : hits) {
            int os = map == null ? h.start() : map[h.start()];
            int oe = map == null ? h.end() : map[h.end() - 1] + 1;
            out.append(text, last, os);
            String r = policy.replacement(h.own());
            if (formatted) {
                boolean inner = (oe - os) > (h.end() - h.start());
                if (policy.obfuscate(h.own())) {
                    out.append(SECTION).append('k').append(r)
                       .append(SECTION).append('r').append(activeCodes(text, oe));
                } else {
                    out.append(r);
                    if (inner) out.append(SECTION).append('r').append(activeCodes(text, oe));
                }
            } else {
                out.append(r);
            }
            last = oe;
        }
        out.append(text, last, text.length());
        return out.toString();
    }

    /** Какие коды форматирования действуют в позиции end (цвет + стили). */
    public static String activeCodes(String text, int end) {
        char color = 0;
        StringBuilder flags = new StringBuilder();
        for (int i = 0; i + 1 < end && i + 1 < text.length(); i++) {
            if (text.charAt(i) != SECTION) continue;
            char c = Character.toLowerCase(text.charAt(i + 1));
            i++;
            if (c == 'r') {
                color = 0;
                flags.setLength(0);
            } else if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')) {
                color = c;
                flags.setLength(0);
            } else if ("klmno".indexOf(c) >= 0) {
                if (flags.indexOf(String.valueOf(c)) < 0) flags.append(c);
            }
        }
        StringBuilder sb = new StringBuilder();
        if (color != 0) sb.append(SECTION).append(color);
        for (int i = 0; i < flags.length(); i++) sb.append(SECTION).append(flags.charAt(i));
        return sb.toString();
    }

    /**
     * Замена ников в тексте, разбитом на куски со своими стилями (так хранятся компоненты чата).
     * Находит ники, разбитые по буквам на несколько кусков (градиенты в голограммах и топах).
     * Возвращает null, если заменять нечего.
     */
    public static <S> List<Seg<S>> rewriteSegments(List<String> texts, List<S> styles, Map<String, Boolean> kinds,
                                                   Policy policy, BiFunction<S, Boolean, S> obfuscator) {
        if (kinds.isEmpty()) return null;
        StringBuilder joined = new StringBuilder();
        for (String t : texts) joined.append(t);
        List<Hit> hits = find(joined.toString(), kinds);
        if (hits.isEmpty()) return null;

        List<Seg<S>> out = new ArrayList<>();
        int base = 0;
        int mi = 0;
        for (int i = 0; i < texts.size(); i++) {
            String t = texts.get(i);
            S st = styles.get(i);
            int a = base;
            int b = base + t.length();
            base = b;
            int cur = a;
            while (mi < hits.size() && hits.get(mi).end() <= cur) mi++;
            int k = mi;
            while (k < hits.size() && hits.get(k).start() < b) {
                Hit h = hits.get(k);
                int s = Math.max(h.start(), cur);
                int e = Math.min(h.end(), b);
                if (s > cur) out.add(new Seg<>(t.substring(cur - a, s - a), st));
                if (h.start() >= a) {
                    boolean ob = policy.obfuscate(h.own());
                    out.add(new Seg<>(policy.replacement(h.own()), ob ? obfuscator.apply(st, true) : st));
                }
                cur = Math.max(cur, e);
                if (h.end() <= b) k++; else break;
            }
            if (cur < b) out.add(new Seg<>(t.substring(cur - a), st));
            mi = k;
        }
        return out;
    }
}
