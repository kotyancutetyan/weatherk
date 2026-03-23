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
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AvatarWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "AvatarWidgetProvider";
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
        try {
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
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_avatar);
                if (avatar != null) views.setImageViewBitmap(R.id.widgetAvatarImage, avatar);
                views.setTextViewText(R.id.widgetAvatarText, phrase);
                views.setOnClickPendingIntent(R.id.widgetAvatarRoot, createNextIntent(context));
                manager.updateAppWidget(appWidgetId, views);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to render avatar widget", e);
        }
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
            // Держим bitmap компактным для RemoteViews, но максимально крупным для 1x2.
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            InputStream in1 = context.getAssets().open("public/avatar.png");
            BitmapFactory.decodeStream(in1, null, bounds);
            in1.close();

            int reqW = 180;
            int reqH = 260;
            int sample = 1;
            while ((bounds.outWidth / sample) > reqW * 2 || (bounds.outHeight / sample) > reqH * 2) {
                sample *= 2;
            }

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = Math.max(1, sample);
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            InputStream in2 = context.getAssets().open("public/avatar.png");
            Bitmap bmp = BitmapFactory.decodeStream(in2, null, opts);
            in2.close();
            if (bmp == null) return null;
            int srcW = bmp.getWidth();
            int srcH = bmp.getHeight();
            if (srcW <= 0 || srcH <= 0) return bmp;
            float ratio = Math.min((float) reqW / srcW, (float) reqH / srcH);
            if (ratio >= 1f) return bmp;
            int outW = Math.max(1, Math.round(srcW * ratio));
            int outH = Math.max(1, Math.round(srcH * ratio));
            return Bitmap.createScaledBitmap(bmp, outW, outH, true);
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
}

