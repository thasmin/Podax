package com.axelby.podax;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.axelby.podax.player.AudioPlayer;

import java.util.ArrayList;

public class PodcastPlayer {

	// listen for audio focus changes - another app started/stopped, phone call, etc
	private final AudioManager.OnAudioFocusChangeListener _afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
		public void onAudioFocusChange(int focusChange) {
			if (focusChange == AudioManager.AUDIOFOCUS_LOSS)
				stop();
			else if (focusChange == AudioManager.AUDIOFOCUS_GAIN)
				unpause(Constants.PAUSE_AUDIOFOCUS);
			else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
				pause(Constants.PAUSE_AUDIOFOCUS);
		}
	};

	protected Context _context;
	private ArrayList<Boolean> _pausingFor = new ArrayList<Boolean>(2);

	private OnPauseListener _onPauseListener = null;
	private OnPlayListener _onPlayListener = null;
	private OnStopListener _onStopListener = null;
	private OnSeekListener _onSeekListener = null;
	private OnCompletionListener _onCompletionListener = null;
	//private MediaPlayer.OnErrorListener _onErrorListener = null;

	private Thread _playerThread = null;
	private AudioPlayer _player = null;

	public PodcastPlayer(Context context) {
		_context = context;
		_pausingFor.add(false);
		_pausingFor.add(false);
	}

	public boolean changePodcast(String filename, float positionInSeconds) {
		if (_player != null) {
			_player.stop();
			_player = null;
		}
		if (_playerThread != null) {
			try {
				_playerThread.join();
			} catch (InterruptedException ex) {
				Log.e("Podax", "player thread interrupted", ex);
			}
			_playerThread = null;
		}

		try {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_context);
			float playbackRate = prefs.getFloat("playbackRate", 1.0f);
			_player = new AudioPlayer(filename, positionInSeconds, playbackRate);
			_playerThread = new Thread(_player, "AudioPlayer");

			/*
			_player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
				@Override
				public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
					mediaPlayer.reset();
					if (_onErrorListener != null)
						_onErrorListener.onError(mediaPlayer, what, extra);
					return true;
				}
			});
			*/
			_player.setOnCompletionListener(new AudioPlayer.OnCompletionListener() {
				@Override
				public void onCompletion() {
					if (_onCompletionListener != null)
						_onCompletionListener.onCompletion();
				}
			});
			_player.setPeriodicListener(new AudioPlayer.PeriodicListener() {
				@Override
				public void pulse(float position) {
					if (_onSeekListener != null)
						_onSeekListener.onSeek(position);
				}
			});

			return true;
		} catch (Exception ex) {
			Log.e("Podax", "unable to change to new podcast", ex);
			return false;
		}
	}

	/*
	public void setOnErrorListener(MediaPlayer.OnErrorListener onErrorListener) {
		this._onErrorListener = onErrorListener;
	}
	*/
	public void setOnPauseListener(OnPauseListener onPauseListener) {
		this._onPauseListener = onPauseListener;
	}
	public void setOnPlayListener(OnPlayListener onPlayListener) {
		this._onPlayListener = onPlayListener;
	}
	public void setOnStopListener(OnStopListener onStopListener) {
		this._onStopListener = onStopListener;
	}
	public void setOnSeekListener(OnSeekListener onSeekListener) { this._onSeekListener = onSeekListener; }
	public void setOnCompletionListener(OnCompletionListener onCompletionListener) {
		this._onCompletionListener = onCompletionListener;
	}

	/* external functions */

	// change position of podcast
	public void seekTo(float offsetInSeconds) {
		_player.seekTo(offsetInSeconds);
	}

	// if playing, pause. if paused, play.
	public void playPause(int pauseReason) {
		if (_player.isPlaying())
			pause(pauseReason);
		else
			unpause(pauseReason);
	}

	// if playing, stop. if paused, play.
	public void playStop() {
		if (_player.isPlaying())
			stop();
		else
			play();
	}

	// resume playing the podcast at the current position
	public void play() {
		internalPlay();
	}

	// set a pause reason and pause
	public void pause(int reason) {
		_pausingFor.set(reason, true);
		if (_player.isPlaying())
			internalPause();
	}

	// clear a pause reason and play if there's no valid pause reasons
	public void unpause(int pauseReason) {
		_pausingFor.set(pauseReason, false);
		if (!_pausingFor.contains(true))
			play();
	}

	// stop the podcast with no intention of resuming in the near future
	public void stop() {
		internalStop();
	}

	/* internal operations and state keeping */
	private boolean grabAudioFocus() {
		AudioManager audioManager = (AudioManager) _context.getSystemService(Context.AUDIO_SERVICE);
		int result = audioManager.requestAudioFocus(_afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
			stop();
			return false;
		}

		return true;
	}

	private void internalPlay() {
		if (_player.isPlaying())
			return;

		if (!grabAudioFocus())
			return;

		// make sure we're not pausing
		if (_pausingFor.contains(true))
			return;

		_player.resume();
		if (_playerThread.getState() == Thread.State.NEW)
			_playerThread.start();

		if (_onPlayListener != null)
			_onPlayListener.onPlay(_player.getDuration());
	}

	private void internalPause() {
		_player.pause();

		// tell the interested party
		if (_onPauseListener != null)
			_onPauseListener.onPause(_player.getPosition());
	}

	private void internalStop() {
		// stop playing
		float position = _player.getPosition();
		if (_player.isPlaying()) {
			_player.stop();
			try {
				_playerThread.join();
			} catch (InterruptedException e) {
				Log.e("Podax", "stop InterruptedException", e);
			}
		}

		// tell Android that we don't want the audio focus
		AudioManager am = (AudioManager) _context.getSystemService(Context.AUDIO_SERVICE);
		am.abandonAudioFocus(_afChangeListener);

		// tell the interested party
		if (_onStopListener != null)
			_onStopListener.onStop(position);
	}

	public static interface OnPauseListener {
		public void onPause(float positionInSeconds);
	}
	public static interface OnPlayListener {
		public void onPlay(float durationInSeconds);
	}
	public static interface OnStopListener {
		public void onStop(float positionInSeconds);
	}
	public static interface OnSeekListener {
		public void onSeek(float positionInSeconds);
	}
	public static interface OnCompletionListener {
		public void onCompletion();
	}
}
