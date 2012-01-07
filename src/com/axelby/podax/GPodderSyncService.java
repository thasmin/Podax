package com.axelby.podax;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;

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
			SharedPreferences prefs = _context.getSharedPreferences("gpodder", MODE_PRIVATE);
			int lastTimestamp = prefs.getInt("lastTimestamp", 0);

			AccountManager accountManager = AccountManager.get(_context);
			GPodderClient client = new GPodderClient(account.name, accountManager.getPassword(account));
			client.authenticate();
			Integer newTimestamp = client.getSubscriptionChanges(lastTimestamp);

			if (newTimestamp != null) {
				Editor editor = prefs.edit();
				editor.putInt("lastTimestamp", newTimestamp);
				editor.commit();
			}
		}
	
	}
}