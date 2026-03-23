package com.kotyancutetyan.weatherk;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AvatarWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_NEXT = "com.kotyancutetyan.weatherk.widget.NEXT_PHRASE";
    private static final String PREFS = "avatar_widget_prefs";
    private static final String KEY_INDEX = "phrase_index";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        renderWidgets(context, appWidgetManager, appWidgetIds, false);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent != null && ACTION_NEXT.equals(intent.getAction())) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, AvatarWidgetProvider.class));
            renderWidgets(context, manager, ids, true);
        }
    }

    private void renderWidgets(Context context, AppWidgetManager manager, int[] ids, boolean nextPhrase) {
        List<String> phrases = loadCompliments(context);
        if (phrases.isEmpty()) phrases.add("Катюша, ты прекрасная!");

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int idx = prefs.getInt(KEY_INDEX, -1);
        if (idx < 0 || idx >= phrases.size()) idx = 0;
        if (nextPhrase) {
            idx = (idx + 1) % phrases.size();
            prefs.edit().putInt(KEY_INDEX, idx).apply();
        }

        Bitmap avatar = loadAvatarBitmap(context);
        String phrase = phrases.get(idx);

        for (int appWidgetId : ids) {
            RemoteViews loading = new RemoteViews(context.getPackageName(), R.layout.widget_avatar);
            loading.setTextViewText(R.id.widgetAvatarCity, "Обновляю...");
            loading.setTextViewText(R.id.widgetAvatarWeather, "Погоду загружаю");
            loading.setTextViewText(R.id.widgetAvatarText, phrase);
            if (avatar != null) loading.setImageViewBitmap(R.id.widgetAvatarImage, avatar);
            manager.updateAppWidget(appWidgetId, loading);
        }

        new Thread(() -> {
            WeatherSnapshot snap = fetchWeather(context);
            for (int appWidgetId : ids) {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_avatar);
                if (avatar != null) views.setImageViewBitmap(R.id.widgetAvatarImage, avatar);
                views.setTextViewText(R.id.widgetAvatarCity, snap.cityLine);
                views.setTextViewText(R.id.widgetAvatarWeather, snap.weatherLine);
                views.setTextViewText(R.id.widgetAvatarText, phrase);
                manager.updateAppWidget(appWidgetId, views);
            }
        }).start();
    }

    private WeatherSnapshot fetchWeather(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(WeatherPrefsPlugin.PREFS_NAME, Context.MODE_PRIVATE);
        String city = prefs.getString(WeatherPrefsPlugin.KEY_LAST_CITY, "Москва");
        String apiKey = prefs.getString(WeatherPrefsPlugin.KEY_API_KEY, "");
        if (apiKey == null || apiKey.trim().isEmpty()) apiKey = readApiKeyFromConfig(context);
        if (city == null || city.trim().isEmpty()) city = "Москва";

        if (apiKey == null || apiKey.trim().isEmpty()) {
            return new WeatherSnapshot(city, "Нет API ключа");
        }

        HttpURLConnection conn = null;
        try {
            Uri uri = Uri.parse("https://api.openweathermap.org/data/2.5/weather")
                    .buildUpon()
                    .appendQueryParameter("q", city)
                    .appendQueryParameter("appid", apiKey)
                    .appendQueryParameter("lang", "ru")
                    .build();
            URL url = new URL(uri.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestMethod("GET");

            InputStream stream = (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300)
                    ? conn.getInputStream() : conn.getErrorStream();
            String body = readAll(stream);
            JSONObject root = new JSONObject(body);

            String cityName = root.optString("name", city);
            JSONObject main = root.optJSONObject("main");
            double tempKelvin = main != null ? main.optDouble("temp", Double.NaN) : Double.NaN;
            int temp = Double.isNaN(tempKelvin) ? 0 : (int) Math.round(tempKelvin - 273.15);
            JSONArray arr = root.optJSONArray("weather");
            int id = 0;
            if (arr != null && arr.length() > 0) {
                JSONObject w = arr.optJSONObject(0);
                if (w != null) id = w.optInt("id", 0);
            }
            return new WeatherSnapshot(cityName, weatherIcon(id) + " · " + temp + "°C");
        } catch (Exception e) {
            return new WeatherSnapshot(city, "Ошибка обновления");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private String readApiKeyFromConfig(Context context) {
        try {
            InputStream in = context.getAssets().open("public/config.js");
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            br.close();
            in.close();

            Matcher m = Pattern.compile("OPENWEATHER_API_KEY\\s*=\\s*'([^']+)'").matcher(sb.toString());
            if (m.find()) return m.group(1).trim();
        } catch (Exception ignored) {
        }
        return "";
    }

    private String weatherIcon(int id) {
        if (id == 800) return "☀️";
        if (id >= 200 && id < 300) return "⛈️";
        if (id >= 300 && id < 400) return "🌦️";
        if (id >= 500 && id < 600) return "🌧️";
        if (id >= 600 && id < 700) return "❄️";
        if (id >= 700 && id < 800) return "🌫️";
        if (id == 801) return "🌤️";
        if (id == 802) return "⛅";
        return "☁️";
    }

    private String readAll(InputStream stream) throws Exception {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }

    private PendingIntent createNextIntent(Context context) {
        Intent intent = new Intent(context, AvatarWidgetProvider.class);
        intent.setAction(ACTION_NEXT);
        return PendingIntent.getBroadcast(
                context,
                1002,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private Bitmap loadAvatarBitmap(Context context) {
        try {
            InputStream in = context.getAssets().open("public/avatar.png");
            Bitmap bmp = BitmapFactory.decodeStream(in);
            in.close();
            return bmp;
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<String> loadCompliments(Context context) {
        List<String> result = new ArrayList<>();
        try {
            InputStream in = context.getAssets().open("public/compliments.js");
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            br.close();
            in.close();

            Matcher m = Pattern.compile("'([^'\\\\]*(?:\\\\.[^'\\\\]*)*)'").matcher(sb.toString());
            while (m.find()) {
                String text = m.group(1).replace("\\'", "'");
                if (!TextUtils.isEmpty(text)) result.add(text);
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private static class WeatherSnapshot {
        final String cityLine;
        final String weatherLine;
        WeatherSnapshot(String cityLine, String weatherLine) {
            this.cityLine = cityLine;
            this.weatherLine = weatherLine;
        }
    }
}

