package com.axelby.podax;

import java.io.File;
import java.util.Date;

import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

public class SubscriptionCursor {

	private Context _context;
	private Cursor _cursor;
	
	private Integer _idColumn = null;
	private Integer _titleColumn = null;
	private Integer _urlColumn = null;
	private Integer _lastModifiedColumn = null;
	private Integer _lastUpdateColumn = null;
	private Integer _etagColumn = null;
	private Integer _thumbnailColumn = null;

	public SubscriptionCursor(Context context, Cursor cursor) {
		_context = context;
		if (cursor.isAfterLast())
			return;
		_cursor = cursor;

		// duplicated code to avoid a throws clause in the constructor
		_idColumn = _cursor.getColumnIndex(SubscriptionProvider.COLUMN_ID);
		if (!_cursor.isNull(_idColumn))
			cursor.setNotificationUri(_context.getContentResolver(), ContentUris.withAppendedId(SubscriptionProvider.URI, _cursor.getLong(_idColumn)));

	}
	
	public boolean isNull() {
		return _cursor == null;
	}

	public Uri getContentUri() {
		if (getId() == null)
			return null;
		return ContentUris.withAppendedId(SubscriptionProvider.URI, getId());
	}

	public void registerContentObserver(ContentObserver observer) {
		if (getId() == null)
			return;
		_context.getContentResolver().registerContentObserver(getContentUri(), false, observer);
	}

	public void unregisterContentObserver(ContentObserver observer) {
		_context.getContentResolver().unregisterContentObserver(observer);
	}

	public Long getId() {
		if (_idColumn == null)
			_idColumn = _cursor.getColumnIndexOrThrow(SubscriptionProvider.COLUMN_ID);
		if (_cursor.isNull(_idColumn))
			return null;
		return _cursor.getLong(_idColumn);
	}

	public String getTitle() {
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

	public String getThumbnailFilename() {
		String externalPath = Environment.getExternalStorageDirectory().getAbsolutePath();
		String podaxDir = externalPath + "/Android/data/com.axelby.podax/files/";
		File podaxFile = new File(podaxDir);
		if (!podaxFile.exists())
			podaxFile.mkdirs();
		return podaxDir + "/" + getId() + ".jpg";
	}

}
