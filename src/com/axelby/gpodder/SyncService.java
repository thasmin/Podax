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
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.axelby.gpodder.Client.Changes;
import com.axelby.podax.PodcastProvider;
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
			Client client = new Client(_context, account.name, accountManager.getPassword(account));
			if (!client.authenticate())
				return;

			SharedPreferences gpodderPrefs = getContext().getSharedPreferences("gpodder", MODE_PRIVATE);
			int lastTimestamp = gpodderPrefs.getInt("lastTimestamp-" + account.name, 0);
			
			// send diffs to gpodder
			client.syncDiffs();

			// get the changes since the last time we updated
			Client.Changes changes = client.getSubscriptionChanges(lastTimestamp);
			updateSubscriptions(changes);

			// remember when we last updated
			SharedPreferences.Editor gpodderPrefsEditor = gpodderPrefs.edit();
			gpodderPrefsEditor.putInt("lastTimestamp-" + account.name, changes.timestamp);
			gpodderPrefsEditor.commit();

			Log.d("Podax", "done syncing");
		}

		private void updateSubscriptions(Changes changes) {
			for (String removedUrl : changes.removed)
				_context.getContentResolver().delete(SubscriptionProvider.FROM_GPODDER_URI, "url = ?", new String[] { removedUrl });

			for (String addedUrl : changes.added) {
				ContentValues values = new ContentValues();
				values.put(SubscriptionProvider.COLUMN_URL, addedUrl);
				Uri newUri = _context.getContentResolver().insert(SubscriptionProvider.FROM_GPODDER_URI, values);
				UpdateService.updateSubscription(_context, newUri);
			}
		}
	
	}
}