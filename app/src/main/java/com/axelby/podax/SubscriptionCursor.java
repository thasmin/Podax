package com.axelby.podax;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.axelby.podax.model.SubscriptionDB;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.io.File;
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
	private Integer _descriptionColumn = null;
	private Integer _singleUseColumn = null;
	private Integer _playlistNewColumn = null;
	private Integer _expirationColumn = null;

	public SubscriptionCursor(Cursor cursor) {
		_cursor = cursor;
		if (_cursor.isAfterLast())
			return;
		if (_cursor.isBeforeFirst())
			_cursor.moveToFirst();
	}

	private static Uri getContentUri(long id) {
		return ContentUris.withAppendedId(SubscriptionProvider.URI, id);
	}

	public void closeCursor() {
		_cursor.close();
	}

	public Uri getContentUri() {
		if (getId() == null)
			return null;
		return ContentUris.withAppendedId(SubscriptionProvider.URI, getId());
	}

	public Long getId() {
		if (_idColumn == null)
			_idColumn = _cursor.getColumnIndexOrThrow(SubscriptionDB.COLUMN_ID);
		if (_cursor.isNull(_idColumn))
			return null;
		return _cursor.getLong(_idColumn);
	}

	public String getTitle() {
		if (_titleOverrideColumn == null)
			_titleOverrideColumn = _cursor.getColumnIndex(SubscriptionDB.COLUMN_TITLE_OVERRIDE);
		if (_titleOverrideColumn != -1 && !_cursor.isNull(_titleOverrideColumn))
			return _cursor.getString(_titleOverrideColumn);
		if (_titleColumn == null)
			_titleColumn = _cursor.getColumnIndexOrThrow(SubscriptionDB.COLUMN_TITLE);
		if (_cursor.isNull(_titleColumn))
			return getUrl();
		return _cursor.getString(_titleColumn);
	}

	public String getRawTitle() {
		if (_titleColumn == null)
			_titleColumn = _cursor.getColumnIndexOrThrow(SubscriptionDB.COLUMN_TITLE);
		return _cursor.getString(_titleColumn);
	}

	public String getTitleOverride() {
		if (_titleOverrideColumn == null)
			_titleOverrideColumn = _cursor.getColumnIndexOrThrow(SubscriptionDB.COLUMN_TITLE_OVERRIDE);
		return _cursor.getString(_titleOverrideColumn);
	}

	public String getUrl() {
		if (_urlColumn == null)
			_urlColumn = _cursor.getColumnIndexOrThrow(SubscriptionDB.COLUMN_URL);
		if (_cursor.isNull(_urlColumn))
			return null;
		return _cursor.getString(_urlColumn);
	}

	public Date getLastModified() {
		if (_lastModifiedColumn == null)
			_lastModifiedColumn = _cursor.getColumnIndexOrThrow(SubscriptionDB.COLUMN_LAST_MODIFIED);
		if (_cursor.isNull(_lastModifiedColumn))
			return null;
		return new Date(_cursor.getLong(_lastModifiedColumn) * 1000);
	}

	public Date getLastUpdate() {
		if (_lastUpdateColumn == null)
			_lastUpdateColumn = _cursor.getColumnIndexOrThrow(SubscriptionDB.COLUMN_LAST_UPDATE);
		if (_cursor.isNull(_lastUpdateColumn))
			return null;
		return new Date(_cursor.getLong(_lastUpdateColumn) * 1000);
	}

	public String getETag() {
		if (_etagColumn == null)
			_etagColumn = _cursor.getColumnIndexOrThrow(SubscriptionDB.COLUMN_ETAG);
		if (_cursor.isNull(_etagColumn))
			return null;
		return _cursor.getString(_etagColumn);
	}

	public String getThumbnail() {
		if (_thumbnailColumn == null)
			_thumbnailColumn = _cursor.getColumnIndexOrThrow(SubscriptionDB.COLUMN_THUMBNAIL);
		if (_cursor.isNull(_thumbnailColumn))
			return null;
		return _cursor.getString(_thumbnailColumn);
	}

	public Integer getExpirationDays() {
		if (_expirationColumn == null)
			_expirationColumn = _cursor.getColumnIndexOrThrow(SubscriptionDB.COLUMN_EXPIRATION);
		if (_cursor.isNull(_expirationColumn))
			return null;
		return _cursor.getInt(_expirationColumn);
	}

	public static String getThumbnailFilename(Context context, long subscriptionId) {
		String storagePath = Storage.getStoragePath(context);
		return storagePath + String.valueOf(subscriptionId) + "podcast.image";
	}

	public static RequestCreator getThumbnailImage(Context context, long subscriptionId) {
		String filename = getThumbnailFilename(context, subscriptionId);
		if (!new File(filename).exists())
			return Picasso.with(context).load(R.drawable.ic_menu_podax).fit();
		return Picasso.with(context).load(new File(filename)).fit();
	}

	public static Bitmap getThumbnailImageRaw(Context context, long subscriptionId) {
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

    public String getDescription() {
        if (_descriptionColumn == null)
            _descriptionColumn = _cursor.getColumnIndexOrThrow(SubscriptionDB.COLUMN_DESCRIPTION);
        if (_cursor.isNull(_descriptionColumn))
            return null;
        return _cursor.getString(_descriptionColumn);
    }

	public boolean isSingleUse() {
		if (_singleUseColumn == null)
			_singleUseColumn = _cursor.getColumnIndexOrThrow(SubscriptionDB.COLUMN_SINGLE_USE);
		if (_cursor.isNull(_singleUseColumn))
			return false;
		return _cursor.getInt(_singleUseColumn) != 0;
	}

	public boolean areNewEpisodesAddedToPlaylist() {
		if (_playlistNewColumn == null)
			_playlistNewColumn = _cursor.getColumnIndexOrThrow(SubscriptionDB.COLUMN_PLAYLIST_NEW);
		if (_cursor.isNull(_playlistNewColumn))
			return true;
		return _cursor.getInt(_playlistNewColumn) != 0;
	}
}
