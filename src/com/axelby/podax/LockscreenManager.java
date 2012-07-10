package com.axelby.podax;

import java.io.File;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.RemoteControlClient.MetadataEditor;
import android.os.Build;
import android.util.Log;

public class LockscreenManager {

	private RemoteControlClient _remoteControlClient;

	@TargetApi(14)
	public void setupLockscreenControls(Context context, PodcastCursor podcast) {
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
		_remoteControlClient.setTransportControlFlags(
				RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
				| RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
				| RemoteControlClient.FLAG_KEY_MEDIA_NEXT);

		try {
			final int METADATA_KEY_ARTWORK = 100;

			// Update the remote controls
			MetadataEditor metadataEditor = _remoteControlClient
					.editMetadata(true)
					.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, podcast.getSubscriptionTitle())
					.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, podcast.getTitle())
					.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, podcast.getDuration());
			if (new File(podcast.getThumbnailFilename()).exists()) {
				Bitmap subscriptionThumbnail = BitmapFactory.decodeFile(podcast.getThumbnailFilename());
				metadataEditor.putBitmap(METADATA_KEY_ARTWORK, subscriptionThumbnail);
			}
			metadataEditor.apply();
		} catch (Exception e) {
			Log.d("Podax", "Updating lockscreen: " + e.toString());
		}
	}

	@TargetApi(14)
	public void removeLockscreenControls() {
		if (_remoteControlClient != null)
			_remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
	}

	@TargetApi(14)
	public void setLockscreenPaused() {
		if (_remoteControlClient == null)
			return;
		_remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
	}

	@TargetApi(14)
	public void setLockscreenPlaying() {
		if (_remoteControlClient == null)
			return;
		_remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
	}
}
