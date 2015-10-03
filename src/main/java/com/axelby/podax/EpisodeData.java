package com.axelby.podax;

import android.content.Context;

import java.util.Date;

public class EpisodeData {

	// TODO: change Date to LocalDateTime
	private final long _id;
	private final String _title;
	private final long _subscriptionId;
	private final String _subscriptionTitle;
	private final String _subscriptionUrl;
	private final Integer _playlistPosition;
	private final String _mediaUrl;
	private final Integer _fileSize;
	private final String _description;
	private final String _link;
	private final int _lastPosition;
	private final int _duration;
	private final Date _pubDate;
	private final Date _gpodderUpdateTimestamp;
	private final String _payment;
	private final Date _finishedDate;

	public EpisodeData(EpisodeCursor ep) {
		_id = ep.getId();
		_title = ep.getTitle();
		_subscriptionId = ep.getSubscriptionId();
		_subscriptionTitle = ep.getSubscriptionTitle();
		_subscriptionUrl = ep.getSubscriptionUrl();
		_playlistPosition = ep.getPlaylistPosition();
		_mediaUrl = ep.getMediaUrl();
		_fileSize = ep.getFileSize();
		_description = ep.getDescription();
		_link = ep.getLink();
		_lastPosition = ep.getLastPosition();
		_duration = ep.getDuration();
		_pubDate = ep.getPubDate();
		_gpodderUpdateTimestamp = ep.getGPodderUpdateTimestamp();
		_payment = ep.getPaymentUrl();
		_finishedDate = ep.getFinishedDate();
	}

	public static EpisodeData create(Context context, long episodeId) {
		if (episodeId < 0)
			return null;

		EpisodeCursor ep = EpisodeCursor.getCursor(context, episodeId);
		if (ep == null)
			return null;

		EpisodeData d = new EpisodeData(ep);
		ep.closeCursor();
		return d;
	}

	public long getId() {
		return _id;
	}

	public String getTitle() {
		return _title;
	}

	public long getSubscriptionId() {
		return _subscriptionId;
	}

	public String getSubscriptionTitle() {
		return _subscriptionTitle;
	}

	public String getSubscriptionUrl() {
		return _subscriptionUrl;
	}

	public Integer getPlaylistPosition() {
		return _playlistPosition;
	}

	public String getMediaUrl() {
		return _mediaUrl;
	}

	public Integer getFileSize() {
		return _fileSize;
	}

	public String getDescription() {
		return _description;
	}

	public String getLink() {
		return _link;
	}

	public int getLastPosition() {
		return _lastPosition;
	}

	public int getDuration() {
		return _duration;
	}

	public Date getPubDate() {
		return _pubDate;
	}

	public Date getGpodderUpdateTimestamp() {
		return _gpodderUpdateTimestamp;
	}

	public String getPayment() {
		return _payment;
	}

	public Date getFinishedDate() {
		return _finishedDate;
	}
}
