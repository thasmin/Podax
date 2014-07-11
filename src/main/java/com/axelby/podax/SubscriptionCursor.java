package com.axelby.podax;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class SubscriptionCursor {

	private Cursor _cursor;

	private Integer _idColumn = null;
	private Integer _titleColumn = null;
	private Integer _urlColumn = null;
	private Integer _lastModifiedColumn = null;
	private Integer _lastUpdateColumn = null;
	private Integer _etagColumn = null;
	private Integer _thumbnailColumn = null;
	private Integer _titleOverrideColumn = null;
	private Integer _playlistNewColumn = null;
	private Integer _expirationDaysColumn = null;

	public SubscriptionCursor(Cursor cursor) {
		if (cursor.isAfterLast())
			return;
		_cursor = cursor;
	}

	public boolean isNull() {
		return _cursor == null;
	}

	public Uri getContentUri() {
		if (getId() == null)
			return null;
		return ContentUris.withAppendedId(SubscriptionProvider.URI, getId());
	}

	public Long getId() {
		if (_idColumn == null)
			_idColumn = _cursor.getColumnIndexOrThrow(SubscriptionProvider.COLUMN_ID);
		if (_cursor.isNull(_idColumn))
			return null;
		return _cursor.getLong(_idColumn);
	}

	public String getTitle() {
		if (_titleOverrideColumn == null)
			_titleOverrideColumn = _cursor.getColumnIndex(SubscriptionProvider.COLUMN_TITLE_OVERRIDE);
		if (_titleOverrideColumn != -1 && !_cursor.isNull(_titleOverrideColumn))
			return _cursor.getString(_titleOverrideColumn);
		if (_titleColumn == null)
			_titleColumn = _cursor.getColumnIndexOrThrow(SubscriptionProvider.COLUMN_TITLE);
		if (_cursor.isNull(_titleColumn))
			return getUrl();
		return _cursor.getString(_titleColumn);
	}

	public String getUrl() {
		if (_urlColumn == null)
			_urlColumn = _cursor.getColumnIndexOrThrow(SubscriptionProvider.COLUMN_URL);
		if (_cursor.isNull(_urlColumn))
			return null;
		return _cursor.getString(_urlColumn);
	}

	public Date getLastModified() {
		if (_lastModifiedColumn == null)
			_lastModifiedColumn = _cursor.getColumnIndexOrThrow(SubscriptionProvider.COLUMN_LAST_MODIFIED);
		if (_cursor.isNull(_lastModifiedColumn))
			return null;
		return new Date(_cursor.getLong(_lastModifiedColumn) * 1000);
	}

	public Date getLastUpdate() {
		if (_lastUpdateColumn == null)
			_lastUpdateColumn = _cursor.getColumnIndexOrThrow(SubscriptionProvider.COLUMN_LAST_UPDATE);
		if (_cursor.isNull(_lastUpdateColumn))
			return null;
		return new Date(_cursor.getLong(_lastUpdateColumn) * 1000);
	}

	public String getETag() {
		if (_etagColumn == null)
			_etagColumn = _cursor.getColumnIndexOrThrow(SubscriptionProvider.COLUMN_ETAG);
		if (_cursor.isNull(_etagColumn))
			return null;
		return _cursor.getString(_etagColumn);
	}

	public String getThumbnail() {
		if (_thumbnailColumn == null)
			_thumbnailColumn = _cursor.getColumnIndexOrThrow(SubscriptionProvider.COLUMN_THUMBNAIL);
		if (_cursor.isNull(_thumbnailColumn))
			return null;
		return _cursor.getString(_thumbnailColumn);
	}

	private static String getThumbnailFilename(Context context, long subscriptionId) {
		String storagePath = EpisodeCursor.getStoragePath(context);
		return storagePath + String.valueOf(subscriptionId) + "podcast.image";
	}

	public static Bitmap getThumbnailImage(Context context, long subscriptionId) {
		String filename = getThumbnailFilename(context, subscriptionId);
		if (!new File(filename).exists())
			return null;
		return BitmapFactory.decodeFile(filename);
	}

	public static void evictThumbnails(Context context, long subscriptionId) {
		File thumbnail = new File(getThumbnailFilename(context, subscriptionId));
		if (!thumbnail.exists())
			return;
		thumbnail.delete();
	}

	public static void saveThumbnailImage(Context context, long subscriptionId, Bitmap thumbnail) {
		try {
			String filename = getThumbnailFilename(context, subscriptionId);
			new File(filename).delete();
			BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(filename));
			thumbnail.compress(Bitmap.CompressFormat.PNG, 95, outputStream);
			outputStream.close();
		} catch (FileNotFoundException e) {
			Log.e("Podax", "unable to save subscription thumbnail", e);
		} catch (IOException e) {
			Log.e("Podax", "unable to save subscription thumbnail", e);
		}
	}

	public boolean getQueueNew() {
		if (_playlistNewColumn == null)
			_playlistNewColumn = _cursor.getColumnIndexOrThrow(SubscriptionProvider.COLUMN_PLAYLIST_NEW);
		if (_cursor.isNull(_playlistNewColumn))
			return true;
		return _cursor.getInt(_playlistNewColumn) != 0;
	}

	public String getTitleOverride() {
		if (_titleOverrideColumn == null)
			_titleOverrideColumn = _cursor.getColumnIndexOrThrow(SubscriptionProvider.COLUMN_TITLE_OVERRIDE);
		if (_cursor.isNull(_titleOverrideColumn))
			return null;
		return _cursor.getString(_titleOverrideColumn);
	}

	public Integer getExpirationDays() {
		if (_expirationDaysColumn == null)
			_expirationDaysColumn = _cursor.getColumnIndexOrThrow(SubscriptionProvider.COLUMN_EXPIRATION);
		if (_cursor.isNull(_thumbnailColumn))
			return null;
		return _cursor.getInt(_expirationDaysColumn);
	}
}
