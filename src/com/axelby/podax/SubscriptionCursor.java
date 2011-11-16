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
		if (cursor.getCount() == 0)
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

	public Uri getContentUri() throws MissingFieldException {
		if (getId() == null)
			return null;
		return ContentUris.withAppendedId(SubscriptionProvider.URI, getId());
	}

	public void registerContentObserver(ContentObserver observer) throws MissingFieldException {
		if (getId() == null)
			return;
		_context.getContentResolver().registerContentObserver(getContentUri(), false, observer);
	}

	public void unregisterContentObserver(ContentObserver observer) {
		_context.getContentResolver().unregisterContentObserver(observer);
	}

	public Long getId() throws MissingFieldException {
		if (_idColumn == -1)
			throw new MissingFieldException();
		if (_cursor.isNull(_idColumn))
			return null;
		return _cursor.getLong(_idColumn);
	}

	public String getTitle() throws MissingFieldException {
		if (_titleColumn == null)
			_titleColumn = _cursor.getColumnIndex(SubscriptionProvider.COLUMN_TITLE);
		if (_titleColumn == -1)
			throw new MissingFieldException();
		if (_cursor.isNull(_titleColumn))
			return null;
		return _cursor.getString(_titleColumn);
	}

	public String getUrl() throws MissingFieldException {
		if (_urlColumn == null)
			_urlColumn = _cursor.getColumnIndex(SubscriptionProvider.COLUMN_URL);
		if (_urlColumn == -1)
			throw new MissingFieldException();
		if (_cursor.isNull(_urlColumn))
			return null;
		return _cursor.getString(_urlColumn);
	}

	public Date getLastModified() throws MissingFieldException {
		if (_lastModifiedColumn == null)
			_lastModifiedColumn = _cursor.getColumnIndex(SubscriptionProvider.COLUMN_LAST_MODIFIED);
		if (_titleColumn == -1)
			throw new MissingFieldException();
		if (_cursor.isNull(_lastModifiedColumn))
			return null;
		return new Date(_cursor.getLong(_lastModifiedColumn) * 1000);
	}

	public Date getLastUpdate() throws MissingFieldException {
		if (_lastUpdateColumn == null)
			_lastUpdateColumn = _cursor.getColumnIndex(SubscriptionProvider.COLUMN_LAST_UPDATE);
		if (_lastUpdateColumn == -1)
			throw new MissingFieldException();
		if (_cursor.isNull(_lastUpdateColumn))
			return null;
		return new Date(_cursor.getLong(_lastUpdateColumn) * 1000);
	}

	public String getETag() throws MissingFieldException {
		if (_etagColumn == null)
			_etagColumn = _cursor.getColumnIndex(SubscriptionProvider.COLUMN_ETAG);
		if (_etagColumn == -1)
			throw new MissingFieldException();
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

	public String getThumbnailFilename() throws MissingFieldException {
		String externalPath = Environment.getExternalStorageDirectory().getAbsolutePath();
		String podaxDir = externalPath + "/Android/data/com.axelby.podax/files/";
		File podaxFile = new File(podaxDir);
		if (!podaxFile.exists())
			podaxFile.mkdirs();
		return podaxDir + "/" + getId() + ".jpg";
	}

}
