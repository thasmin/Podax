package com.axelby.podax;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class UpdateService extends Service {
	private final long ONESECOND = 1000;
	final long ONEMINUTE = ONESECOND * 60;
	private final long ONEHOUR = ONEMINUTE * 60;

	private static UpdateService _instance;
	public static UpdateService getInstance() {
		return _instance;
	}
	
	Timer _timer = new Timer();
	Vector<Subscription> _toUpdate = new Vector<Subscription>();
	private UpdateSubscriptionTimerTask _rssTask = new UpdateSubscriptionTimerTask(this);
	private RefreshSubscriptionsTimerTask _refreshTask = new RefreshSubscriptionsTimerTask();
	PodcastDownloadTimerTask _downloadTask = new PodcastDownloadTimerTask(this);
	
	public void updateSubscription(Subscription subscription) {
		_toUpdate.add(subscription);
	}

	private SubscriptionUpdateBinder _binder = new SubscriptionUpdateBinder();
	public class SubscriptionUpdateBinder extends Binder {
		UpdateService getService() {
			return UpdateService.this;
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return _binder;
	}

	@Override
	public void onCreate() {
		_instance = this;
	}

	@Override
	public void onDestroy() {
	}

	@Override
	public void onStart(Intent intent, int startId) {
	    handleCommand(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    handleCommand(intent);
	    return START_NOT_STICKY;
	}
	
	private void handleCommand(Intent intent) {
        // check the queue every second
		_timer.scheduleAtFixedRate(_rssTask, ONESECOND, ONESECOND);
		// populate the queue with all of the scriptions every hour
		_timer.scheduleAtFixedRate(_refreshTask, ONESECOND, ONEHOUR);
		_timer.scheduleAtFixedRate(_downloadTask, ONESECOND, ONESECOND * 3);
	}
	
	private class RefreshSubscriptionsTimerTask extends TimerTask {
		@Override
		public void run() {
			final DBAdapter dbAdapter = DBAdapter.getInstance(UpdateService.this);
	        _toUpdate.addAll( dbAdapter.getUpdatableSubscriptions() );
		}
	}
}
