package com.example.sentenix_proto_1.update;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ConfigStore {

    private static final String PREFS = "hershield_update";
    private static final String KEY_APPLIED = "applied";
    private static final String KEY_XML = "xml";
    private static final String KEY_VERSION = "version";
    private static final String KEY_ROLE = "role";
    public static final String ROLE_USER = "user";
    public static final String ROLE_ADMIN = "admin";

    private final SharedPreferences prefs;

    public ConfigStore(@NonNull Context ctx) {
        this.prefs = ctx.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isApplied() {
        return prefs.getBoolean(KEY_APPLIED, false);
    }

    @Nullable
    public String getAppliedXml() {
        return prefs.getString(KEY_XML, null);
    }

    public int getAppliedVersion() {
        return prefs.getInt(KEY_VERSION, 0);
    }

    public void save(@NonNull String xml, int version) {
        prefs.edit()
                .putBoolean(KEY_APPLIED, true)
                .putString(KEY_XML, xml)
                .putInt(KEY_VERSION, version)
                .apply();
    }

    public void clear() {
        // Preserve role; only wipe the applied-update state.
        prefs.edit()
                .remove(KEY_APPLIED)
                .remove(KEY_XML)
                .remove(KEY_VERSION)
                .apply();
    }

    @NonNull
    public String getRole() {
        return prefs.getString(KEY_ROLE, ROLE_USER);
    }

    public void saveRole(@NonNull String role) {
        prefs.edit().putString(KEY_ROLE, role).apply();
    }
}
