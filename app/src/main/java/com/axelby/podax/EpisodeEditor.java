package com.axelby.podax;

import android.content.ContentValues;
import android.content.Context;

import java.util.Date;

public class EpisodeEditor {
	private final Context _context;
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

	public static EpisodeEditor fromNew(Context context) {
		return new EpisodeEditor(context, -1);
	}

	public EpisodeEditor(Context context, long episodeId) {
		_context = context;
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

	public void commit() {
		ContentValues values = new ContentValues(16);

		if (_idSet)
			values.put(EpisodeProvider.COLUMN_ID, _id);
		if (_titleSet)
			values.put(EpisodeProvider.COLUMN_TITLE, _title);
		if (_subscriptionIdSet)
			values.put(EpisodeProvider.COLUMN_SUBSCRIPTION_ID, _subscriptionId);
		if (_playlistPositionSet)
			values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, _playlistPosition);
		if (_mediaUrlSet)
			values.put(EpisodeProvider.COLUMN_MEDIA_URL, _mediaUrl);
		if (_fileSizeSet)
			values.put(EpisodeProvider.COLUMN_FILE_SIZE, _fileSize);
		if (_descriptionSet)
			values.put(EpisodeProvider.COLUMN_DESCRIPTION, _description);
		if (_linkSet)
			values.put(EpisodeProvider.COLUMN_LINK, _link);
		if (_lastPositionSet)
			values.put(EpisodeProvider.COLUMN_LAST_POSITION, _lastPosition);
		if (_durationSet)
			values.put(EpisodeProvider.COLUMN_DURATION, _duration);
		if (_pubDateSet)
			values.put(EpisodeProvider.COLUMN_PUB_DATE, _pubDate.getTime() / 1000);
		if (_gpodderUpdateTimestampSet)
			values.put(EpisodeProvider.COLUMN_GPODDER_UPDATE_TIMESTAMP, _gpodderUpdateTimestamp.getTime() / 1000);
		if (_paymentSet)
			values.put(EpisodeProvider.COLUMN_PAYMENT, _payment);
		if (_finishedDateSet)
			values.put(EpisodeProvider.COLUMN_FINISHED_TIME, _finishedDate.getTime() / 1000);

		if (_episodeId != -1)
			_context.getContentResolver().update(EpisodeProvider.getContentUri(_episodeId), values, null, null);
		else
			_context.getContentResolver().insert(EpisodeProvider.URI, values);
	}
}
