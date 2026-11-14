package com.namehider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class NHConfig {

    /** Один профиль = набор настроек "моего" персонажа. */
    public static class Profile {
        public String name = "Profile 1";
        public boolean hideNick = true;
        public String customNick = "";
        public boolean changeSkin = true;
        public String skinPath = "";
        /** AUTO / WIDE / SLIM */
        public String model = "AUTO";
        /** KEEP / REMOVE / CUSTOM */
        public String capeMode = "KEEP";
        public String capePath = "";
    }

    public static List<Profile> profiles = new ArrayList<>();
    public static int active = 0;
    /** Общий выключатель "скрытый режим": выкл = мод ничего не меняет. */
    public static boolean enabled = true;
    /** AUTO / RU / EN */
    public static String language = "AUTO";
    /** OFF / ALL / LIST */
    public static String othersNicks = "OFF";
    public static String othersNickList = "";
    public static String othersSkins = "OFF";
    public static String othersSkinList = "";
    /** KEEP / REMOVE */
    public static String othersCapes = "KEEP";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final class Data {
        List<Profile> profiles;
        int active;
        boolean enabled = true;
        String language = "AUTO";
        String othersNicks = "OFF";
        String othersNickList = "";
        String othersSkins = "OFF";
        String othersSkinList = "";
        String othersCapes = "KEEP";
        // поля старого формата (версии 1.0-1.1) для переноса настроек
        Boolean hideNick;
        String customNick;
        Boolean changeSkin;
        String skinPath;
    }

    static {
        load();
    }

    private NHConfig() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("namehider.json");
    }

    public static Profile p() {
        if (profiles.isEmpty()) profiles.add(new Profile());
        if (active < 0 || active >= profiles.size()) active = 0;
        return profiles.get(active);
    }

    public static Profile copy(Profile src) {
        return GSON.fromJson(GSON.toJson(src), Profile.class);
    }

    public static Set<String> parseList(String s) {
        Set<String> out = new LinkedHashSet<>();
        if (s == null) return out;
        for (String part : s.split("[,;\\s]+")) {
            if (!part.isBlank()) out.add(part.toLowerCase(Locale.ROOT));
        }
        return out;
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) {
            profiles.add(new Profile());
            return;
        }
        try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            Data d = GSON.fromJson(r, Data.class);
            if (d == null) { profiles.add(new Profile()); return; }
            if (d.profiles != null && !d.profiles.isEmpty()) {
                profiles = d.profiles;
            } else {
                Profile p = new Profile();
                if (d.hideNick != null) p.hideNick = d.hideNick;
                if (d.customNick != null) p.customNick = d.customNick;
                if (d.changeSkin != null) p.changeSkin = d.changeSkin;
                if (d.skinPath != null) p.skinPath = d.skinPath;
                profiles.add(p);
            }
            active = d.active;
            enabled = d.enabled;
            if (d.language != null) language = d.language;
            if (d.othersNicks != null) othersNicks = d.othersNicks;
            if (d.othersNickList != null) othersNickList = d.othersNickList;
            if (d.othersSkins != null) othersSkins = d.othersSkins;
            if (d.othersSkinList != null) othersSkinList = d.othersSkinList;
            if (d.othersCapes != null) othersCapes = d.othersCapes;
            // режим "по списку" убран: всё, что не "все", считаем выключенным
            if (!"ALL".equals(othersNicks)) othersNicks = "OFF";
            if (!"ALL".equals(othersSkins)) othersSkins = "OFF";
        } catch (Exception e) {
            e.printStackTrace();
            if (profiles.isEmpty()) profiles.add(new Profile());
        }
    }

    public static void save() {
        Data d = new Data();
        d.profiles = profiles;
        d.active = active;
        d.enabled = enabled;
        d.language = language;
        d.othersNicks = othersNicks;
        d.othersNickList = othersNickList;
        d.othersSkins = othersSkins;
        d.othersSkinList = othersSkinList;
        d.othersCapes = othersCapes;
        try (Writer w = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
            GSON.toJson(d, w);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
