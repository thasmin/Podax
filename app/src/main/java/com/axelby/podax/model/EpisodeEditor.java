package com.axelby.podax.model;

import android.content.ContentValues;

import java.util.Date;

public class EpisodeEditor {
	private final long _episodeId;

	private long _id;
	private boolean _idSet;
	private String _title;
	private boolean _titleSet;
	private long _subscriptionId;
	private boolean _subscriptionIdSet;
	private Integer _playlistPosition;
	private boolean _playlistPositionSet;
	private String _mediaUrl;
	private boolean _mediaUrlSet;
	private long _fileSize;
	private boolean _fileSizeSet;
	private String _description;
	private boolean _descriptionSet;
	private String _link;
	private boolean _linkSet;
	private int _lastPosition;
	private boolean _lastPositionSet;
	private int _duration;
	private boolean _durationSet;
	private Date _pubDate;
	private boolean _pubDateSet;
	private Date _gpodderUpdateTimestamp;
	private boolean _gpodderUpdateTimestampSet;
	private String _payment;
	private boolean _paymentSet;
	private Date _finishedDate;
	private boolean _finishedDateSet;

	public static EpisodeEditor fromNew(long subscriptionId, String mediaUrl) {
		return new EpisodeEditor(-1).setSubscriptionId(subscriptionId).setMediaUrl(mediaUrl);
	}

	public EpisodeEditor(long episodeId) {
		_episodeId = episodeId;
	}

	public EpisodeEditor setId(long id) {
		_idSet = true;
		_id = id;
		return this;
	}

	public EpisodeEditor setTitle(String title) {
		_titleSet = true;
		_title = title;
		return this;
	}

	public EpisodeEditor setSubscriptionId(long subscriptionId) {
		_subscriptionIdSet = true;
		_subscriptionId = subscriptionId;
		return this;
	}

	public EpisodeEditor setPlaylistPosition(Integer playlistPosition) {
		_playlistPositionSet = true;
		_playlistPosition = playlistPosition;
		return this;
	}

	public EpisodeEditor setMediaUrl(String mediaUrl) {
		_mediaUrlSet = true;
		_mediaUrl = mediaUrl;
		return this;
	}

	public EpisodeEditor setFileSize(long fileSize) {
		_fileSizeSet = true;
		_fileSize = fileSize;
		return this;
	}

	public EpisodeEditor setDescription(String description) {
		_descriptionSet = true;
		_description = description;
		return this;
	}

	public EpisodeEditor setLink(String link) {
		_linkSet = true;
		_link = link;
		return this;
	}

	public EpisodeEditor setLastPosition(int lastPosition) {
		_lastPositionSet = true;
		_lastPosition = lastPosition;
		return this;
	}

	public EpisodeEditor setDuration(int duration) {
		_durationSet = true;
		_duration = duration;
		return this;
	}

	public EpisodeEditor setPubDate(Date pubDate) {
		_pubDateSet = true;
		_pubDate = pubDate;
		return this;
	}

	public EpisodeEditor setGpodderUpdateTimestamp(Date gpodderUpdateTimestamp) {
		_gpodderUpdateTimestampSet = true;
		_gpodderUpdateTimestamp = gpodderUpdateTimestamp;
		return this;
	}

	public EpisodeEditor setPayment(String payment) {
		_paymentSet = true;
		_payment = payment;
		return this;
	}

	public EpisodeEditor setFinishedDate(Date finishedDate) {
		_finishedDateSet = true;
		_finishedDate = finishedDate;
		return this;
	}

	public long commit() {
		ContentValues values = new ContentValues(16);

		if (_idSet)
			values.put(EpisodeDB.COLUMN_ID, _id);
		if (_titleSet)
			values.put(EpisodeDB.COLUMN_TITLE, _title);
		if (_subscriptionIdSet)
			values.put(EpisodeDB.COLUMN_SUBSCRIPTION_ID, _subscriptionId);
		if (_playlistPositionSet)
			values.put(EpisodeDB.COLUMN_PLAYLIST_POSITION, _playlistPosition);
		if (_mediaUrlSet)
			values.put(EpisodeDB.COLUMN_MEDIA_URL, _mediaUrl);
		if (_fileSizeSet)
			values.put(EpisodeDB.COLUMN_FILE_SIZE, _fileSize);
		if (_descriptionSet)
			values.put(EpisodeDB.COLUMN_DESCRIPTION, _description);
		if (_linkSet)
			values.put(EpisodeDB.COLUMN_LINK, _link);
		if (_lastPositionSet)
			values.put(EpisodeDB.COLUMN_LAST_POSITION, _lastPosition);
		if (_durationSet)
			values.put(EpisodeDB.COLUMN_DURATION, _duration);
		if (_pubDateSet) {
			if (_pubDate != null)
				values.put(EpisodeDB.COLUMN_PUB_DATE, _pubDate.getTime() / 1000);
			else
				values.putNull(EpisodeDB.COLUMN_PUB_DATE);
		}
		if (_gpodderUpdateTimestampSet) {
			if (_gpodderUpdateTimestamp != null)
				values.put(EpisodeDB.COLUMN_GPODDER_UPDATE_TIMESTAMP, _gpodderUpdateTimestamp.getTime() / 1000);
			else
				values.putNull(EpisodeDB.COLUMN_GPODDER_UPDATE_TIMESTAMP);
		}
		if (_paymentSet)
			values.put(EpisodeDB.COLUMN_PAYMENT, _payment);
		if (_finishedDateSet) {
			if (_finishedDate != null)
				values.put(EpisodeDB.COLUMN_FINISHED_TIME, _finishedDate.getTime() / 1000);
			else
				values.putNull(EpisodeDB.COLUMN_FINISHED_TIME);
		}

		if (_episodeId != -1) {
			PodaxDB.episodes.update(_episodeId, values);
			return _episodeId;
		} else {
			return PodaxDB.episodes.insert(values);
		}
	}
}
