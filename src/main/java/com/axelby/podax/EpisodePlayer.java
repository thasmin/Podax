package com.axelby.podax;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.axelby.podax.player.MP3Player;
import com.axelby.podax.player.AudioPlayerBase;
import com.axelby.podax.player.MediaPlayer;
import com.axelby.podax.player.StreamMP3Player;

import java.util.ArrayList;

class EpisodePlayer {

	// listen for audio focus changes - another app started/stopped, phone call, etc
	private final AudioManager.OnAudioFocusChangeListener _afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
		public void onAudioFocusChange(int focusChange) {
			if (_player == null)
				return;
			if (focusChange == AudioManager.AUDIOFOCUS_LOSS)
				stop();
			else if (focusChange == AudioManager.AUDIOFOCUS_GAIN)
				unpause(Constants.PAUSE_AUDIOFOCUS);
			else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
				pause(Constants.PAUSE_AUDIOFOCUS);
		}
	};

	private final Context _context;
	private final ArrayList<Boolean> _pausingFor = new ArrayList<>(2);

	private OnPauseListener _onPauseListener = null;
	private OnPlayListener _onPlayListener = null;
	private OnStopListener _onStopListener = null;
	private OnSeekListener _onSeekListener = null;
	private OnCompletionListener _onCompletionListener = null;
	private OnChangeListener _onChangeListener = null;

	private Thread _mp3PlayerThread = null; // only has a value if using MP3Player
	private AudioPlayerBase _player = null;

	public EpisodePlayer(Context context) {
		_context = context;
		_pausingFor.add(false);
		_pausingFor.add(false);
	}

	public void changeEpisode(long episodeId) {
		EpisodeCursor episode = EpisodeCursor.getCursor(_context, episodeId);
		if (episode == null)
			return;

		try {
			if (_player != null) {
				_player.stop();
				_player = null;
			}
			if (_mp3PlayerThread != null) {
				try {
					_mp3PlayerThread.join();
				} catch (InterruptedException ex) {
					Log.e("Podax", "player thread interrupted", ex);
				}
				_mp3PlayerThread = null;
			}

			boolean stream = !episode.isDownloaded(_context);

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_context);
			float playbackRate = prefs.getFloat("playbackRate", 1.0f);
			boolean useMP3Player = MP3Player.supports(episode.getFilename(_context));

			if (!useMP3Player)
				_player = new MediaPlayer(episode.getFilename(_context));
			 else if (stream)
				_player = new StreamMP3Player(episode.getFilename(_context), playbackRate);
			else
				_player = new MP3Player(episode.getFilename(_context), playbackRate);
			_mp3PlayerThread = new Thread(_player, "MP3Player");

			if (episode.getLastPosition() != 0)
				_player.seekTo(episode.getLastPosition() / 1000f);

			_player.setOnCompletionListener(new AudioPlayerBase.OnCompletionListener() {
				@Override
				public void onCompletion() {
					if (_onCompletionListener != null)
						_onCompletionListener.onCompletion();
				}
			});
			_player.setPeriodicListener(new AudioPlayerBase.PeriodicListener() {
				@Override
				public void pulse(float position) {
					if (_onSeekListener != null)
						_onSeekListener.onSeek(position);
				}
			});

			if (_onChangeListener != null)
				_onChangeListener.onChange();
		} catch (Exception ex) {
			Log.e("Podax", "unable to change to new episode", ex);
		} finally {
			episode.closeCursor();
		}
	}

	public void setOnPauseListener(OnPauseListener onPauseListener) {
		this._onPauseListener = onPauseListener;
	}
	public void setOnPlayListener(OnPlayListener onPlayListener) {
		this._onPlayListener = onPlayListener;
	}
	public void setOnStopListener(OnStopListener onStopListener) {
		this._onStopListener = onStopListener;
	}
	public void setOnSeekListener(OnSeekListener onSeekListener) {
		this._onSeekListener = onSeekListener;
	}
	public void setOnCompletionListener(OnCompletionListener onCompletionListener) {
		this._onCompletionListener = onCompletionListener;
	}
	public void setOnChangeListener(OnChangeListener onEpisodeChangeListener) {
		this._onChangeListener = onEpisodeChangeListener;
	}

	/* external functions */

	// change position of episode
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

	// resume playing the episode at the current position
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

	// stop the episode with no intention of resuming in the near future
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
		if (_player == null || _player.isPlaying())
			return;

		if (!grabAudioFocus())
			return;

		// make sure we're not pausing
		if (_pausingFor.contains(true))
			return;

		PlayerStatus status = PlayerStatus.getCurrentState(_context);
		if (!status.isEpisodeDownloaded())
			UpdateService.downloadEpisode(_context, status.getEpisodeId());

		_player.resume();
		if (_mp3PlayerThread.getState() == Thread.State.NEW)
			_mp3PlayerThread.start();

		if (_onPlayListener != null)
			_onPlayListener.onPlay(_player.getPosition(), _player.getPlaybackRate());
	}

	private void internalPause() {
		if (_player == null)
			return;

		_player.pause();

		// tell the interested party
		if (_onPauseListener != null)
			_onPauseListener.onPause(_player.getPosition());
	}

	private void internalStop() {
		if (_player == null)
			return;

		// stop playing
		float position = _player.getPosition();
		if (_player.isPlaying()) {
			_player.stop();
			try {
				_mp3PlayerThread.join();
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
		public void onPlay(float positionInSeconds, float playbackRate);
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
	public static interface OnChangeListener {
		public void onChange();
	}
}
