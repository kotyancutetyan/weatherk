package com.kotyancutetyan.weatherk;

import com.getcapacitor.BridgeActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Context;
import android.content.SharedPreferences;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.webkit.WebView;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class MainActivity extends BridgeActivity {
    private final Handler widgetSyncHandler = new Handler(Looper.getMainLooper());
    private final Runnable widgetSyncTask = new Runnable() {
        @Override
        public void run() {
            syncWidgetPrefsFromWebView();
            widgetSyncHandler.postDelayed(this, 15000);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerPlugin(WeatherPrefsPlugin.class);
        enableImmersiveMode();
    }

    @Override
    public void onResume() {
        super.onResume();
        syncWidgetPrefsFromWebView();
        widgetSyncHandler.removeCallbacks(widgetSyncTask);
        widgetSyncHandler.postDelayed(widgetSyncTask, 2000);
    }

    @Override
    public void onPause() {
        super.onPause();
        widgetSyncHandler.removeCallbacks(widgetSyncTask);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enableImmersiveMode();
    }

    private void enableImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }
    }

    private void syncWidgetPrefsFromWebView() {
        WebView webView = bridge != null ? bridge.getWebView() : null;
        if (webView == null) return;

        // 1) city from input value or localStorage
        webView.evaluateJavascript(
                "(function(){try{" +
                        "var fromInput=(document.getElementById('cityInput')&&document.getElementById('cityInput').value)||'';" +
                        "var fromStorage=localStorage.getItem('weatherLastCity')||'';" +
                        "var city=(fromInput||fromStorage||'').toString().trim();" +
                        "return JSON.stringify(city);" +
                        "}catch(e){return '\"\"';}})();",
                cityRaw -> {
                    String city = parseJsString(cityRaw);
                    if (city != null && !city.trim().isEmpty()) {
                        saveWidgetPref(WeatherPrefsPlugin.KEY_LAST_CITY, city.trim());
                        triggerAvatarWidgetUpdate();
                    }
                }
        );

        // 2) api key from page runtime
        webView.evaluateJavascript(
                "(function(){try{return JSON.stringify((window.OPENWEATHER_API_KEY||'').toString());}catch(e){return '\"\"';}})();",
                apiRaw -> {
                    String apiKey = parseJsString(apiRaw);
                    if (apiKey != null && !apiKey.trim().isEmpty()) {
                        saveWidgetPref(WeatherPrefsPlugin.KEY_API_KEY, apiKey.trim());
                        triggerAvatarWidgetUpdate();
                    }
                }
        );
    }

    private void saveWidgetPref(String key, String value) {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences(WeatherPrefsPlugin.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(key, value).apply();
    }

    private void triggerAvatarWidgetUpdate() {
        try {
            Context context = getApplicationContext();
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, AvatarWidgetProvider.class));
            if (ids != null && ids.length > 0) {
                new AvatarWidgetProvider().onUpdate(context, manager, ids);
                Intent i = new Intent(context, AvatarWidgetProvider.class);
                i.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                context.sendBroadcast(i);
            }
        } catch (Exception ignored) {
        }
    }

    private String parseJsString(String jsValue) {
        if (jsValue == null || jsValue.equals("null")) return "";
        String v = jsValue;
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
            v = v.substring(1, v.length() - 1);
        }
        return v.replace("\\\\", "\\").replace("\\\"", "\"").replace("\\n", "\n");
    }
}
