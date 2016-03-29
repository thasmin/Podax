package com.axelby.podax.model;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.LruCache;
import android.view.View;

import com.axelby.podax.AppFlow;
import com.axelby.podax.EpisodeCursor;
import com.axelby.podax.EpisodeDownloadService;
import com.axelby.podax.EpisodeProvider;
import com.axelby.podax.FlattrHelper;
import com.axelby.podax.Helper;
import com.axelby.podax.PlayerService;
import com.axelby.podax.PlayerStatus;
import com.axelby.podax.R;
import com.squareup.picasso.RequestCreator;

import java.io.File;
import java.lang.ref.SoftReference;
import java.text.DateFormat;
import java.util.Date;

public class EpisodeData {

	private final static LruCache<Long, SoftReference<EpisodeData>> _cache = new LruCache<>(200);

	// TODO: change Date to LocalDateTime and urls to URI
	private final long _id;
	private final String _title;
	private final long _subscriptionId;
	private final String _subscriptionTitle;
	private final String _subscriptionUrl;
	private final Integer _playlistPosition;
	private final String _mediaUrl;
	private final Long _fileSize;
	private final String _description;
	private final String _link;
	private final int _lastPosition;
	private final int _duration;
	private final Date _pubDate;
	private final Date _gpodderUpdateTimestamp;
	private final String _payment;
	private final Date _finishedDate;

	private String _filename = null;

	private EpisodeData(EpisodeCursor ep) {
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

	public static EpisodeData from(EpisodeCursor ep) {
		long episodeId = ep.getId();
		synchronized (_cache) {
			if (_cache.get(episodeId) != null && _cache.get(episodeId).get() != null)
				return _cache.get(episodeId).get();
		}

		EpisodeData episodeData = new EpisodeData(ep);
		synchronized (_cache) {
			_cache.put(episodeId, new SoftReference<>(episodeData));
		}
		return episodeData;
	}

	public static EpisodeData create(Context context, long episodeId) {
		synchronized (_cache) {
			if (_cache.get(episodeId) != null && _cache.get(episodeId).get() != null)
				return _cache.get(episodeId).get();
		}

		if (episodeId < 0)
			return null;

		EpisodeCursor ep = EpisodeCursor.getCursor(context, episodeId);
		if (ep == null)
			return null;

		EpisodeData d = EpisodeData.from(ep);
		ep.closeCursor();
		return d;
	}

	public static EpisodeData getActive(Context context) {
		long activeEpisodeId = EpisodeProvider.getActiveEpisodeId(context);
		if (activeEpisodeId == -1)
			return null;
		return EpisodeData.create(context, activeEpisodeId);
	}

	public static void evictCache() {
		_cache.evictAll();
	}

	public static void evictFromCache(long episodeId) {
		_cache.remove(episodeId);
	}

	public static EpisodeData cacheSwap(EpisodeCursor c) {
		EpisodeData data = new EpisodeData(c);
		synchronized (_cache) {
			_cache.put(c.getId(), new SoftReference<>(data));
		}
		return data;
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

	public Long getFileSize() {
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

	public String getPaymentUrl() {
		return _payment;
	}

	public Date getFinishedDate() {
		return _finishedDate;
	}

	public String getFilename(Context context) {
		if (_filename == null)
			_filename = String.format("%s%s.%s",
				EpisodeCursor.getPodcastStoragePath(context),
				String.valueOf(getId()),
				EpisodeCursor.getExtension(getMediaUrl())
			);

		return _filename;
	}

	public boolean isDownloaded(Context context) {
		if (getFileSize() == null)
			return false;
		File file = new File(getFilename(context));
		return file.exists() && file.length() == getFileSize() && getFileSize() != 0;
	}

	private Uri getContentUri() {
		return ContentUris.withAppendedId(EpisodeProvider.URI, getId());
	}

	public String getFinishedDate(Context context) {
		return context.getString(R.string.finished_on, DateFormat.getInstance().format(getFinishedDate()));
	}

	public String getSubscriptionImageFilename() {
		return SubscriptionData.getThumbnailFilename(getSubscriptionId());
	}

	public RequestCreator getSubscriptionImage() {
		return SubscriptionData.getThumbnailImage(getSubscriptionId());
	}

	/* -------
	   actions
	   ------- */

	public void removeFromPlaylist(Context context) {
		ContentValues values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, (Integer) null);
		context.getContentResolver().update(getContentUri(), values, null, null);
	}

	public void addToPlaylist(Context context) {
		ContentValues values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, Integer.MAX_VALUE);
		context.getContentResolver().update(getContentUri(), values, null, null);
	}

	public void moveToPlaylistPosition(Context context, int newPosition) {
		ContentValues values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, newPosition);
		context.getContentResolver().update(getContentUri(), values, null, null);
	}

	public void restart(View view) {
		EpisodeProvider.restart(view.getContext(), getId());
	}
	public void rewind(View view) {
		EpisodeProvider.movePositionBy(view.getContext(), getId(), -15);
	}
	public void playstop(View view) {
		PlayerStatus status = PlayerStatus.getCurrentState(view.getContext());
		if (status.hasActiveEpisode() && status.getEpisodeId() == getId() && status.isPlaying())
			PlayerService.stop(view.getContext());
		else
			PlayerService.play(view.getContext(), getId());
	}
	public void forward(View view) {
		EpisodeProvider.movePositionBy(view.getContext(), getId(), 30);
	}
	public void skipToEnd(View view) {
		EpisodeProvider.skipToEnd(view.getContext(), getId());
	}

	public void viewDescription(View view) {
		if (getLink() != null)
			view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getLink())));
	}

	/* ------------
	   data binding
	   ------------ */

	public void show(View view) {
		View thumbnail = view.findViewById(R.id.thumbnail);
		View title = view.findViewById(R.id.title);
		AppFlow.get(view.getContext()).displayEpisode(_id, thumbnail, title);
	}

	public void play(View view) {
		PlayerService.play(view.getContext(), _id);
	}

	public void playAndShow(View view) {
		play(view);
		show(view);
	}

	public void removeFromPlaylist(View view) {
		removeFromPlaylist(view.getContext());
	}

	public void togglePlaylist(View view) {
		if (getPlaylistPosition() == null)
			addToPlaylist(view.getContext());
		else
			removeFromPlaylist(view.getContext());
	}

	public boolean hasDuration() {
		return getDuration() != 0;
	}

	public String getDurationAsWords(Context context) {
		return Helper.getVerboseTimeString(context, getDuration() / 1000.0f, false) + " long";
	}

	public String getTimeRemaining() {
		if (!hasDuration())
			return "";
		return "-" + Helper.getTimeString(getDuration() - getLastPosition());
	}

	public String getDownloadStatus(Context context) {
		String episodeFilename = getFilename(context);
		float downloaded = new File(episodeFilename).length();
		if (getFileSize() == downloaded)
			return context.getString(R.string.downloaded);
		else if (EpisodeDownloadService.isDownloading(episodeFilename))
			return context.getString(R.string.now_downloading);
		else
			return context.getString(R.string.not_downloaded);
	}

	public int getDownloadStatusColor(Context context) {
		String filename = getFilename(context);
		float downloaded = new File(filename).length();
		if (getFileSize() == downloaded || EpisodeDownloadService.isDownloading(filename))
			return android.R.color.holo_green_dark;
		else
			return android.R.color.holo_red_dark;
	}

	public String getReleaseDate(Context context) {
		return context.getString(R.string.released_on) + " " + DateFormat.getInstance().format(getPubDate());
	}

	public boolean hasFlattrPaymentUrl() {
		return getPaymentUrl() != null && FlattrHelper.isFlattrUri(Uri.parse(getPaymentUrl()));
	}

	public static long parseId(Context context, Uri uri) {
		if (uri.equals(EpisodeProvider.ACTIVE_EPISODE_URI))
			return EpisodeProvider.getActiveEpisodeId(context);
		return ContentUris.parseId(uri);
	}
}
