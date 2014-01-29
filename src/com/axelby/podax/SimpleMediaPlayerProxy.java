package com.axelby.podax;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

import com.android.ex.variablespeed.MediaPlayerProxy;

import java.io.IOException;

public class SimpleMediaPlayerProxy implements MediaPlayerProxy {
	private final MediaPlayer _player;

	public SimpleMediaPlayerProxy() {
		_player = new MediaPlayer();
	}

	@Override
	public void setOnErrorListener(MediaPlayer.OnErrorListener listener) {
		_player.setOnErrorListener(listener);
	}

	@Override
	public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
		_player.setOnCompletionListener(listener);
	}

	@Override
	public void release() {
		_player.release();
	}

	@Override
	public void reset() {
		_player.reset();
	}

	@Override
	public void setDataSource(String path) throws IllegalStateException, IOException {
		_player.setDataSource(path);
	}

	@Override
	public void setDataSource(Context context, Uri intentUri) throws IllegalStateException, IOException {
		_player.setDataSource(context, intentUri);
	}

	@Override
	public void prepare() throws IOException {
		_player.prepare();
	}

	@Override
	public int getDuration() {
		return _player.getDuration();
	}

	@Override
	public void seekTo(int startPosition) {
		_player.seekTo(startPosition);
	}

	@Override
	public void start() {
		_player.start();
	}

	@Override
	public boolean isPlaying() {
		return _player.isPlaying();
	}

	@Override
	public int getCurrentPosition() {
		return _player.getCurrentPosition();
	}

	@Override
	public void pause() {
		_player.pause();
	}

	@Override
	public void setAudioStreamType(int streamType) {
		_player.setAudioStreamType(streamType);
	}

	@Override
	public void prepareAsync() throws IOException {
		_player.prepareAsync();
	}

	@Override
	public void setOnPreparedListener(MediaPlayer.OnPreparedListener listener) {
		_player.setOnPreparedListener(listener);
	}

	@Override
	public void stop() {
		_player.stop();
	}
}
