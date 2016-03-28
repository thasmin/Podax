package com.axelby.podax.model;

import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.NonNull;

import com.axelby.podax.UpdateService;

import java.util.Date;

public class SubscriptionEditor {
	private long _subscriptionId;

	private boolean _rawTitleSet = false;
	private String _rawTitle;
	private boolean _urlSet = false;
	private String _url;
	private boolean _lastModifiedSet = false;
	private Date _lastModified;
	private boolean _lastUpdateSet = false;
	private Date _lastUpdate;
	private boolean _etagSet = false;
	private String _etag;
	private boolean _thumbnailSet = false;
	private String _thumbnail;
	private boolean _titleOverrideSet = false;
	private String _titleOverride;
	private boolean _descriptionSet = false;
	private String _description;
	private boolean _singleUseSet = false;
	private boolean _singleUse;
	private boolean _playlistNewSet = false;
	private boolean _playlistNew;
	private boolean _expirationDaysSet = false;
	private Integer _expirationDays;

	private boolean _fromGPodder = false;

	public static SubscriptionEditor create(String url) {
		return new SubscriptionEditor(-1).setUrl(url).setSingleUse(false);
	}

	public static SubscriptionEditor createViaGPodder(String url) {
		return new SubscriptionEditor(-1).setFromGPodder(true).setUrl(url).setSingleUse(false);
	}

	public static long addNewSubscription(Context context, String url) {
		long id = SubscriptionEditor.create(url).setSingleUse(false).commit();
		UpdateService.updateSubscription(context, id);
		return id;
	}

	public static long addSingleUseSubscription(Context context, String url) {
		long id = SubscriptionEditor.create(url)
			.setSingleUse(true)
			.setPlaylistNew(false)
			.commit();
		UpdateService.updateSubscription(context, id);
		return id;
	}

	public SubscriptionEditor(long subscriptionId) {
		_subscriptionId = subscriptionId;
	}

	public SubscriptionEditor setRawTitle(String rawTitle) {
		_rawTitleSet = true;
		_rawTitle = rawTitle;
		return this;
	}

	public SubscriptionEditor setUrl(String url) {
		_urlSet = true;
		_url = url;
		return this;
	}

	public SubscriptionEditor setLastModified(Date lastModified) {
		_lastModifiedSet = true;
		_lastModified = lastModified;
		return this;
	}

	public SubscriptionEditor setLastUpdate(Date lastUpdate) {
		_lastUpdateSet = true;
		_lastUpdate = lastUpdate;
		return this;
	}

	public SubscriptionEditor setEtag(String etag) {
		_etagSet = true;
		_etag = etag;
		return this;
	}

	public SubscriptionEditor setThumbnail(String thumbnail) {
		_thumbnailSet = true;
		_thumbnail = thumbnail;
		return this;
	}

	public SubscriptionEditor setTitleOverride(String title) {
		_titleOverrideSet = true;
		_titleOverride = title;
		return this;
	}

	public SubscriptionEditor setDescription(String description) {
		_descriptionSet = true;
		_description = description;
		return this;
	}

	public SubscriptionEditor setSingleUse(boolean singleUse) {
		_singleUseSet = true;
		_singleUse = singleUse;
		return this;
	}

	public SubscriptionEditor setPlaylistNew(boolean playlistNew) {
		_playlistNewSet = true;
		_playlistNew = playlistNew;
		return this;
	}

	public SubscriptionEditor setExpirationDays(Integer expirationDays) {
		_expirationDaysSet = true;
		_expirationDays = expirationDays;
		return this;
	}

	public SubscriptionEditor setFromGPodder(boolean fromGPodder) {
		_fromGPodder = fromGPodder;
		return this;
	}

	public long commit() {
		ContentValues values = getContentValues();

		if (_subscriptionId != -1) {
			PodaxDB.subscriptions.update(_subscriptionId, values);
			SubscriptionData.evictFromCache(_subscriptionId);
		} else {
			values.remove(Subscriptions.COLUMN_ID);
			_subscriptionId = PodaxDB.subscriptions.insert(values);
			if (!_fromGPodder)
				PodaxDB.gPodder.add(_url);
		}

		Subscriptions.notifyChange(SubscriptionData.from(getContentValues()));
		return _subscriptionId;
	}

	@NonNull
	private ContentValues getContentValues() {
		ContentValues values = new ContentValues(17);

		values.put(Subscriptions.COLUMN_ID, _subscriptionId);
		if (_rawTitleSet)
			values.put(Subscriptions.COLUMN_TITLE, _rawTitle);
		if (_urlSet)
			values.put(Subscriptions.COLUMN_URL, _url);
		if (_lastModifiedSet) {
			if (_lastModified != null)
				values.put(Subscriptions.COLUMN_LAST_MODIFIED, _lastModified.getTime() / 1000);
			else
				values.putNull(Subscriptions.COLUMN_LAST_MODIFIED);
		}
		if (_lastUpdateSet) {
			if (_lastUpdate != null)
				values.put(Subscriptions.COLUMN_LAST_UPDATE, _lastUpdate.getTime() / 1000);
			else
				values.putNull(Subscriptions.COLUMN_LAST_UPDATE);
		}
		if (_etagSet)
			values.put(Subscriptions.COLUMN_ETAG, _etag);
		if (_thumbnailSet)
			values.put(Subscriptions.COLUMN_THUMBNAIL, _thumbnail);
		if (_titleOverrideSet)
			values.put(Subscriptions.COLUMN_TITLE_OVERRIDE, _titleOverride);
		if (_descriptionSet)
			values.put(Subscriptions.COLUMN_DESCRIPTION, _description);
		if (_singleUseSet)
			values.put(Subscriptions.COLUMN_SINGLE_USE, _singleUse);
		if (_playlistNewSet)
			values.put(Subscriptions.COLUMN_PLAYLIST_NEW, _playlistNew);
		if (_expirationDaysSet)
			values.put(Subscriptions.COLUMN_EXPIRATION, _expirationDays);
		return values;
	}
}
