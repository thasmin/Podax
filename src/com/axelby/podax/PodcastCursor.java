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

public class PodcastCursor {

	private Cursor _cursor;
	
	private Integer _idColumn = null;
	private Integer _titleColumn = null;
	private Integer _subscriptionIdColumn = null;
	private Integer _subscriptionTitleColumn = null;
	private Integer _subscriptionThumbnailColumn = null;
	private Integer _queuePositionColumn = null;
	private Integer _mediaUrlColumn = null;
	private Integer _fileSizeColumn = null;
	private Integer _descriptionColumn = null;
	private Integer _lastPositionColumn = null;
	private Integer _durationColumn = null;
	private Integer _pubDateColumn = null;
	private Integer _downloadIdColumn = null;
	private Integer _paymentColumn = null;

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

	public Uri getContentUri() {
		return ContentUris.withAppendedId(PodcastProvider.URI, getId());
	}

	public long getId() {
		if (_idColumn == null)
			_idColumn = _cursor.getColumnIndexOrThrow(PodcastProvider.COLUMN_ID);
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

	public String getSubscriptionThumbnailUrl() {
		if (_subscriptionThumbnailColumn == null)
			_subscriptionThumbnailColumn = _cursor.getColumnIndexOrThrow(PodcastProvider.COLUMN_SUBSCRIPTION_THUMBNAIL);
		if (_cursor.isNull(_subscriptionThumbnailColumn))
			return null;
		return _cursor.getString(_subscriptionThumbnailColumn);
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

	public Long getDownloadId() {
		if (_downloadIdColumn == null)
			_downloadIdColumn = _cursor.getColumnIndexOrThrow(PodcastProvider.COLUMN_DOWNLOAD_ID);
		if (_cursor.isNull(_downloadIdColumn))
			return null;
		return _cursor.getLong(_downloadIdColumn);
	}

	public String getFilename(Context context) {
		return PodcastCursor.getStoragePath(context) + String.valueOf(getId()) + "." + PodcastCursor.getExtension(getMediaUrl());
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
		    extension = filename.substring(i+1);
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

	public String getPaymentUrl() {
		if (_paymentColumn == null)
			_paymentColumn = _cursor.getColumnIndexOrThrow(PodcastProvider.COLUMN_PAYMENT);
		if (_cursor.isNull(_paymentColumn))
			return null;
		return _cursor.getString(_paymentColumn);
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

	public int determineDuration(Context context) {
		MediaPlayer mp = new MediaPlayer();
		try {
			mp.setDataSource(getFilename(context));
			mp.prepare();
			ContentValues values = new ContentValues();
			values.put(PodcastProvider.COLUMN_DURATION, mp.getDuration());
			context.getContentResolver().update(getContentUri(), values, null, null);
			return mp.getDuration();
		} catch (IOException ex) {
			PodaxLog.log(context, "Unable to determine length of " + getFilename(context) + ": " + ex.getMessage());
			return 0;
		} finally {
			if (mp != null)
				mp.release();
		}
	}
}
