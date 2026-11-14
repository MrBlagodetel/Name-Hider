package com.namehider;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Свои переводы (RU/EN), чтобы не зависеть от Fabric API. */
public final class NHLang {
    private static final Map<String, String[]> T = new HashMap<>();

    private static void a(String key, String ru, String en) {
        T.put(key, new String[] {ru, en});
    }

    static {
        a("master", "Скрытый режим: %s", "Hidden mode: %s");
        a("menu.profiles", "Профили...", "Profiles...");
        a("menu.my.short", "Мой персонаж...", "My character...");
        a("menu.others.short", "Другие игроки...", "Other players...");
        a("profiles.title", "Профили", "Profiles");
        a("q.mynick", "Мой ник: %s", "My nick: %s");
        a("q.myskin", "Мой скин: %s", "My skin: %s");
        a("q.othernicks", "Ники других: %s", "Others' nicks: %s");
        a("q.otherskins", "Скины других: %s", "Others' skins: %s");
        a("q.mycape", "Мой плащ: %s", "My cape: %s");
        a("q.othercapes", "Плащи других: %s", "Others' capes: %s");
        a("v.nick.on", "скрыт", "hidden");
        a("v.nick.off", "виден", "shown");
        a("v.skin.on", "подменён", "swapped");
        a("v.skin.off", "настоящий", "real");
        a("v.others.ALL", "скрыты", "hidden");
        a("v.others.OFF", "видны", "shown");
        a("v.cape.on", "вкл", "on");
        a("v.cape.off", "выкл", "off");
        a("v.cape.custom", "свой", "custom");
        a("title", "Name Hider — настройки", "Name Hider — settings");
        a("profile", "Профиль: %s (%d/%d)", "Profile: %s (%d/%d)");
        a("profile.default", "Профиль", "Profile");
        a("profile.hint", "Название профиля", "Profile name");
        a("profile.new", "Новый профиль", "New profile");
        a("profile.delete", "Удалить профиль", "Delete profile");
        a("menu.my", "Мой ник, скин и плащ...", "My nickname, skin and cape...");
        a("menu.others", "Другие игроки...", "Other players...");
        a("lang", "Язык: %s", "Language: %s");
        a("lang.AUTO", "Авто", "Auto");
        a("lang.RU", "Русский", "Russian");
        a("lang.EN", "English", "English");
        a("done", "Готово", "Done");
        a("back", "Назад", "Back");
        a("on", "ВКЛ", "ON");
        a("off", "ВЫКЛ", "OFF");
        a("pick", "Выбрать...", "Browse...");
        a("pick.title", "Выберите PNG-файл", "Choose a PNG file");

        a("my.title", "Мой персонаж", "My character");
        a("my.hide", "Скрывать ник: %s", "Hide nickname: %s");
        a("my.nick.hint", "Свой ник (пусто = 8 символов ????????)", "Custom nickname (empty = 8 symbols ????????)");
        a("my.skin", "Менять скин: %s", "Change skin: %s");
        a("my.skinpath.hint", "Путь к PNG 64x64 (пусто = скин мода)", "Path to PNG 64x64 (empty = mod skin)");
        a("my.model", "Модель: %s", "Model: %s");
        a("model.AUTO", "как есть", "keep");
        a("model.WIDE", "обычная (Steve)", "wide (Steve)");
        a("model.SLIM", "тонкая (Alex)", "slim (Alex)");
        a("my.cape", "Мой плащ: %s", "My cape: %s");
        a("cape.KEEP", "оставить", "keep");
        a("cape.REMOVE", "убрать", "remove");
        a("cape.CUSTOM", "свой файл", "custom file");
        a("my.capepath.hint", "Путь к PNG плаща 64x32", "Path to cape PNG 64x32");

        a("others.title", "Другие игроки", "Other players");
        a("others.nicks", "Ники других: %s", "Others' nicknames: %s");
        a("others.skins", "Скины других: %s", "Others' skins: %s");
        a("others.capes", "Плащи других: %s", "Others' capes: %s");
        a("mode.OFF", "не трогать", "keep");
        a("mode.ALL", "все", "all");
        a("mode.LIST", "по списку", "by list");
        a("others.capes.KEEP", "оставить", "keep");
        a("others.capes.REMOVE", "убрать", "remove");
        a("others.list.hint", "Ники через запятую", "Nicknames, comma separated");

        a("st.skin.off", "Скин: выкл", "Skin: off");
        a("st.skin.default", "Скин: из мода", "Skin: mod default");
        a("st.skin.file", "Скин: свой файл", "Skin: your file");
        a("st.skin.bad", "Скин: нужен PNG 64x64", "Skin: need PNG 64x64");
        a("st.skin.err", "Скин: файл не прочитан", "Skin: can't read file");
        a("st.skin.none", "Скин: -", "Skin: -");
        a("st.cape.none", "Плащ: -", "Cape: -");
        a("st.cape.file", "Плащ: свой файл", "Cape: your file");
        a("st.cape.bad", "Плащ: нужен PNG 64x32", "Cape: need PNG 64x32");
        a("st.cape.err", "Плащ: файл не прочитан", "Cape: can't read file");
        a("hint.drag", "Перетащите PNG в окно (имя с \"cape\" = плащ)", "Drag a PNG into the window (name with \"cape\" = cape)");
    }

    private NHLang() {}

    public static boolean ru() {
        String l = NHConfig.language;
        if ("RU".equals(l)) return true;
        if ("EN".equals(l)) return false;
        try {
            Object lm = Minecraft.getInstance().getLanguageManager();
            Object code = lm.getClass().getMethod("getSelected").invoke(lm);
            if (code != null) return code.toString().toLowerCase(Locale.ROOT).startsWith("ru");
        } catch (Throwable ignored) {
        }
        return "ru".equals(Locale.getDefault().getLanguage());
    }

    public static String t(String key) {
        String[] v = T.get(key);
        if (v == null) return key;
        return ru() ? v[0] : v[1];
    }

    public static String f(String key, Object... args) {
        return String.format(t(key), args);
    }

    public static Component c(String key) {
        return Component.literal(t(key));
    }
}
