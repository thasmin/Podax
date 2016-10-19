package com.axelby.podax;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.axelby.podax.model.EpisodeDB;
import com.axelby.podax.model.PodaxDB;
import com.axelby.podax.model.SubscriptionData;

public class MediaButtonIntentReceiver extends BroadcastReceiver {

	@Override
    public void onReceive(Context context, Intent intent) {
		if (handleMediaButton(context, intent))
			return;

		if (isOrderedBroadcast())
            abortBroadcast();
    }

	private static boolean handleMediaButton(Context context, Intent intent) {
        KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

        if (event == null || event.getAction() != KeyEvent.ACTION_DOWN)
            return false;

		long activeEpisodeId = PodaxDB.episodes.getActiveEpisodeId();
		switch (event.getKeyCode()) {
			// Simple headsets only send KEYCODE_HEADSETHOOK
			case KeyEvent.KEYCODE_HEADSETHOOK:
				PlayerService.pause(context);
				break;
			case KeyEvent.KEYCODE_MEDIA_PLAY:
				PlayerService.play(context);
				break;
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				PlayerService.playpause(context);
				break;
			case KeyEvent.KEYCODE_MEDIA_PAUSE:
				PlayerService.pause(context);
				break;
			case KeyEvent.KEYCODE_MEDIA_STOP:
				PlayerService.stop(context);
				break;
			case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
			case KeyEvent.KEYCODE_MEDIA_NEXT:
				EpisodeDB.movePositionBy(activeEpisodeId, 30);
				break;
			case KeyEvent.KEYCODE_MEDIA_REWIND:
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
				EpisodeDB.movePositionBy(activeEpisodeId, -15);
				break;
			default:
				Log.e("Podax", "No matched event: " + event.getKeyCode());
		}

		return true;
	}

	private static MediaSessionCompat _mediaSession = null;
	public static MediaSessionCompat.Token getSessionToken() {
		if (_mediaSession != null)
			return _mediaSession.getSessionToken();
		return null;
	}

	private static MediaSessionCompat.Callback _mediaCallback = new MediaSessionCompat.Callback() {
		@Override
		public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
			return handleMediaButton(PodaxApplication.get(), mediaButtonEvent);
		}

		@Override public void onPlay() {
			super.onPlay();
			PlayerService.play(PodaxApplication.get());
		}

		@Override public void onPause() {
			super.onPause();
			PlayerService.pause(PodaxApplication.get());
		}

		@Override public void onStop() {
			super.onStop();
			PlayerService.stop(PodaxApplication.get());
		}

		@Override public void onSkipToNext() {
			super.onSkipToNext();
			EpisodeDB.skipToEnd(PodaxDB.episodes.getActiveEpisodeId());
		}

		@Override public void onSkipToPrevious() {
			super.onSkipToPrevious();
			EpisodeDB.restart(PodaxDB.episodes.getActiveEpisodeId());
		}

		@Override public void onFastForward() {
			super.onFastForward();
			EpisodeDB.movePositionBy(PodaxDB.episodes.getActiveEpisodeId(), 30);
		}
		@Override public void onRewind() {
			super.onRewind();
			EpisodeDB.movePositionBy(PodaxDB.episodes.getActiveEpisodeId(), -15);
		}

		@Override public void onSeekTo(long pos) {
			super.onSeekTo(pos);
			EpisodeDB.movePositionTo(PodaxDB.episodes.getActiveEpisodeId(), (int) pos);
		}
	};

	public static void initialize(Context context) {
		_mediaSession = new MediaSessionCompat(context, "podax", new ComponentName(context, MediaButtonIntentReceiver.class), null);
		_mediaSession.setCallback(_mediaCallback);
		_mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
		updateMetadata(context);
	}

	private static final long PLAYBACK_ACTIONS = PlaybackStateCompat.ACTION_PLAY
		| PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_STOP
		| PlaybackStateCompat.ACTION_REWIND | PlaybackStateCompat.ACTION_FAST_FORWARD
		| PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
		| PlaybackStateCompat.ACTION_SEEK_TO | PlaybackStateCompat.ACTION_PLAY_PAUSE;

	public static void updateState(@PlaybackStateCompat.State int state, float positionInSeconds, float playbackRate) {
		if (_mediaSession == null)
			return;

		PlaybackStateCompat.Builder bob = new PlaybackStateCompat.Builder();
		bob.setState(state, (long) (positionInSeconds * 1000), playbackRate);
		bob.setActions(PLAYBACK_ACTIONS);
		PlaybackStateCompat pbState = bob.build();
		_mediaSession.setPlaybackState(pbState);
		if (state == PlaybackStateCompat.STATE_PLAYING)
			_mediaSession.setActive(true);
	}

	protected static void updateMetadata(Context context) {
		PlayerStatus status = PlayerStatus.getCurrentState(context);
		MediaMetadataCompat.Builder bob = new MediaMetadataCompat.Builder();
		bob.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, status.getTitle());
		bob.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, status.getSubscriptionTitle());
		bob.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, status.getSubscriptionTitle());
		bob.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, status.getSubscriptionTitle());
		bob.putString(MediaMetadataCompat.METADATA_KEY_TITLE, status.getTitle());
		bob.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, status.getDuration());

		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics metrics = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(metrics);
		Bitmap thumbnail = SubscriptionData.getThumbnailImageRaw(status.getSubscriptionId());
		if (thumbnail != null) {
			bob.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, thumbnail);
			bob.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, thumbnail);
			bob.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, thumbnail);
		}

		_mediaSession.setMetadata(bob.build());
	}
}