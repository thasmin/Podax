package com.axelby.podax;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
<<<<<<< HEAD
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
=======
import android.content.Context;
import android.content.Intent;
>>>>>>> 9f00096c1d2909dee5f7948a09b0b30fc132d6b1
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;

<<<<<<< HEAD
import com.axelby.podax.GPodderClient.Changes;

=======
>>>>>>> 9f00096c1d2909dee5f7948a09b0b30fc132d6b1
public class GPodderSyncService extends Service {
    private static final Object _syncAdapterLock = new Object();
    private static GPodderSyncAdapter _syncAdapter = null;

    @Override
    public void onCreate() {
        synchronized (_syncAdapterLock) {
            if (_syncAdapter == null) {
                _syncAdapter = new GPodderSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return _syncAdapter.getSyncAdapterBinder();
    }

	private static class GPodderSyncAdapter extends AbstractThreadedSyncAdapter {
	
		private Context _context;
	
		public GPodderSyncAdapter(Context context, boolean autoInitialize) {
	        super(context, autoInitialize);
	        _context = context;
	    }
	
		@Override
		public void onPerformSync(Account account, Bundle extras, String authority,
				ContentProviderClient provider, SyncResult syncResult) {
			AccountManager accountManager = AccountManager.get(_context);
			GPodderClient client = new GPodderClient(_context, account.name, accountManager.getPassword(account));
			if (!client.authenticate())
				return;

<<<<<<< HEAD
			SharedPreferences gpodderPrefs = getContext().getSharedPreferences("gpodder", MODE_PRIVATE);
			int lastTimestamp = gpodderPrefs.getInt("lastTimestamp", 0);

			// get the changes since the last time we updated
			GPodderClient.Changes changes = client.getSubscriptionChanges(lastTimestamp);
			updateSubscriptions(changes);

			// get a list of all subscriptions to detect local changes
			// gpodderDiffs contains differences that need to be rectified
			GPodderClient.Changes gpodderDiffs = client.getSubscriptionChanges(0);
			// find the last time we updated
			String[] projection = { SubscriptionProvider.COLUMN_URL };
			Cursor c = _context.getContentResolver().query(SubscriptionProvider.URI, projection, null, null, null);
			while (c.moveToNext()) {
				String url = c.getString(0);
				// if this url isn't in gpodder, send it
				if (!gpodderDiffs.added.contains(url)) {
					gpodderDiffs.removed.add(url);
				}
				// we know about it so don't remove it
				gpodderDiffs.added.remove(c.getString(0));
			}
			c.close();

			// send diffs to gpodder
			lastTimestamp = client.syncDiffs(gpodderDiffs);

			// remember when we last updated
			SharedPreferences.Editor gpodderPrefsEditor = gpodderPrefs.edit();
			gpodderPrefsEditor.putInt("lastTimestamp", lastTimestamp + 1);
			gpodderPrefsEditor.commit();

			UpdateService.updateSubscriptions(_context);
		}

		private void updateSubscriptions(Changes changes) {
			for (String newUrl : changes.added) {
				ContentValues values = new ContentValues();
				values.put(SubscriptionProvider.COLUMN_URL, newUrl);
				_context.getContentResolver().insert(SubscriptionProvider.URI, values);
			}

			for (String oldUrl : changes.removed) {
				_context.getContentResolver().delete(SubscriptionProvider.URI, "url = ?", new String[] { oldUrl });
			}
=======
			// find the last time we updated
			String[] projection = { SubscriptionProvider.COLUMN_GPODDER_SYNCTIME };
			Cursor c = _context.getContentResolver().query(SubscriptionProvider.URI, projection, null, null, SubscriptionProvider.COLUMN_GPODDER_SYNCTIME + " DESC");
			if (!c.moveToNext())
				return;
			int lastTimestamp = c.getInt(0);
			c.close();

			// update gpodder with new subscriptions
			client.sendSubscriptions();

			client.getSubscriptionChanges(lastTimestamp);
>>>>>>> 9f00096c1d2909dee5f7948a09b0b30fc132d6b1
		}
	
	}
}