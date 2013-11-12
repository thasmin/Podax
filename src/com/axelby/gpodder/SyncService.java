package com.axelby.gpodder;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;

import com.axelby.gpodder.dto.Changes;
import com.axelby.podax.Helper;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.UpdateService;

public class SyncService extends Service {
    private static final Object _syncAdapterLock = new Object();
    private static SyncAdapter _syncAdapter = null;

    @Override
    public void onCreate() {
        synchronized (_syncAdapterLock) {
            if (_syncAdapter == null) {
                _syncAdapter = new SyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return _syncAdapter.getSyncAdapterBinder();
    }

	private static class SyncAdapter extends AbstractThreadedSyncAdapter {
	
		private Context _context;
	
		public SyncAdapter(Context context, boolean autoInitialize) {
	        super(context, autoInitialize);
	        _context = context;
	    }
	
		@Override
		public void onPerformSync(Account account, Bundle extras, String authority,
				ContentProviderClient provider, SyncResult syncResult) {
			AccountManager accountManager = AccountManager.get(_context);
			if (accountManager == null)
				return;

			Client client = new Client(_context, account.name, accountManager.getPassword(account));
			if (!client.authenticate())
				return;

			SharedPreferences gpodderPrefs = getContext().getSharedPreferences("gpodder", MODE_PRIVATE);
			// default deviceId is podax for historical reasons, it should be generated with new account
			String deviceId = gpodderPrefs.getString("deviceId", "podax");

			if (gpodderPrefs.getBoolean("configurationNeedsUpdate", false)) {
				String caption = gpodderPrefs.getString("caption", "podax");
				String type = gpodderPrefs.getString("type", Helper.isTablet(_context) ? "tablet" : "phone");
				client.setDeviceConfiguration(deviceId, new DeviceConfiguration(caption, type));
			}

			int lastTimestamp = gpodderPrefs.getInt("lastTimestamp-" + account.name, 0);
			
			// send diffs to gpodder
			client.syncDiffs(deviceId);

			// get the changes since the last time we updated
			Changes changes = client.getSubscriptionChanges(deviceId, lastTimestamp);
			if (changes == null)
				return;
			updateSubscriptions(changes);

			// remember when we last updated
			SharedPreferences.Editor gpodderPrefsEditor = gpodderPrefs.edit();
			gpodderPrefsEditor.putInt("lastTimestamp-" + account.name, changes.getTimestamp());
			gpodderPrefsEditor.commit();
		}

		private void updateSubscriptions(Changes changes) {
			for (String removedUrl : changes.getRemovedUrls())
				_context.getContentResolver().delete(SubscriptionProvider.FROM_GPODDER_URI, "url = ?", new String[] { removedUrl });

			for (String addedUrl : changes.getAddedUrls()) {
				ContentValues values = new ContentValues();
				values.put(SubscriptionProvider.COLUMN_URL, addedUrl);
				Uri newUri = _context.getContentResolver().insert(SubscriptionProvider.FROM_GPODDER_URI, values);
				UpdateService.updateSubscription(_context, newUri);
			}
		}
	
	}
}