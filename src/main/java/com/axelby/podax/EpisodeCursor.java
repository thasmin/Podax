package com.axelby.podax;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.axelby.podax.player.AudioPlayer;
import com.axelby.podax.player.IMediaDecoder;

import java.io.File;
import java.util.Date;

public class EpisodeCursor {

	private Cursor _cursor;

	private Integer _idColumn = null;
	private Integer _titleColumn = null;
	private Integer _subscriptionIdColumn = null;
	private Integer _subscriptionTitleColumn = null;
	private Integer _subscriptionUrlColumn = null;
	private Integer _playlistPositionColumn = null;
	private Integer _mediaUrlColumn = null;
	private Integer _fileSizeColumn = null;
	private Integer _descriptionColumn = null;
	private Integer _lastPositionColumn = null;
	private Integer _durationColumn = null;
	private Integer _pubDateColumn = null;
	private Integer _downloadIdColumn = null;
	private Integer _gpodderUpdateTimestampColumn = null;
	private Integer _paymentColumn = null;

	public EpisodeCursor(Cursor cursor) {
		_cursor = cursor;
		if (_cursor.getCount() == 0)
			return;
		if (_cursor.isBeforeFirst())
			_cursor.moveToFirst();
	}

	public boolean isNull() {
		return _cursor.isAfterLast();
	}

	public Uri getContentUri() {
		return ContentUris.withAppendedId(EpisodeProvider.URI, getId());
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

	public Integer getFileSize() {
		if (_fileSizeColumn == null)
			_fileSizeColumn = _cursor.getColumnIndexOrThrow(EpisodeProvider.COLUMN_FILE_SIZE);
		if (_cursor.isNull(_fileSizeColumn))
			return null;
		return _cursor.getInt(_fileSizeColumn);
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

	public Long getDownloadId() {
		if (_downloadIdColumn == null)
			_downloadIdColumn = _cursor.getColumnIndexOrThrow(EpisodeProvider.COLUMN_DOWNLOAD_ID);
		if (_cursor.isNull(_downloadIdColumn))
			return null;
		return _cursor.getLong(_downloadIdColumn);
	}

	public Date getGPodderUpdateTimestamp() {
		if (_gpodderUpdateTimestampColumn == null)
			_gpodderUpdateTimestampColumn = _cursor.getColumnIndexOrThrow(EpisodeProvider.COLUMN_GPODDER_UPDATE_TIMESTAMP);
		if (_cursor.isNull(_gpodderUpdateTimestampColumn))
			return null;
		return new Date(_cursor.getLong(_gpodderUpdateTimestampColumn));
	}

	public String getFilename(Context context) {
		return EpisodeCursor.getStoragePath(context) + String.valueOf(getId()) + "." + EpisodeCursor.getExtension(getMediaUrl());
	}
	public String getOldFilename(Context context) {
		return EpisodeCursor.getOldStoragePath(context) + String.valueOf(getId()) + "." + EpisodeCursor.getExtension(getMediaUrl());
	}

	public static String getIndexFilename(Context context, long id) {
		String externalPath = Storage.getExternalStorageDirectory(context).getAbsolutePath();
		String podaxDir = externalPath + "/Android/data/com.axelby.podax/files/";
		return podaxDir + String.valueOf(id) + ".index";
	}
	public String getIndexFilename(Context context) {
		return EpisodeCursor.getIndexFilename(context, getId());
	}

	public static String getDownloadingIndicatorFilename(String externalPath, long id) {
		String podaxDir = externalPath + "/Android/data/com.axelby.podax/files/";
		return podaxDir + String.valueOf(id) + ".downloading";
	}
	public static String getDownloadingIndicatorFilename(Context context, long id) {
		String externalPath = Storage.getExternalStorageDirectory(context).getAbsolutePath();
		return getDownloadingIndicatorFilename(externalPath, id);
	}
	public String getDownloadingIndicatorFilename(Context context) {
		return getDownloadingIndicatorFilename(context, getId());
	}

	public static String extractExternalStorageDirectory(String filename) {
		int podaxSpecificStart = filename.indexOf("/Android/data/com.axelby.podax/files/");
		return filename.substring(0, podaxSpecificStart);
	}
	public static long extractIdFromFilename(String filename) {
		return 0l;
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

	public static String getStoragePath(Context context) {
		String externalPath = Storage.getExternalStorageDirectory(context).getAbsolutePath();
		String podaxDir = externalPath + "/Android/data/com.axelby.podax/files/Podcasts/";
		File podaxFile = new File(podaxDir);
		if (!podaxFile.exists())
			podaxFile.mkdirs();
		return podaxDir;
	}
	public static String getOldStoragePath(Context context) {
		String externalPath = Storage.getExternalStorageDirectory(context).getAbsolutePath();
		String podaxDir = externalPath + "/Android/data/com.axelby.podax/files/";
		File podaxFile = new File(podaxDir);
		if (!podaxFile.exists())
			podaxFile.mkdirs();
		return podaxDir;
	}

	public String getPaymentUrl() {
		if (_paymentColumn == null)
			_paymentColumn = _cursor.getColumnIndexOrThrow(EpisodeProvider.COLUMN_PAYMENT);
		if (_cursor.isNull(_paymentColumn))
			return null;
		return _cursor.getString(_paymentColumn);
	}

	public void removeFromPlaylist(Context context) {
		ContentValues values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, (Integer) null);
		context.getContentResolver().update(getContentUri(), values, null, null);
	}

	public void addToPlaylist(Context context) {
		ContentValues values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, Integer.MAX_VALUE);
		context.getContentResolver().update(getContentUri(), values, null, null);
	}

	public int determineDuration(Context context) {
		IMediaDecoder decoder = AudioPlayer.loadFile(getFilename(context));
		if (decoder == null)
			return 0;
		int duration = (int) (decoder.getDuration() * 1000);
		decoder.close();

		ContentValues values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_DURATION, duration);
		context.getContentResolver().update(getContentUri(), values, null, null);
		return duration;
	}
}
