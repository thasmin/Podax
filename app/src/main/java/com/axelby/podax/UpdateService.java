package com.axelby.podax;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.axelby.podax.model.PodaxDB;
import com.axelby.podax.model.SubscriptionDB;

import rx.Observable;
import rx.subjects.PublishSubject;

public class UpdateService extends IntentService {
	private final Handler _uiHandler = new Handler();

	public UpdateService() {
		super("UpdateService");
	}

	public static void updateSubscriptions(Context context) {
		Intent intent = new Intent(context, UpdateService.class);
		intent.setAction(Constants.ACTION_REFRESH_ALL_SUBSCRIPTIONS);
		intent.putExtra(Constants.EXTRA_MANUAL_REFRESH, true);
		context.startService(intent);
	}

	public static void updateSubscription(Context context, long subscriptionId) {
		Intent intent = createUpdateSubscriptionIntent(context, subscriptionId);
		intent.putExtra(Constants.EXTRA_MANUAL_REFRESH, true);
		context.startService(intent);
	}

	private static Intent createUpdateSubscriptionIntent(Context context, long subscriptionId) {
		Intent intent = new Intent(context, UpdateService.class);
		intent.setAction(Constants.ACTION_REFRESH_SUBSCRIPTION);
		intent.putExtra(Constants.EXTRA_SUBSCRIPTION_ID, subscriptionId);
		return intent;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onHandleIntent(Intent intent) {
		handleIntent(intent);
	}

	void handleIntent(Intent intent) {
		String action = intent.getAction();
		if (action == null)
			return;

		boolean isManualRefresh = intent.getBooleanExtra(Constants.EXTRA_MANUAL_REFRESH, false);
		if (!isManualRefresh && Helper.isInvalidNetworkState(this)) {
			_uiHandler.post(() -> Toast.makeText(UpdateService.this,
					R.string.update_request_no_wifi,
					Toast.LENGTH_SHORT).show());
			return;
		}

		switch (action) {
			case Constants.ACTION_REFRESH_ALL_SUBSCRIPTIONS: {
				Observable.from(PodaxDB.subscriptions.getFor(SubscriptionDB.COLUMN_SINGLE_USE, 1))
					.subscribe(
						s -> updateSubscription(s.getId()),
						e -> Log.e("UpdateService", "unable to get all subscriptions", e)
					);
				break;
			}
			case Constants.ACTION_REFRESH_SUBSCRIPTION:
				long subscriptionId = intent.getLongExtra(Constants.EXTRA_SUBSCRIPTION_ID, -1);
				updateSubscription(subscriptionId);
				break;
		}

		removeNotification();
	}

	private static PublishSubject<Long> _updatingSubject = PublishSubject.create();
	public static Observable<Long> watch() {
		return _updatingSubject;
	}

	private static Long _updatingId = null;
	public static boolean areYouUpdating(long id) {
		return _updatingId != null && id == _updatingId;
	}

	private void updateSubscription(long subscriptionId) {
		if (subscriptionId == -1)
			return;

		_updatingId = subscriptionId;
		_updatingSubject.onNext(subscriptionId);
		new SubscriptionUpdater(this).update(subscriptionId);
		_updatingId = null;
		_updatingSubject.onNext(null);
	}

	private void removeNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(Constants.NOTIFICATION_UPDATE);
	}
}
