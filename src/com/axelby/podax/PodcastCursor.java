package com.axelby.podax;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

public class PodcastCursor {

	private Cursor _cursor;
	
	private Integer _idColumn = null;
	private Integer _titleColumn = null;
	private Integer _subscriptionTitleColumn = null;
	private Integer _subscriptionIdColumn = null;
	private Integer _queuePositionColumn = null;
	private Integer _mediaUrlColumn = null;
	private Integer _fileSizeColumn = null;
	private Integer _descriptionColumn = null;
	private Integer _lastPositionColumn = null;
	private Integer _durationColumn = null;
	private Integer _pubDateColumn = null;

	public PodcastCursor(Cursor cursor) {
		_cursor = cursor;
		if (_cursor.getCount() == 0)
			return;
		if (_cursor.isBeforeFirst())
			_cursor.moveToFirst();
	}

	public boolean isNull() {
		return _cursor.isAfterLast();
	}

	public void closeCursor() {
		if (!_cursor.isClosed())
			_cursor.close();
	}

	public Uri getContentUri() {
		if (getId() == null)
			return null;
		return ContentUris.withAppendedId(PodcastProvider.URI, getId());
	}

	public Long getId() {
		if (_idColumn == null)
			_idColumn = _cursor.getColumnIndexOrThrow(PodcastProvider.COLUMN_ID);
		if (_cursor.isNull(_idColumn))
			return null;
		return _cursor.getLong(_idColumn);
	}

	public String getTitle() {
		if (_titleColumn == null)
			_titleColumn = _cursor.getColumnIndexOrThrow(PodcastProvider.COLUMN_TITLE);
		if (_cursor.isNull(_titleColumn))
			return null;
		return _cursor.getString(_titleColumn);
	}

	public Long getSubscriptionId() {
		if (_subscriptionIdColumn == null)
			_subscriptionIdColumn = _cursor.getColumnIndexOrThrow(PodcastProvider.COLUMN_SUBSCRIPTION_ID);
		if (_cursor.isNull(_subscriptionIdColumn))
			return null;
		return _cursor.getLong(_subscriptionIdColumn);
	}

	public String getSubscriptionTitle() {
		if (_subscriptionTitleColumn == null)
			_subscriptionTitleColumn = _cursor.getColumnIndexOrThrow(PodcastProvider.COLUMN_SUBSCRIPTION_TITLE);
		if (_cursor.isNull(_subscriptionTitleColumn))
			return null;
		return _cursor.getString(_subscriptionTitleColumn);
	}

	public String getMediaUrl() {
		if (_mediaUrlColumn == null)
			_mediaUrlColumn = _cursor.getColumnIndexOrThrow(PodcastProvider.COLUMN_MEDIA_URL);
		if (_cursor.isNull(_mediaUrlColumn))
			return null;
		return _cursor.getString(_mediaUrlColumn);
	}

	public Integer getFileSize() {
		if (_fileSizeColumn == null)
			_fileSizeColumn = _cursor.getColumnIndexOrThrow(PodcastProvider.COLUMN_FILE_SIZE);
		if (_cursor.isNull(_fileSizeColumn))
			return null;
		return _cursor.getInt(_fileSizeColumn);
	}

	public Integer getQueuePosition() {
		if (_queuePositionColumn == null)
			_queuePositionColumn = _cursor.getColumnIndex(PodcastProvider.COLUMN_QUEUE_POSITION);
		if (_cursor.isNull(_queuePositionColumn))
			return null;
		return _cursor.getInt(_queuePositionColumn);
	}

	public String getDescription() {
		if (_descriptionColumn == null)
			_descriptionColumn = _cursor.getColumnIndexOrThrow(PodcastProvider.COLUMN_DESCRIPTION);
		if (_cursor.isNull(_descriptionColumn))
			return null;
		return _cursor.getString(_descriptionColumn);
	}

	public Integer getLastPosition() {
		if (_lastPositionColumn == null)
			_lastPositionColumn = _cursor.getColumnIndexOrThrow(PodcastProvider.COLUMN_LAST_POSITION);
		if (_cursor.isNull(_lastPositionColumn))
			return null;
		return _cursor.getInt(_lastPositionColumn);
	}

	public Integer getDuration() {
		if (_durationColumn == null)
			_durationColumn = _cursor.getColumnIndexOrThrow(PodcastProvider.COLUMN_DURATION);
		if (_cursor.isNull(_durationColumn))
			return null;
		return _cursor.getInt(_durationColumn);
	}

	public Date getPubDate() {
		if (_pubDateColumn == null)
			_pubDateColumn = _cursor.getColumnIndexOrThrow(PodcastProvider.COLUMN_PUB_DATE);
		if (_cursor.isNull(_pubDateColumn))
			return null;
		return new Date(_cursor.getLong(_durationColumn) * 1000);
	}

	public String getFilename() {
		return PodcastCursor.getStoragePath() + String.valueOf(getId()) + "." + PodcastCursor.getExtension(getMediaUrl());
	}

	public boolean isDownloaded() {
		if (getFileSize() == null)
			return false;
		File file = new File(getFilename());
		return file.exists() && file.length() == getFileSize() && getFileSize() != 0;
	}

	public static String getExtension(String filename) {
		String extension = "";
		int i = filename.lastIndexOf('.');
		if (i > 0)
		    extension = filename.substring(i+1);
		return extension;
	}

	public static String getStoragePath() {
		String externalPath = Environment.getExternalStorageDirectory().getAbsolutePath();
		String podaxDir = externalPath + "/Android/data/com.axelby.podax/files/";
		File podaxFile = new File(podaxDir);
		if (!podaxFile.exists())
			podaxFile.mkdirs();
		return podaxDir;
	}

	public String getThumbnailFilename() {
		String externalPath = Environment.getExternalStorageDirectory().getAbsolutePath();
		String podaxDir = externalPath + "/Android/data/com.axelby.podax/files/";
		File podaxFile = new File(podaxDir);
		if (!podaxFile.exists())
			podaxFile.mkdirs();
		return podaxDir + "/" + getSubscriptionId() + ".jpg";
	}

	// setters

	public void setFileSize(Context context, long fileSize) {
		ContentValues values = new ContentValues();
		values.put(PodcastProvider.COLUMN_FILE_SIZE, fileSize);
		context.getContentResolver().update(getContentUri(), values, null, null);
	}

	public void setLastPosition(Context context, long lastPosition) {
		ContentValues values = new ContentValues();
		values.put(PodcastProvider.COLUMN_LAST_POSITION, lastPosition);
		context.getContentResolver().update(getContentUri(), values, null, null);
	}

	public void removeFromQueue(Context context) {
		ContentValues values = new ContentValues();
		values.put(PodcastProvider.COLUMN_QUEUE_POSITION, (Integer)null);
		context.getContentResolver().update(getContentUri(), values, null, null);
	}

	public void addToQueue(Context context) {
		ContentValues values = new ContentValues();
		values.put(PodcastProvider.COLUMN_QUEUE_POSITION, Integer.MAX_VALUE);
		context.getContentResolver().update(getContentUri(), values, null, null);
	}

	public void moveToFirstInQueue(Context context) {
		ContentValues values = new ContentValues();
		values.put(PodcastProvider.COLUMN_QUEUE_POSITION, 0);
		context.getContentResolver().update(getContentUri(), values, null, null);
	}

	public void determineDuration(Context context) {
		MediaPlayer mp = new MediaPlayer();
		try {
			mp.setDataSource(this.getFilename());
			mp.prepare();
			ContentValues values = new ContentValues();
			values.put(PodcastProvider.COLUMN_DURATION, mp.getDuration());
			context.getContentResolver().update(getContentUri(), values, null, null);
		} catch (IOException ex) {
			Log.e("Podax", "Unable to determine length of " + this.getFilename());
		} finally {
			if (mp != null)
				mp.release();
		}
	}
}
