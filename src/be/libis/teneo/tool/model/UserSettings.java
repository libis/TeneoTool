package be.libis.teneo.tool.model;

import java.util.Locale;
import java.util.prefs.Preferences;

public class UserSettings {
    private static final String LANG_PREF = "Language";
    private static final String TOOL_PREF = "Tool";
    private static final String DIR_PREF = "Directory";

    private static Preferences getPrefs() {
        return Preferences.userNodeForPackage(be.libis.teneo.tool.Main.class);
    }

    public static String getTool() {
        String defaultValue = "MD5Checker";
        return getPrefs().get(TOOL_PREF, defaultValue);
    }

    public static void setTool(String newValue) {
        getPrefs().put(TOOL_PREF, newValue);
    }

    public static String getDir() {
        String defaultValue = "";
        return getPrefs().get(DIR_PREF, defaultValue);
    }

    public static void setDir(String newValue) {
        getPrefs().put(DIR_PREF, newValue);
    }

    static Locale getLocale() {
        String defaultValue = "nl";
        return new Locale(getPrefs().get(LANG_PREF, defaultValue), "");
    }

    static void setLocale(Locale locale) {
        getPrefs().put(LANG_PREF, locale.getLanguage());
    }
}
