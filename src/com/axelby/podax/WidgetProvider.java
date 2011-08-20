package com.axelby.podax;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class WidgetProvider extends AppWidgetProvider {
	static int requestCode = 0;
	
	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		
		DBAdapter dbAdapter = DBAdapter.getInstance(context);
		Podcast p = dbAdapter.getFirstInQueue();
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
		views.setTextViewText(R.id.title, p.getTitle());
		views.setTextViewText(R.id.podcast, p.getSubscription().getDisplayTitle());
		views.setTextViewText(R.id.positionstring, PlayerService.getPositionString(p.getDuration(), p.getLastPosition()));
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);

		for (int appWidgetId : appWidgetIds) {
			Log.d("Podax", "widget onUpdate");
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
			
			Intent playIntent = new Intent(context, PlayerService.class);
			playIntent.putExtra(Constants.EXTRA_PLAYER_COMMAND, Constants.PLAYER_COMMAND_PLAYPAUSE);
			PendingIntent playPendingIntent = PendingIntent.getService(context, appWidgetId, playIntent, 0);
			views.setOnClickPendingIntent(R.id.play_btn, playPendingIntent);
			
			Intent showIntent = new Intent(context, PodcastDetailActivity.class);
			PendingIntent showPendingIntent = PendingIntent.getActivity(context, appWidgetId, showIntent, 0);
			views.setOnClickPendingIntent(R.id.show_btn, showPendingIntent);
			
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}

}
