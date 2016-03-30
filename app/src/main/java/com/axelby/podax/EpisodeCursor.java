package com.axelby.podax;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;

import com.axelby.podax.model.EpisodeEditor;
import com.axelby.podax.model.PodaxDB;
import com.axelby.podax.player.AudioPlayerBase;

import java.io.File;
import java.util.Date;

public class EpisodeCursor {

	private final Cursor _cursor;

	private Integer _idColumn = null;
	private Integer _titleColumn = null;
	private Integer _subscriptionIdColumn = null;
	private Integer _subscriptionTitleColumn = null;
	private Integer _subscriptionUrlColumn = null;
	private Integer _playlistPositionColumn = null;
	private Integer _mediaUrlColumn = null;
	private Integer _fileSizeColumn = null;
	private Integer _descriptionColumn = null;
	private Integer _linkColumn = null;
	private Integer _lastPositionColumn = null;
	private Integer _durationColumn = null;
	private Integer _pubDateColumn = null;
	private Integer _gpodderUpdateTimestampColumn = null;
	private Integer _paymentColumn = null;
	private Integer _finishedDateColumn = null;

	public EpisodeCursor(Cursor cursor) {
		_cursor = cursor;
		if (_cursor.isAfterLast())
			return;
		if (_cursor.isBeforeFirst())
			_cursor.moveToFirst();
	}

	public static EpisodeCursor getCursor(Context context, long episodeId) {
		@SuppressLint("Recycle")
		Cursor c = context.getContentResolver().query(EpisodeCursor.getContentUri(episodeId), null, null, null, null);
		if (c == null)
			return null;
		return new EpisodeCursor(c);
	}

	public static long getActiveEpisodeId(Context context) {
		// first check shared pref, then db
		final String PREF_ACTIVE = "active";
		SharedPreferences prefs = context.getSharedPreferences("internals", Context.MODE_PRIVATE);
		long activeId = prefs.getLong(PREF_ACTIVE, -1);
		if (activeId != -1)
			return activeId;

		Cursor c = context.getContentResolver().query(EpisodeProvider.URI,
			new String[] { EpisodeProvider.COLUMN_ID },
			EpisodeProvider.COLUMN_PLAYLIST_POSITION + " = 0", null, null);
		if (c == null || !c.moveToNext())
			return -1;
		activeId = c.getLong(0);
		c.close();
		return activeId;
	}

	private static Uri getContentUri(long id) {
		return ContentUris.withAppendedId(EpisodeProvider.URI, id);
	}

	public void closeCursor() {
		_cursor.close();
	}

	public Uri getContentUri() {
		return EpisodeCursor.getContentUri(getId());
	}

	public long getId() {
		if (_idColumn == null)
			_idColumn = _cursor.getColumnIndexOrThrow(EpisodeProvider.COLUMN_ID);
		return _cursor.getLong(_idColumn);
	}

	public String getTitle() {
		if (_titleColumn == null)
			_titleColumn = _cursor.getColumnIndexOrThrow(EpisodeProvider.COLUMN_TITLE);
		if (_cursor.isNull(_titleColumn))
			return null;
		return _cursor.getString(_titleColumn);
	}

	public Long getSubscriptionId() {
		if (_subscriptionIdColumn == null)
			_subscriptionIdColumn = _cursor.getColumnIndexOrThrow(EpisodeProvider.COLUMN_SUBSCRIPTION_ID);
		if (_cursor.isNull(_subscriptionIdColumn))
			return null;
		return _cursor.getLong(_subscriptionIdColumn);
	}

	public String getSubscriptionTitle() {
		if (_subscriptionTitleColumn == null)
			_subscriptionTitleColumn = _cursor.getColumnIndexOrThrow(EpisodeProvider.COLUMN_SUBSCRIPTION_TITLE);
		if (_cursor.isNull(_subscriptionTitleColumn))
			return null;
		return _cursor.getString(_subscriptionTitleColumn);
	}

	public String getSubscriptionUrl() {
		if (_subscriptionUrlColumn == null)
			_subscriptionUrlColumn = _cursor.getColumnIndexOrThrow(EpisodeProvider.COLUMN_SUBSCRIPTION_URL);
		if (_cursor.isNull(_subscriptionUrlColumn))
			return null;
		return _cursor.getString(_subscriptionUrlColumn);
	}

	public String getMediaUrl() {
		if (_mediaUrlColumn == null)
			_mediaUrlColumn = _cursor.getColumnIndexOrThrow(EpisodeProvider.COLUMN_MEDIA_URL);
		if (_cursor.isNull(_mediaUrlColumn))
			return null;
		return _cursor.getString(_mediaUrlColumn);
	}

	public Long getFileSize() {
		if (_fileSizeColumn == null)
			_fileSizeColumn = _cursor.getColumnIndexOrThrow(EpisodeProvider.COLUMN_FILE_SIZE);
		if (_cursor.isNull(_fileSizeColumn))
			return null;
		return _cursor.getLong(_fileSizeColumn);
	}

	public Integer getPlaylistPosition() {
		if (_playlistPositionColumn == null)
			_playlistPositionColumn = _cursor.getColumnIndex(EpisodeProvider.COLUMN_PLAYLIST_POSITION);
		if (_cursor.isNull(_playlistPositionColumn))
			return null;
		return _cursor.getInt(_playlistPositionColumn);
	}

	public String getDescription() {
		if (_descriptionColumn == null)
			_descriptionColumn = _cursor.getColumnIndexOrThrow(EpisodeProvider.COLUMN_DESCRIPTION);
		if (_cursor.isNull(_descriptionColumn))
			return null;
		return _cursor.getString(_descriptionColumn);
	}

	public String getLink() {
		if (_linkColumn == null)
			_linkColumn = _cursor.getColumnIndexOrThrow(EpisodeProvider.COLUMN_LINK);
		if (_cursor.isNull(_linkColumn))
			return null;
		return _cursor.getString(_linkColumn);
	}

	public Integer getLastPosition() {
		if (_lastPositionColumn == null)
			_lastPositionColumn = _cursor.getColumnIndexOrThrow(EpisodeProvider.COLUMN_LAST_POSITION);
		if (_cursor.isNull(_lastPositionColumn))
			return null;
		return _cursor.getInt(_lastPositionColumn);
	}

	public Integer getDuration() {
		if (_durationColumn == null)
			_durationColumn = _cursor.getColumnIndexOrThrow(EpisodeProvider.COLUMN_DURATION);
		if (_cursor.isNull(_durationColumn))
			return null;
		return _cursor.getInt(_durationColumn);
	}

	public Date getPubDate() {
		if (_pubDateColumn == null)
			_pubDateColumn = _cursor.getColumnIndexOrThrow(EpisodeProvider.COLUMN_PUB_DATE);
		if (_cursor.isNull(_pubDateColumn))
			return null;
		return new Date(_cursor.getLong(_pubDateColumn) * 1000);
	}

	public Date getGPodderUpdateTimestamp() {
		if (_gpodderUpdateTimestampColumn == null)
			_gpodderUpdateTimestampColumn = _cursor.getColumnIndexOrThrow(EpisodeProvider.COLUMN_GPODDER_UPDATE_TIMESTAMP);
		if (_cursor.isNull(_gpodderUpdateTimestampColumn))
			return null;
		return new Date(_cursor.getLong(_gpodderUpdateTimestampColumn));
	}

	public String getFilename(Context context) {
		return EpisodeCursor.getPodcastStoragePath(context) + String.valueOf(getId()) + "." + EpisodeCursor.getExtension(getMediaUrl());
	}

	public boolean isDownloaded(Context context) {
		if (getFileSize() == null)
			return false;
		File file = new File(getFilename(context));
		return file.exists() && file.length() == getFileSize() && getFileSize() != 0;
	}

	public static String getExtension(String filename) {
		// dissect the url to get the filename portion
		int s = filename.lastIndexOf('/');
		filename = filename.substring(s + 1);
		int q = filename.indexOf('?');
		if (q != -1)
			filename = filename.substring(0, q);

		String extension = "";
		int i = filename.lastIndexOf('.');
		if (i > 0)
			extension = filename.substring(i + 1);
		return extension;
	}

	public static String getPodcastStoragePath(Context context) {
		File podaxFile = new File(Storage.getStoragePath(context), "Podcasts");
		if (!podaxFile.exists())
			podaxFile.mkdirs();
		return podaxFile.getAbsolutePath() + "/";
	}

	public String getPaymentUrl() {
		if (_paymentColumn == null)
			_paymentColumn = _cursor.getColumnIndexOrThrow(EpisodeProvider.COLUMN_PAYMENT);
		if (_cursor.isNull(_paymentColumn))
			return null;
		return _cursor.getString(_paymentColumn);
	}

	public Date getFinishedDate() {
		if (_finishedDateColumn == null)
			_finishedDateColumn = _cursor.getColumnIndexOrThrow(EpisodeProvider.COLUMN_FINISHED_TIME);
		if (_cursor.isNull(_finishedDateColumn ))
			return null;
		return new Date(_cursor.getLong(_finishedDateColumn) * 1000);
	}

	public int determineDuration(Context context) {
		int duration = (int) (AudioPlayerBase.determineDuration(getFilename(context)) * 1000);
		new EpisodeEditor(getId()).setDuration(duration).commit();
		return duration;
	}
}
