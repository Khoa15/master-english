package com.doradodo.app.data.repository;

import com.doradodo.app.data.local.prefs.AppPreferences;

import java.time.DateTimeException;
import java.time.ZoneId;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * The time zone dates are shown in. It follows the device (no location needed) until the learner picks a zone in
 * the profile, and goes back to the device on request.
 */
@Singleton
public class UserTimeZone {

    private final AppPreferences preferences;

    @Inject
    public UserTimeZone(AppPreferences preferences) {
        this.preferences = preferences;
    }

    public ZoneId zone() {
        String id = preferences.getTimeZoneId();
        if (id != null) {
            try {
                return ZoneId.of(id);
            } catch (DateTimeException e) {
                // A stored id the device no longer knows: fall back to the device's zone.
            }
        }
        return ZoneId.systemDefault();
    }

    /** True while the zone follows the device. */
    public boolean isAutomatic() {
        return preferences.getTimeZoneId() == null;
    }

    /** Uses {@code zoneId}; an id that is not a known zone is ignored. */
    public void set(String zoneId) {
        try {
            preferences.setTimeZoneId(ZoneId.of(zoneId).getId());
        } catch (DateTimeException e) {
            // Keep the current choice.
        }
    }

    public void useAutomatic() {
        preferences.setTimeZoneId(null);
    }
}
