package com.axelby.podax.model;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.graphics.Palette;
import android.text.Html;
import android.util.LruCache;
import android.widget.CompoundButton;

import com.axelby.podax.PodaxApplication;
import com.axelby.podax.R;
import com.axelby.podax.Storage;
import com.axelby.podax.UpdateService;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.Date;
import java.util.List;

public class SubscriptionData {

	private final static LruCache<Long, SoftReference<SubscriptionData>> _cache = new LruCache<>(50);

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
	private final int _dominantColor;

	private SubscriptionData(Cursor cursor) {
		_id = cursor.getLong(cursor.getColumnIndex(SubscriptionDB.COLUMN_ID));
		_rawTitle = cursor.getString(cursor.getColumnIndex(SubscriptionDB.COLUMN_TITLE));
		_url = cursor.getString(cursor.getColumnIndex(SubscriptionDB.COLUMN_URL));

		int lastModifiedIndex = cursor.getColumnIndex(SubscriptionDB.COLUMN_LAST_MODIFIED);
		_lastModified = cursor.isNull(lastModifiedIndex) ? null : new Date(cursor.getLong(lastModifiedIndex) * 1000);
		int lastUpdateIndex = cursor.getColumnIndex(SubscriptionDB.COLUMN_LAST_UPDATE);
		_lastUpdate = cursor.isNull(lastUpdateIndex) ? null : new Date(cursor.getLong(lastUpdateIndex) * 1000);

		_etag = cursor.getString(cursor.getColumnIndex(SubscriptionDB.COLUMN_ETAG));
		_thumbnail = cursor.getString(cursor.getColumnIndex(SubscriptionDB.COLUMN_THUMBNAIL));
		_titleOverride = cursor.getString(cursor.getColumnIndex(SubscriptionDB.COLUMN_TITLE_OVERRIDE));
		_description = cursor.getString(cursor.getColumnIndex(SubscriptionDB.COLUMN_DESCRIPTION));
		_singleUse = cursor.getInt(cursor.getColumnIndex(SubscriptionDB.COLUMN_SINGLE_USE)) == 1;
		_playlistNew = cursor.getInt(cursor.getColumnIndex(SubscriptionDB.COLUMN_PLAYLIST_NEW)) == 1;

		int expirationIndex = cursor.getColumnIndex(SubscriptionDB.COLUMN_EXPIRATION);
		_expirationDays = cursor.isNull(expirationIndex) ? null : cursor.getInt(expirationIndex);

		_dominantColor = cursor.getInt(cursor.getColumnIndex(SubscriptionDB.COLUMN_DOMINANT_COLOR));
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

	public long getId() { return _id; }
	public String getRawTitle() { return _rawTitle; }
	public String getUrl() { return _url; }
	public Date getLastModified() { return _lastModified; }
	public Date getLastUpdate() { return _lastUpdate; }
	public String getETag() { return _etag; }
	public String getThumbnail() { return _thumbnail; }
	public String getTitleOverride() { return _titleOverride; }
	public String getDescription() { return _description; }
	public boolean isSubscribed() { return !_singleUse; }
	public boolean areNewEpisodesAddedToPlaylist() { return _playlistNew; }
	public Integer getExpirationDays() { return _expirationDays; }
	public int getDominantColor() { return _dominantColor; }
	public String getTitle() {
		if (_titleOverride != null && _titleOverride.length() > 0)
			return Html.fromHtml(_titleOverride).toString();
		if (_rawTitle != null)
			return Html.fromHtml(_rawTitle).toString();
		return "";
	}

	/* ---------
	   thumbnail
	   --------- */

	public static String getThumbnailFilename(long subscriptionId) {
		String storagePath = Storage.getStoragePath(PodaxApplication.get());
		return storagePath + String.valueOf(subscriptionId) + "podcast.image";
	}

	public String getThumbnailFilename() {
		String storagePath = Storage.getStoragePath(PodaxApplication.get());
		return storagePath + String.valueOf(getId()) + "podcast.image";
	}

	public RequestCreator getThumbnailImage() {
		String filename = getThumbnailFilename(getId());
		if (!new File(filename).exists())
			return Picasso.with(PodaxApplication.get()).load(R.drawable.ic_menu_podax).fit();
		return Picasso.with(PodaxApplication.get()).load(new File(filename)).fit();
	}

	public static RequestCreator getThumbnailImage(long subscriptionId) {
		String filename = getThumbnailFilename(subscriptionId);
		if (!new File(filename).exists())
			return Picasso.with(PodaxApplication.get()).load(R.drawable.ic_menu_podax).fit();
		return Picasso.with(PodaxApplication.get()).load(new File(filename)).fit();
	}

	public static Bitmap getThumbnailImageRaw(long subscriptionId) {
		String filename = getThumbnailFilename(subscriptionId);
		if (!new File(filename).exists())
			return null;

		return BitmapFactory.decodeFile(filename);
	}

	public static Palette.Swatch getThumbnailSwatch(long subscriptionId) {
		SubscriptionData subscription = SubscriptionData.create(subscriptionId);
		if (subscription == null)
			return null;
		return subscription.getThumbnailSwatch();
	}

	public static int findDominantColor(long subscriptionId) {
		Bitmap thumbnail = SubscriptionData.getThumbnailImageRaw(subscriptionId);
		if (thumbnail == null)
			return -1;
		Palette palette = Palette.from(thumbnail).generate();
		int dominantColor = palette.getDominantColor(-1);
		new SubscriptionEditor(subscriptionId).setDominantColor(dominantColor).commit();
		return dominantColor;
	}

	public Palette.Swatch getThumbnailSwatch() {
		if (getDominantColor() == -1)
			findDominantColor(getId());
		if (getDominantColor() == -1)
			return null;
		return new Palette.Swatch(getDominantColor(), 1);
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
		return UpdateService.areYouUpdating(getId());
	}

	public List<EpisodeData> getEpisodes() {
		return PodaxDB.episodes.getForSubscriptionId(getId());
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
