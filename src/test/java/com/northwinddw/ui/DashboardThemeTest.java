package com.northwinddw.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardThemeTest {
    private Preferences preferences;

    @BeforeEach
    void createPreferencesNode() throws Exception {
        preferences = Preferences.userRoot().node("northwind-dw/tests/" + UUID.randomUUID());
    }

    @AfterEach
    void removePreferencesNode() throws Exception {
        preferences.removeNode();
        preferences.flush();
    }

    @Test
    void startsInDarkModeWhenNoChoiceHasBeenSaved() {
        assertEquals(DashboardTheme.DARK, DashboardTheme.load(preferences));
    }

    @Test
    void usesDarkModeWhenSavedPreferenceIsInvalid() {
        preferences.put("theme", "unrecognized");

        assertEquals(DashboardTheme.DARK, DashboardTheme.load(preferences));
    }

    @Test
    void savesAndRestoresTheSelectedTheme() {
        DashboardTheme.LIGHT.save(preferences);

        assertEquals(DashboardTheme.LIGHT, DashboardTheme.load(preferences));
    }

    @Test
    void switchesBetweenLightAndDark() {
        assertEquals(DashboardTheme.LIGHT, DashboardTheme.DARK.next());
        assertEquals(DashboardTheme.DARK, DashboardTheme.LIGHT.next());
    }
}
