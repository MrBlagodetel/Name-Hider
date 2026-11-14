package com.namehider;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Поиск и замена ников (свой и чужих) в тексте. Сама логика поиска живёт в {@link NHMatch}. */
public final class NHText {
    /** 8 символов; при отрисовке им ставится стиль "obfuscated" (случайные меняющиеся знаки). */
    public static final String HIDDEN = "????????";

    private static final long REFRESH_NS = 250_000_000L;
    private static final int SEEN_LIMIT = 4096;
    /** Сколько без соединения помним ники (переход матч -> лобби занимает секунды). */
    private static final long FORGET_NS = 60_000_000_000L;
    private static long nullSince;

    /** ник (нижний регистр) -> true, если это мой ник. */
    private static volatile Map<String, Boolean> kinds = Map.of();
    private static volatile long lastRefresh;
    private static volatile boolean dirty = true;
    private static volatile int lastCount = -1;

    /**
     * Номер "версии" набора скрываемых ников и настроек. Игра кэширует готовый текст компонентов
     * (таб, скорборд, голограммы...), поэтому при смене номера мы заставляем пересчитать этот кэш
     * (см. MutableComponentMixin). Так новые игроки и нажатие кнопки в меню действуют сразу.
     */
    private static volatile int generation;

    /** Ники, которые мы видели на сервере (таб, игроки в мире, команды скорборда). Нужны для режима "все":
     *  в топах, голограммах и сообщениях после матча бывают игроки, которых уже нет в сети.
     *  Переживает переходы между серверами (матч -> лобби), очищается после минуты без соединения. */
    private static final Set<String> seen = new LinkedHashSet<>();

    private static final Object OWN_LOCK = new Object();
    private static String ownCachedName;
    private static Pattern ownCachedPattern;

    private static final NHMatch.Policy POLICY = new NHMatch.Policy() {
        @Override public String replacement(boolean own) { return NHText.replacement(own); }
        @Override public boolean obfuscate(boolean own) { return NHText.obfuscate(own); }
    };

    private NHText() {}

    /** Настройки изменились: пересчитать ники и сбросить кэш готового текста. */
    public static void invalidate() {
        dirty = true;
        generation++;
    }

    /** Только пересчитать список ников (кэш текста сбросится сам, если список изменился). */
    public static void markDirty() {
        dirty = true;
    }

    public static int generation() {
        refresh();
        return generation;
    }

    /** Перерисовать уже написанные строки чата (старые сообщения тоже скроют/покажут ники). */
    public static void refreshChat() {
        try {
            Object gui = Minecraft.getInstance().gui;
            Object chat = call(gui, "getChat");
            if (chat == null) chat = field(gui, "chat");
            if (chat == null) return;
            for (String n : new String[] {"rescaleChat", "refreshTrimmedMessages", "refreshTrimmedMessage"}) {
                if (callVoid(chat, n)) return;
            }
        } catch (Throwable ignored) {
        }
    }

    private static Object call(Object o, String name) {
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass()) {
            try {
                var m = c.getDeclaredMethod(name);
                m.setAccessible(true);
                return m.invoke(o);
            } catch (NoSuchMethodException e) {
                // ищем выше по иерархии
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    private static boolean callVoid(Object o, String name) {
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass()) {
            try {
                var m = c.getDeclaredMethod(name);
                m.setAccessible(true);
                m.invoke(o);
                return true;
            } catch (NoSuchMethodException e) {
                // ищем выше по иерархии
            } catch (Throwable t) {
                return false;
            }
        }
        return false;
    }

    private static Object field(Object o, String name) {
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass()) {
            try {
                var f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(o);
            } catch (NoSuchFieldException e) {
                // ищем выше по иерархии
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    private static String ownName() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getUser() != null) return mc.getUser().getName();
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String bounded(String name) {
        return "(?<![A-Za-z0-9_])" + Pattern.quote(name) + "(?![A-Za-z0-9_])";
    }

    private static void addSeen(String n) {
        if (n == null || n.isEmpty()) return;
        if (seen.size() >= SEEN_LIMIT) {
            var it = seen.iterator();
            it.next();
            it.remove();
        }
        seen.add(n.toLowerCase(Locale.ROOT));
    }

    private static int onlineCount() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getConnection() == null) return 0;
            return mc.getConnection().getOnlinePlayers().size();
        } catch (Throwable t) {
            return 0;
        }
    }

    private static void collectSeen() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getConnection() == null) {
                long now = System.nanoTime();
                if (nullSince == 0) nullSince = now;
                else if (now - nullSince > FORGET_NS) seen.clear();
                return;
            }
            nullSince = 0;
            for (PlayerInfo pi : mc.getConnection().getOnlinePlayers()) addSeen(pi.getProfile().name());
            if (mc.level != null) {
                for (var p : mc.level.players()) addSeen(p.getGameProfile().name());
                for (var team : mc.level.getScoreboard().getPlayerTeams()) {
                    for (String n : team.getPlayers()) addSeen(n);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void refresh() {
        long now = System.nanoTime();
        if (!dirty && now - lastRefresh < REFRESH_NS) {
            // быстрая проверка: в режиме "все" кто-то зашёл или вышел -> пересчитываем сразу
            if (!"ALL".equals(NHConfig.othersNicks) || onlineCount() == lastCount) return;
        }
        doRefresh(now);
    }

    private static synchronized void doRefresh(long now) {
        dirty = false;
        lastRefresh = now;
        lastCount = onlineCount();

        Map<String, Boolean> m = new HashMap<>();
        if (NHConfig.enabled) {
            String own = ownName();
            String ownLower = own == null ? null : own.toLowerCase(Locale.ROOT);
            if (NHConfig.p().hideNick && ownLower != null && !ownLower.isEmpty()) {
                m.put(ownLower, true);
            }

            if ("ALL".equals(NHConfig.othersNicks)) {
                collectSeen();
                for (String n : seen) {
                    // свой ник в режиме "все" не трогаем: им управляет отдельная настройка
                    if (!n.equals(ownLower)) m.putIfAbsent(n, false);
                }
            }
        }
        if (!m.equals(kinds)) {
            kinds = m;
            generation++;
        }
    }

    private static Map<String, Boolean> current() {
        refresh();
        return kinds;
    }

    /** Есть ли вообще что скрывать. */
    public static boolean active() {
        return !current().isEmpty();
    }

    /** Найденные в тексте ники (пустой список, если ничего нет). */
    public static List<NHMatch.Hit> find(String text) {
        return NHMatch.find(text, current());
    }

    /** Свой ник введён? Тогда показываем его, иначе 8 непонятных символов. */
    public static boolean useCustomNick() {
        String c = NHConfig.p().customNick;
        return c != null && !c.isBlank();
    }

    /** Нужен ли стиль "obfuscated" для этой замены. */
    public static boolean obfuscate(boolean own) {
        return !own || !useCustomNick();
    }

    public static String replacement(boolean own) {
        return own && useCustomNick() ? NHConfig.p().customNick.trim() : HIDDEN;
    }

    /** Простая замена (без стилей): для getString(), логов и т.п. */
    public static String replaceAll(String text) {
        return NHMatch.rewrite(text, current(), false, POLICY);
    }

    /** Замена в строке, которая будет разобрана как форматированная (с кодами §). */
    public static String replaceFormatted(String text) {
        return NHMatch.rewrite(text, current(), true, POLICY);
    }

    /**
     * Замена ников в готовом компоненте перед отрисовкой. Ищет по всему тексту целиком,
     * поэтому находит и ники, разбитые на несколько компонентов (градиент по буквам).
     */
    public static FormattedText rewriteText(FormattedText in) {
        if (in == null) return null;
        Map<String, Boolean> k = current();
        if (k.isEmpty()) return in;
        try {
            List<String> texts = new ArrayList<>();
            List<Style> styles = new ArrayList<>();
            in.visit((style, str) -> {
                texts.add(str);
                styles.add(style);
                return Optional.empty();
            }, Style.EMPTY);
            List<NHMatch.Seg<Style>> segs =
                    NHMatch.rewriteSegments(texts, styles, k, POLICY, (st, b) -> st.withObfuscated(true));
            if (segs == null) return in;
            List<FormattedText> parts = new ArrayList<>(segs.size());
            for (NHMatch.Seg<Style> seg : segs) parts.add(FormattedText.of(seg.text(), seg.style()));
            return FormattedText.composite(parts);
        } catch (Throwable t) {
            return in;
        }
    }

    /** Только свой ник, безопасно из любого потока (для логов). */
    public static String replaceOwnPlain(String text) {
        try {
            if (text == null || text.isEmpty() || !NHConfig.enabled || !NHConfig.p().hideNick) return text;
            String own = ownName();
            if (own == null || own.isEmpty()) return text;
            synchronized (OWN_LOCK) {
                if (!own.equals(ownCachedName)) {
                    ownCachedName = own;
                    ownCachedPattern = Pattern.compile(bounded(own), Pattern.CASE_INSENSITIVE);
                }
                return ownCachedPattern.matcher(text).replaceAll(Matcher.quoteReplacement(replacement(true)));
            }
        } catch (Throwable t) {
            return text;
        }
    }

    /** Действует ли режим "все" для чужих игроков. */
    public static boolean matchesMode(String name, String mode, String ignoredList) {
        return name != null && "ALL".equals(mode);
    }
}
