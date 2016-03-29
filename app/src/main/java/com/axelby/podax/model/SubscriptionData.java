package com.axelby.podax.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.LruCache;
import android.widget.CompoundButton;

import com.axelby.podax.R;
import com.axelby.podax.Storage;
import com.axelby.podax.UpdateService;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.Date;
import java.util.List;

import rx.schedulers.Schedulers;

public class SubscriptionData {

	private final static LruCache<Long, SoftReference<SubscriptionData>> _cache = new LruCache<>(50);

	static {
		PodaxDB.subscriptions.watchAll()
			.subscribeOn(Schedulers.io())
			.subscribe(
				sub -> {
					synchronized (_cache) {
						SoftReference<SubscriptionData> reference = _cache.get(sub.getId());
						if (reference != null && reference.get() != null)
							_cache.put(sub.getId(), new SoftReference<>(sub));
					}
				},
				e -> Log.e("SubscriptionData", "unable to watch subscriptions for changes", e)
			);
	}


	private static Context _context;

	public static void setContext(Context context) {
		_context = context;
	}

	private final long _id;
	private final String _rawTitle;
	private final String _url;
	private final Date _lastModified;
	private final Date _lastUpdate;
	private final String _etag;
	private final String _thumbnail;
	private final String _titleOverride;
	private final String _description;
	private final boolean _singleUse;
	private final boolean _playlistNew;
	private final Integer _expirationDays;

	SubscriptionData(Cursor cursor) {
		_id = cursor.getLong(cursor.getColumnIndex(SubscriptionDB.COLUMN_ID));
		_rawTitle = cursor.getString(cursor.getColumnIndex(SubscriptionDB.COLUMN_TITLE));
		_url = cursor.getString(cursor.getColumnIndex(SubscriptionDB.COLUMN_URL));

		_lastModified = new Date(cursor.getLong(cursor.getColumnIndex(SubscriptionDB.COLUMN_LAST_MODIFIED)) * 1000);
		_lastUpdate = new Date(cursor.getLong(cursor.getColumnIndex(SubscriptionDB.COLUMN_LAST_UPDATE)) * 1000);

		_etag = cursor.getString(cursor.getColumnIndex(SubscriptionDB.COLUMN_ETAG));
		_thumbnail = cursor.getString(cursor.getColumnIndex(SubscriptionDB.COLUMN_THUMBNAIL));
		_titleOverride = cursor.getString(cursor.getColumnIndex(SubscriptionDB.COLUMN_TITLE_OVERRIDE));
		_description = cursor.getString(cursor.getColumnIndex(SubscriptionDB.COLUMN_DESCRIPTION));
		_singleUse = cursor.getInt(cursor.getColumnIndex(SubscriptionDB.COLUMN_SINGLE_USE)) == 1;
		_playlistNew = cursor.getInt(cursor.getColumnIndex(SubscriptionDB.COLUMN_PLAYLIST_NEW)) == 1;
		_expirationDays = cursor.getInt(cursor.getColumnIndex(SubscriptionDB.COLUMN_EXPIRATION));
	}

	private SubscriptionData(ContentValues values) {
		_id = values.getAsLong(SubscriptionDB.COLUMN_ID);
		_url = values.getAsString(SubscriptionDB.COLUMN_URL);

		if (values.containsKey(SubscriptionDB.COLUMN_TITLE))
			_rawTitle = values.getAsString(SubscriptionDB.COLUMN_TITLE);
		else
			_rawTitle = null;

		if (values.containsKey(SubscriptionDB.COLUMN_LAST_MODIFIED)) {
			long lastModifiedTimestamp = values.getAsLong(SubscriptionDB.COLUMN_LAST_MODIFIED);
			_lastModified = new Date(lastModifiedTimestamp * 1000);
		} else
			_lastModified = null;

		if (values.containsKey(SubscriptionDB.COLUMN_LAST_UPDATE)) {
			long lastUpdateTimestamp = values.getAsLong(SubscriptionDB.COLUMN_LAST_UPDATE);
			_lastUpdate = new Date(lastUpdateTimestamp * 1000);
		} else
			_lastUpdate = null;

		if (values.containsKey(SubscriptionDB.COLUMN_ETAG))
			_etag = values.getAsString(SubscriptionDB.COLUMN_ETAG);
		else
			_etag = null;

		if (values.containsKey(SubscriptionDB.COLUMN_THUMBNAIL))
			_thumbnail = values.getAsString(SubscriptionDB.COLUMN_THUMBNAIL);
		else
		_thumbnail = null;

		if (values.containsKey(SubscriptionDB.COLUMN_TITLE_OVERRIDE))
			_titleOverride = values.getAsString(SubscriptionDB.COLUMN_TITLE_OVERRIDE);
		else
		_titleOverride = null;

		if (values.containsKey(SubscriptionDB.COLUMN_DESCRIPTION))
			_description = values.getAsString(SubscriptionDB.COLUMN_DESCRIPTION);
		else
		_description = null;

		if (values.containsKey(SubscriptionDB.COLUMN_SINGLE_USE))
			_singleUse = values.getAsBoolean(SubscriptionDB.COLUMN_SINGLE_USE);
		else
			_singleUse = false;

		if (values.containsKey(SubscriptionDB.COLUMN_PLAYLIST_NEW))
			_playlistNew = values.getAsBoolean(SubscriptionDB.COLUMN_PLAYLIST_NEW);
		else
			_playlistNew = true;

		if (values.containsKey(SubscriptionDB.COLUMN_EXPIRATION))
			_expirationDays = values.getAsInteger(SubscriptionDB.COLUMN_EXPIRATION);
		else
			_expirationDays = null;
	}

	static SubscriptionData from(Cursor cursor) {
		long id = cursor.getLong(cursor.getColumnIndex("_id"));

		synchronized (_cache) {
			if (_cache.get(id) != null && _cache.get(id).get() != null)
				return _cache.get(id).get();
		}

		SubscriptionData data = new SubscriptionData(cursor);
		synchronized (_cache) {
			_cache.put(id, new SoftReference<>(data));
		}
		return data;
	}

	public static SubscriptionData from(ContentValues values) {
		return new SubscriptionData(values);
	}

	public static SubscriptionData create(long id) {
		synchronized (_cache) {
			if (_cache.get(id) != null && _cache.get(id).get() != null)
				return _cache.get(id).get();
		}

		if (id < 0)
			return null;

		SubscriptionData data = PodaxDB.subscriptions.get(id);
		synchronized (_cache) {
			_cache.put(id, new SoftReference<>(data));
		}
		return data;
	}

	/* -----
	   cache
	   ----- */

	public static void evictCache() {
		_cache.evictAll();
	}

	public static void evictFromCache(long subscriptionId) {
		_cache.remove(subscriptionId);
	}

	public static SubscriptionData cacheSwap(SubscriptionData data) {
		synchronized (_cache) {
			_cache.put(data.getId(), new SoftReference<>(data));
		}
		return data;
	}

	public long getId() { return _id; }
	public String getRawTitle() { return _rawTitle; }
	public String getUrl() { return _url; }
	public Date getLastModified() { return _lastModified; }
	public Date getLastUpdate() { return _lastUpdate; }
	public String getETag() { return _etag; }
	public String getThumbnail() { return _thumbnail; }
	public String getTitleOverride() { return _titleOverride; }
	public String getDescription() { return _description; }
	public boolean isSingleUse() { return _singleUse; }
	public boolean isSubscribed() { return !_singleUse; }
	public boolean areNewEpisodesAddedToPlaylist() { return _playlistNew; }
	public Integer getExpirationDays() { return _expirationDays; }
	public String getTitle() {
		if (_titleOverride != null && _titleOverride.length() > 0)
			return _titleOverride;
		return _rawTitle;
	}

	/* ---------
	   thumbnail
	   --------- */

	public static String getThumbnailFilename(long subscriptionId) {
		String storagePath = Storage.getStoragePath(_context);
		return storagePath + String.valueOf(subscriptionId) + "podcast.image";
	}

	public String getThumbnailFilename() {
		String storagePath = Storage.getStoragePath(_context);
		return storagePath + String.valueOf(getId()) + "podcast.image";
	}

	public RequestCreator getThumbnailImage() {
		String filename = getThumbnailFilename(getId());
		if (!new File(filename).exists())
			return Picasso.with(_context).load(R.drawable.ic_menu_podax).fit();
		return Picasso.with(_context).load(new File(filename)).fit();
	}

	public static RequestCreator getThumbnailImage(long subscriptionId) {
		String filename = getThumbnailFilename(subscriptionId);
		if (!new File(filename).exists())
			return Picasso.with(_context).load(R.drawable.ic_menu_podax).fit();
		return Picasso.with(_context).load(new File(filename)).fit();
	}

	public static Bitmap getThumbnailImageRaw(long subscriptionId) {
		String filename = getThumbnailFilename(subscriptionId);
		if (!new File(filename).exists())
			return null;

		return BitmapFactory.decodeFile(filename);
	}

	public static void evictThumbnails(long subscriptionId) {
		File thumbnail = new File(getThumbnailFilename(subscriptionId));
		if (!thumbnail.exists())
			return;
		thumbnail.delete();
	}

	/* -------
	   actions
	   ------- */

	public boolean isCurrentlyUpdating() {
		return UpdateService.getUpdatingObservable().toBlocking().firstOrDefault(-1000L) == getId();
	}

	public List<EpisodeData> getEpisodes() {
		return Episodes.getForSubscriptionId(getId()).toBlocking().first();
	}

	@SuppressWarnings("unused")
	public void changeSubscribe(CompoundButton button, boolean isChecked) {
		new SubscriptionEditor(getId()).setSingleUse(!isChecked).commit();
	}

	@SuppressWarnings("unused")
	public void changeAddNewToPlaylist(CompoundButton button, boolean isChecked) {
		new SubscriptionEditor(getId()).setPlaylistNew(isChecked).commit();
	}
}
