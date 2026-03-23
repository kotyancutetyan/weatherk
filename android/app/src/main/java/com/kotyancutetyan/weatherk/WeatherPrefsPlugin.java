package com.kotyancutetyan.weatherk;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.ComponentName;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "WeatherPrefs")
public class WeatherPrefsPlugin extends Plugin {
    public static final String PREFS_NAME = "weather_widget_prefs";
    public static final String KEY_LAST_CITY = "last_city";
    public static final String KEY_API_KEY = "api_key";

    @PluginMethod
    public void save(PluginCall call) {
        String city = call.getString("lastCity");
        String apiKey = call.getString("apiKey");

        Context context = getContext();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (city != null && !city.trim().isEmpty()) editor.putString(KEY_LAST_CITY, city.trim());
        if (apiKey != null && !apiKey.trim().isEmpty()) editor.putString(KEY_API_KEY, apiKey.trim());
        editor.apply();

        // Принудительно обновим виджеты, чтобы сразу показывался последний город.
        try {
            Context context2 = getContext();
            AppWidgetManager manager = AppWidgetManager.getInstance(context2);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context2, AvatarWidgetProvider.class));
            if (ids != null && ids.length > 0) {
                new AvatarWidgetProvider().onUpdate(context2, manager, ids);
            }
        } catch (_) {
            // ignore
        }

        JSObject ret = new JSObject();
        ret.put("ok", true);
        call.resolve(ret);
    }
}

