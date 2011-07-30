package com.axelby.podax;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.widget.RemoteViews;

public class WidgetProvider extends AppWidgetProvider {
	static int requestCode = 0;
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		for (int appWidgetId : appWidgetIds) {
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
			
			Intent active = new Intent(context, PlayerService.class);
			active.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)); 
			PendingIntent pendingIntent = PendingIntent.getService(context, requestCode++, active, PendingIntent.FLAG_UPDATE_CURRENT);
			views.setOnClickPendingIntent(R.id.play_btn, pendingIntent);
			
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}

}
