package com.axelby.podax;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.RemoteControlClient.MetadataEditor;
import android.os.Build;

public class LockscreenManager {
	private Context _context;
	private RemoteControlClient _remoteControlClient;

	@TargetApi(14)
	public void setupLockscreenControls(Context context, PlayerStatus status) {
		_context = context;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			return;

		AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		if (_remoteControlClient == null) {
			Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
			intent.setComponent(new ComponentName(context, MediaButtonIntentReceiver.class));
			_remoteControlClient = new RemoteControlClient(PendingIntent.getBroadcast(context, 0, intent, 0));
			audioManager.registerRemoteControlClient(_remoteControlClient);
		}

		_remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
		// android built-in lockscreen only supports play/pause/playpause/stop, previous, and next
		// next is destructive in podax because it deletes the cached file
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			_remoteControlClient.setTransportControlFlags(
					RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
							| RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
							| RemoteControlClient.FLAG_KEY_MEDIA_NEXT
							| RemoteControlClient.FLAG_KEY_MEDIA_POSITION_UPDATE
			);
			_remoteControlClient.setPlaybackPositionUpdateListener(new RemoteControlClient.OnPlaybackPositionUpdateListener() {
				@Override
				public void onPlaybackPositionUpdate(long position) {
					ContentValues values = new ContentValues(1);
					values.put(EpisodeProvider.COLUMN_LAST_POSITION, position);
					_context.getContentResolver().update(EpisodeProvider.ACTIVE_EPISODE_URI, values, null, null);
				}
			});
			_remoteControlClient.setOnGetPlaybackPositionListener(new RemoteControlClient.OnGetPlaybackPositionListener() {
				@Override
				public long onGetPlaybackPosition() {
					return PlayerStatus.getCurrentState(_context).getPosition();
				}
			});
		} else {
			_remoteControlClient.setTransportControlFlags(
					RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
							| RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
							| RemoteControlClient.FLAG_KEY_MEDIA_NEXT
			);
		}

		final int METADATA_KEY_ARTWORK = 100;

		// Update the remote controls
		final MetadataEditor metadataEditor = _remoteControlClient
				.editMetadata(true)
				.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, status.getSubscriptionTitle())
				.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, status.getTitle())
				.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, status.getDuration());
		Bitmap thumbnail = SubscriptionCursor.getThumbnailImage(context, status.getSubscriptionId());
		if (thumbnail != null) {
			metadataEditor.putBitmap(METADATA_KEY_ARTWORK, thumbnail);
		}
		metadataEditor.apply();
	}

	@TargetApi(14)
	public void removeLockscreenControls(float positionInSeconds) {
		if (_remoteControlClient == null)
			return;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			_remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED, (int) (positionInSeconds * 1000), 0);
			return;
		}
		_remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
	}

	@TargetApi(14)
	public void setLockscreenPaused(float positionInSeconds) {
		if (_remoteControlClient == null)
			return;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			_remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED, (int) (positionInSeconds * 1000), 0);
			return;
		}
		_remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
	}

	@TargetApi(14)
	public void setLockscreenPlaying(float positionInSeconds, float playbackSpeed) {
		if (_remoteControlClient == null)
			return;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			_remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING, (int) (positionInSeconds * 1000), playbackSpeed);
			return;
		}
		_remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
	}
}
