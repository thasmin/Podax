package com.axelby.podax;

import java.util.Date;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;

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
	private Integer _queueNewColumn = null;
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

	public boolean getQueueNew() {
		if (_queueNewColumn == null)
			_queueNewColumn = _cursor.getColumnIndexOrThrow(SubscriptionProvider.COLUMN_QUEUE_NEW);
		if (_cursor.isNull(_queueNewColumn))
			return true;
		return _cursor.getInt(_queueNewColumn) != 0;
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
