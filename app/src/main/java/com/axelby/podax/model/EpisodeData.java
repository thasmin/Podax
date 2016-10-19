package com.axelby.podax.model;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.util.LruCache;
import android.view.View;

import com.axelby.podax.AppFlow;
import com.axelby.podax.EpisodeDownloadService;
import com.axelby.podax.FlattrHelper;
import com.axelby.podax.Helper;
import com.axelby.podax.PlayerService;
import com.axelby.podax.PlayerStatus;
import com.axelby.podax.R;
import com.axelby.podax.Storage;
import com.axelby.podax.player.AudioPlayerBase;
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

	private EpisodeData(Cursor c) {
		_id = c.getLong(c.getColumnIndex(EpisodeDB.COLUMN_ID));
		_title = c.getString(c.getColumnIndex(EpisodeDB.COLUMN_TITLE));
		_subscriptionId = c.getLong(c.getColumnIndex(EpisodeDB.COLUMN_SUBSCRIPTION_ID));
		_subscriptionTitle = c.getString(c.getColumnIndex(EpisodeDB.COLUMN_SUBSCRIPTION_TITLE));
		_subscriptionUrl = c.getString(c.getColumnIndex(EpisodeDB.COLUMN_SUBSCRIPTION_URL));
		_playlistPosition = c.isNull(c.getColumnIndex(EpisodeDB.COLUMN_PLAYLIST_POSITION)) ? null :
			c.getInt(c.getColumnIndex(EpisodeDB.COLUMN_PLAYLIST_POSITION));
		_mediaUrl = c.getString(c.getColumnIndex(EpisodeDB.COLUMN_MEDIA_URL));
		_fileSize = c.getLong(c.getColumnIndex(EpisodeDB.COLUMN_FILE_SIZE));
		_description = c.getString(c.getColumnIndex(EpisodeDB.COLUMN_DESCRIPTION));
		_link = c.getString(c.getColumnIndex(EpisodeDB.COLUMN_LINK));
		_lastPosition = c.getInt(c.getColumnIndex(EpisodeDB.COLUMN_LAST_POSITION));
		_duration = c.getInt(c.getColumnIndex(EpisodeDB.COLUMN_DURATION));
		_pubDate = new Date(c.getLong(c.getColumnIndex(EpisodeDB.COLUMN_PUB_DATE)) * 1000);
		_gpodderUpdateTimestamp = new Date(c.getLong(c.getColumnIndex(EpisodeDB.COLUMN_GPODDER_UPDATE_TIMESTAMP)) * 1000);
		_payment = c.getString(c.getColumnIndex(EpisodeDB.COLUMN_PAYMENT));
		_finishedDate = new Date(c.getLong(c.getColumnIndex(EpisodeDB.COLUMN_FINISHED_TIME)) * 1000);
	}

	public static EpisodeData from(Cursor cursor) {
		long episodeId = cursor.getLong(cursor.getColumnIndex("_id"));
		synchronized (_cache) {
			if (_cache.get(episodeId) != null && _cache.get(episodeId).get() != null)
				return _cache.get(episodeId).get();
		}

		EpisodeData episodeData = new EpisodeData(cursor);
		synchronized (_cache) {
			_cache.put(episodeId, new SoftReference<>(episodeData));
		}
		return episodeData;
	}

	public static EpisodeData create(long episodeId) {
		synchronized (_cache) {
			if (_cache.get(episodeId) != null && _cache.get(episodeId).get() != null)
				return _cache.get(episodeId).get();
		}

		if (episodeId < 0)
			return null;

		return PodaxDB.episodes.get(episodeId);
	}

	public static void evictCache() {
		_cache.evictAll();
	}

	static void evictFromCache(long episodeId) {
		_cache.remove(episodeId);
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

	public Date getGPodderUpdateTimestamp() {
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
				Storage.getPodcastStoragePath(context),
				String.valueOf(getId()),
				Storage.getExtension(getMediaUrl())
			);

		return _filename;
	}

	public boolean isDownloaded(Context context) {
		if (getFileSize() == null)
			return false;
		File file = new File(getFilename(context));
		return file.exists() && file.length() == getFileSize() && getFileSize() != 0;
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

	public void removeFromPlaylist() {
		new EpisodeEditor(getId()).setPlaylistPosition(null).commit();
	}

	public void addToPlaylist() {
		new EpisodeEditor(getId()).setPlaylistPosition(Integer.MAX_VALUE).commit();
	}

	public void moveToPlaylistPosition(int newPosition) {
		new EpisodeEditor(getId()).setPlaylistPosition(newPosition).commit();
	}

	public int determineDuration(Context context) {
		int duration = (int) (AudioPlayerBase.determineDuration(getFilename(context)) * 1000);
		new EpisodeEditor(getId()).setDuration(duration).commit();
		return duration;
	}

	public void restart(View view) {
		EpisodeDB.restart(getId());
	}
	public void rewind(View view) {
		EpisodeDB.movePositionBy(getId(), -15);
	}
	public void playstop(View view) {
		PlayerStatus status = PlayerStatus.getCurrentState(view.getContext());
		if (status.hasActiveEpisode() && status.getEpisodeId() == getId() && status.isPlaying())
			PlayerService.stop(view.getContext());
		else
			PlayerService.play(view.getContext(), getId());
	}
	public void forward(View view) {
		EpisodeDB.movePositionBy(getId(), 30);
	}
	public void skipToEnd(View view) {
		EpisodeDB.skipToEnd(getId());
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
		AppFlow.get(Helper.getActivityFromView(view)).displayEpisode(_id, thumbnail, title);
	}

	public void play(View view) {
		PlayerService.play(view.getContext(), _id);
	}

	public void playAndShow(View view) {
		play(view);
		show(view);
	}

	public void removeFromPlaylist(View view) {
		removeFromPlaylist();
	}

	public void togglePlaylist(View view) {
		if (getPlaylistPosition() == null)
			addToPlaylist();
		else
			removeFromPlaylist();
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
			return ContextCompat.getColor(context, android.R.color.holo_green_dark);
		else
			return ContextCompat.getColor(context, android.R.color.holo_red_dark);
	}

	public String getReleaseDate(Context context) {
		return context.getString(R.string.released_on) + " " + DateFormat.getInstance().format(getPubDate());
	}

	public boolean hasFlattrPaymentUrl() {
		return getPaymentUrl() != null && FlattrHelper.isFlattrUri(Uri.parse(getPaymentUrl()));
	}
}
