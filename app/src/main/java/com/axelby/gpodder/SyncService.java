package com.axelby.gpodder;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.axelby.gpodder.dto.Changes;
import com.axelby.gpodder.dto.DeviceConfiguration;
import com.axelby.gpodder.dto.EpisodeUpdate;
import com.axelby.gpodder.dto.EpisodeUpdateResponse;
import com.axelby.gpodder.dto.Podcast;
import com.axelby.podax.Constants;
import com.axelby.podax.EpisodeCursor;
import com.axelby.podax.EpisodeProvider;
import com.axelby.podax.Helper;
import com.axelby.podax.R;
import com.axelby.podax.UpdateService;
import com.axelby.podax.model.PodaxDB;
import com.axelby.podax.model.SubscriptionEditor;
import com.axelby.podax.model.Subscriptions;
import com.axelby.podax.ui.MainActivity;

import java.util.ArrayList;
import java.util.List;

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

		private final Context _context;
		private final String _deviceId;
		private final SharedPreferences _gpodderPrefs;

		public SyncAdapter(Context context, boolean autoInitialize) {
			super(context, autoInitialize);
			_context = context;

			_gpodderPrefs = getContext().getSharedPreferences("gpodder", MODE_PRIVATE);
			// default deviceId is podax for historical reasons, it should be generated with new account
			_deviceId = _gpodderPrefs.getString("deviceId", "podax");
		}

		private void showErrorNotification(Client client, int string_resource) {
			Notification notification = new NotificationCompat.Builder(_context)
					.setContentTitle(_context.getString(R.string.gpodder_sync_error))
					.setContentText(_context.getString(string_resource, client.getErrorMessage()))
					.setContentIntent(PendingIntent.getActivity(_context, 0, new Intent(_context, MainActivity.class), 0))
					.setWhen(System.currentTimeMillis())
					.setSmallIcon(R.drawable.ic_stat_gpoddernet)
					.build();
			NotificationManager notificationManager = (NotificationManager) _context.getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.notify(Constants.NOTIFICATION_GPODDER_ERROR, notification);
		}

		private void clearErrorNotification() {
			NotificationManager notificationManager = (NotificationManager) _context.getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.cancel(Constants.NOTIFICATION_GPODDER_ERROR);
		}

		@Override
		public void onPerformSync(Account account, Bundle extras, String authority,
								  ContentProviderClient provider, SyncResult syncResult) {
			AccountManager accountManager = AccountManager.get(_context);
			if (accountManager == null)
				return;

			Client client = new Client(_context, account.name, accountManager.getPassword(account));
			if (!client.login()) {
				showErrorNotification(client, R.string.gpodder_sync_cannot_authenticate);
				return;
			}

			if (!syncConfiguration(client))
				return;
			if (!syncPodcasts(client, account))
				return;
			if (!syncEpisodes(client, account))
				return;

			client.logout();

			clearErrorNotification();
		}

		private boolean syncConfiguration(Client client) {
			// change the configuration if applicable
			if (!_gpodderPrefs.getBoolean("configurationNeedsUpdate", false))
				return true;

			String caption = _gpodderPrefs.getString("caption", "podax");
			String type = _gpodderPrefs.getString("type", Helper.isTablet(_context) ? "laptop" : "phone");
			client.setDeviceConfiguration(_deviceId, new DeviceConfiguration(caption, type));
			if (client.getErrorMessage() != null) {
				showErrorNotification(client, R.string.gpodder_sync_error_configuration);
				return false;
			}

			_gpodderPrefs.edit().putBoolean("configurationNeedsUpdate", false).apply();
			return true;
		}

		private boolean syncPodcasts(Client client, Account account) {
			int lastTimestamp = _gpodderPrefs.getInt("lastTimestamp-" + account.name, 0);

			// if this is the first time, add podcasts from all other devices
			if (lastTimestamp == 0) {
				List<Podcast> subs = client.getAllSubscriptions();
				if (subs == null) {
					showErrorNotification(client, R.string.gpodder_sync_retrieve_all_episodes);
					return false;
				}
				for (Podcast sub : subs)
                    SubscriptionEditor.addNewSubscription(_context, sub.url);
			}

			// send subscription changes
			client.syncSubscriptionDiffs(_deviceId);
			if (client.getErrorMessage() != null) {
				showErrorNotification(client, R.string.gpodder_sync_send_episodes);
				return false;
			}

			// retrieve subscription changes and update podax
			Changes changes = client.getSubscriptionChanges(_deviceId, lastTimestamp);
			if (changes == null) {
				showErrorNotification(client, R.string.gpodder_sync_retrieve_episodes);
				return false;
			}

			for (String removedUrl : changes.remove)
				PodaxDB.subscriptions.deleteViaGPodder(removedUrl);

			for (String addedUrl : changes.add) {
				long id = SubscriptionEditor.createViaGPodder(addedUrl).commit();
				UpdateService.updateSubscription(_context, id);
			}

			// remember when we last updated
			SharedPreferences.Editor gpodderPrefsEditor = _gpodderPrefs.edit();
			gpodderPrefsEditor.putInt("lastTimestamp-" + account.name, changes.timestamp);
			gpodderPrefsEditor.apply();
			return true;
		}

		private boolean syncEpisodes(Client client, Account account) {
			Cursor c = _context.getContentResolver().query(EpisodeProvider.NEED_GPODDER_UPDATE_URI, null, null, null, null);
			if (c != null) {
				ArrayList<EpisodeUpdate> changeUpdates = new ArrayList<>(c.getCount());
				while (c.moveToNext()) {
					EpisodeCursor p = new EpisodeCursor(c);
					changeUpdates.add(new EpisodeUpdate(p.getSubscriptionUrl(), p.getMediaUrl(), _deviceId, "play", p.getGPodderUpdateTimestamp(), p.getLastPosition() / 1000));
				}
				c.close();

				client.updateEpisodes(changeUpdates);
				if (client.getErrorMessage() != null) {
					showErrorNotification(client, R.string.gpodder_sync_update_episode_positions);
					return false;
				}
			}

			// read episode changes
			long lastTimestamp = _gpodderPrefs.getLong("lastEpisodeTimestamp-" + account.name, 0);
			EpisodeUpdateResponse updates = client.getEpisodeUpdates(lastTimestamp);
			if (updates == null) {
				showErrorNotification(client, R.string.gpodder_sync_retrieve_episode_positions);
				return false;
			}

			for (EpisodeUpdate update : updates.actions) {
				if (update.episode == null || update.position == null)
					continue;

				String selection = EpisodeProvider.COLUMN_MEDIA_URL + " = ?";
				String[] selectionArgs = {update.episode};
				String[] projection = new String[] { EpisodeProvider.COLUMN_ID };
				Cursor idCursor = _context.getContentResolver().query(EpisodeProvider.URI, projection, selection, selectionArgs, null);
				if (idCursor != null && idCursor.moveToFirst()) {
					long podcastId = idCursor.getLong(0);
					idCursor.close();
					ContentValues values = new ContentValues();
					values.put(EpisodeProvider.COLUMN_LAST_POSITION, update.position * 1000);
					_context.getContentResolver().update(EpisodeProvider.getContentUri(podcastId), values, selection, selectionArgs);
				}
			}

			// remember when we last updated
			SharedPreferences.Editor gpodderPrefsEditor = _gpodderPrefs.edit();
			gpodderPrefsEditor.putLong("lastEpisodeTimestamp-" + account.name, updates.timestamp);
			gpodderPrefsEditor.apply();

			// update podcast so that gpodder has been synced
			ContentValues clearGpodderValues = new ContentValues(1);
			clearGpodderValues.put(EpisodeProvider.COLUMN_NEEDS_GPODDER_UPDATE, Constants.GPODDER_UPDATE_NONE);
			_context.getContentResolver().update(EpisodeProvider.URI, clearGpodderValues, null, null);

			return true;
		}
	}
}