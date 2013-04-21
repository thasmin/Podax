package com.axelby.podax.ui;

import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.axelby.podax.BootReceiver;
import com.axelby.podax.Constants;
import com.axelby.podax.Helper;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.UpdateService;

public class MainActivity extends SherlockFragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// check if this was opened by android to save an RSS feed
		Intent intent = getIntent();
		if (intent.getDataString() != null) {
			ContentValues values = new ContentValues();
			values.put(SubscriptionProvider.COLUMN_URL, intent.getDataString());
			Uri savedSubscription = getContentResolver().insert(SubscriptionProvider.URI, values);
			UpdateService.updateSubscription(this, Integer.valueOf(savedSubscription.getLastPathSegment()));
		}

		// clear RSS error notification
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) getSystemService(ns);
		notificationManager.cancel(Constants.SUBSCRIPTION_UPDATE_ERROR);

		BootReceiver.setupAlarms(getApplicationContext());

        FrameLayout frame = new FrameLayout(this);
        frame.setId(R.id.fragment);
        setContentView(frame, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        if (savedInstanceState == null) {
			Fragment fragment = Fragment.instantiate(this, MainFragment.class.getName());
		    FragmentManager fm = getSupportFragmentManager();
		    FragmentTransaction ft = fm.beginTransaction();
		    ft.add(R.id.fragment, fragment);
		    ft.commit();
        }
	}

	@Override
	protected void onResume() {
		super.onResume();

		Helper.registerMediaButtons(this);
	}

	/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (PodaxLog.isDebuggable(this)) {
			menu.add(Menu.NONE, R.id.text, 0, R.string.log_viewer);
		}

		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.add_subscription:
			startActivity(new Intent(this, AddSubscriptionActivity.class));
			return true;
		case R.id.download:
			UpdateService.downloadPodcasts(this);
			return true;
		case R.id.preferences:
			startActivity(new Intent(this, Preferences.class));
			return true;
		case R.id.about:
			startActivity(new Intent(this, AboutActivity.class));
			return true;
		case R.id.refresh_subscriptions:
			UpdateService.updateSubscriptions(this);
			return true;
		case R.id.text:
			startActivity(new Intent(this, LogViewer.class));
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}
	*/
}