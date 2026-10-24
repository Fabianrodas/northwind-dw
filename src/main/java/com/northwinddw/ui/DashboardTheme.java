package com.northwinddw.ui;

import java.util.Objects;
import java.util.prefs.Preferences;

public enum DashboardTheme {
    LIGHT("theme-light", "Claro"),
    DARK("theme-dark", "Oscuro");

    private static final String PREFERENCE_KEY = "dashboard-theme";

    private final String styleClass;
    private final String displayName;

    DashboardTheme(String styleClass, String displayName) {
        this.styleClass = styleClass;
        this.displayName = displayName;
    }

    public static DashboardTheme load(Preferences preferences) {
        Objects.requireNonNull(preferences, "preferences");
        try {
            return valueOf(preferences.get(PREFERENCE_KEY, DARK.name()));
        } catch (IllegalArgumentException exception) {
            return DARK;
        }
    }

    public void save(Preferences preferences) {
        Objects.requireNonNull(preferences, "preferences").put(PREFERENCE_KEY, name());
    }

    public DashboardTheme next() {
        return this == DARK ? LIGHT : DARK;
    }

    public String styleClass() {
        return styleClass;
    }

    public String toggleLabel() {
        return "Modo " + next().displayName;
    }
}
