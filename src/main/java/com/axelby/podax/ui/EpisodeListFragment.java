package com.axelby.podax.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import com.axelby.podax.BR;
import com.axelby.podax.Constants;
import com.axelby.podax.DBAdapter;
import com.axelby.podax.EpisodeCursor;
import com.axelby.podax.EpisodeProvider;
import com.axelby.podax.Helper;
import com.axelby.podax.PlayerService;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.UpdateService;
import com.axelby.podax.databinding.EpisodelistFragmentBinding;
import com.axelby.podax.databinding.EpisodelistItemBinding;
import com.axelby.podax.itunes.RSSUrlFetcher;
import com.trello.rxlifecycle.RxLifecycle;
import com.trello.rxlifecycle.components.RxFragment;

import java.text.DateFormat;
import java.util.Date;

import javax.annotation.Nonnull;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class EpisodeListFragment extends RxFragment {
	private long _subscriptionId = -1;

	private LinearLayout _listView;

	private final DateFormat _pubDateFormat = DateFormat.getDateInstance();
	protected ItemModel[] _itemModels;
	private EpisodelistFragmentBinding _binding;
	protected Model _model;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (_listView != null)
			_listView.removeAllViews();

		// extract subscription id
		if (getArguments() == null)
			throw new IllegalArgumentException("EpisodeListFragment needs a subscription id or itunes id url");
		_subscriptionId = getArguments().getLong(Constants.EXTRA_SUBSCRIPTION_ID, -1);
		String itunesIdUrl = getArguments().getString(Constants.EXTRA_ITUNES_ID);
		if (_subscriptionId == -1 && itunesIdUrl == null)
			throw new IllegalArgumentException("EpisodeListFragment needs a subscription id or itunes id url");

		if (_subscriptionId != -1) {
			Observable.just(_subscriptionId)
				.observeOn(Schedulers.io())
				.map(this::getPodcastsCursor)
				.observeOn(AndroidSchedulers.mainThread())
				.compose(RxLifecycle.bindFragment(lifecycle()))
				.subscribe(
					this::showPodcasts,
					e -> Log.e("episodelistfragment", "error while loading podcasts from db", e)
				);
		} else {
			Observable.just(itunesIdUrl)
				.observeOn(Schedulers.io())
				.flatMap(this::getSubscriptionIdFromITunesUrl)
				.concatWith(new RSSUrlFetcher(activity, itunesIdUrl).getRSSUrl()
						.flatMap(url -> {
							Cursor c = activity.getContentResolver().query(SubscriptionProvider.URI,
								new String[]{"_id"}, "url = ?", new String[]{url}, null);
							if (c != null) {
								try {
									if (c.moveToNext())
										return Observable.just(c.getLong(0));
								} finally {
									c.close();
								}
							}

							Uri newUri = SubscriptionProvider.addSingleUseSubscription(activity, url);
							return Observable.just(ContentUris.parseId(newUri));
						})
				)
				.first()
				.observeOn(AndroidSchedulers.mainThread())
				.map(subId -> {
					_subscriptionId = subId;
					setupHeader();
					return subId;
				})
				.observeOn(Schedulers.io())
				.map(this::getPodcastsCursor)
				.observeOn(AndroidSchedulers.mainThread())
				.compose(RxLifecycle.bindFragment(lifecycle()))
				.subscribe(
					this::showPodcasts,
					e -> Log.e("itunesloader", "error while getting rss url from itunes", e)
				);
		}
	}

	private Observable<Long> getSubscriptionIdFromITunesUrl(String iTunesUrl) {
		SQLiteDatabase db = new DBAdapter(getActivity()).getReadableDatabase();
		Cursor c = db.rawQuery("SELECT subscriptionId FROM itunes WHERE idUrl = ?", new String[]{iTunesUrl});
		if (c == null)
			return Observable.empty();
		if (!c.moveToFirst() || c.isNull(0)) {
			c.close();
			return Observable.empty();
		}
		long subId = c.getLong(0);
		c.close();
		return Observable.just(subId);
	}

	public Cursor getPodcastsCursor(long subId) {
		_subscriptionId = subId;

		Uri uri = ContentUris.withAppendedId(SubscriptionProvider.URI, _subscriptionId);
		if (uri == null)
			return null;

		Uri podcastsUri = Uri.withAppendedPath(uri, "podcasts");
		String[] projection = {
				EpisodeProvider.COLUMN_ID,
				EpisodeProvider.COLUMN_TITLE,
				EpisodeProvider.COLUMN_PUB_DATE,
				EpisodeProvider.COLUMN_DURATION,
				EpisodeProvider.COLUMN_MEDIA_URL,
				EpisodeProvider.COLUMN_FILE_SIZE,
				EpisodeProvider.COLUMN_PLAYLIST_POSITION,
		};
		Cursor c = getActivity().getContentResolver().query(podcastsUri, projection, null, null, null);
		if (c == null)
			return null;
		if (c.getCount() == 0)
			UpdateService.updateSubscription(getActivity(), uri);
		return c;
	}

	@Override
	public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		_binding = EpisodelistFragmentBinding.inflate(inflater, container, false);
		return _binding.getRoot();
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		_listView = (LinearLayout) view.findViewById(R.id.list);

		if (_subscriptionId != -1)
			setupHeader();
	}

	@BindingAdapter("android:onChange")
	@SuppressWarnings("unused")
	public static void setOnChangeListener(CompoundButton button, CompoundButton.OnCheckedChangeListener listener) {
		button.setOnCheckedChangeListener(listener);
	}

	@SuppressWarnings("unused")
	public class Model extends BaseObservable {
		private long _id;
		public final ObservableField<String> title;
		public final ObservableBoolean isCurrentlyUpdating;
		public final ObservableBoolean isSubscribed;
		public ObservableBoolean areNewEpisodesAddedToPlaylist;

		public Model(@Nonnull SubscriptionCursor sub) {
			_id = sub.getId();
			title = new ObservableField<>(sub.getTitle());
			isCurrentlyUpdating = new ObservableBoolean(false);
			isSubscribed = new ObservableBoolean(!sub.isSingleUse());
			areNewEpisodesAddedToPlaylist = new ObservableBoolean(sub.areNewEpisodesAddedToPlaylist());
		}

		public String getImageSrc() { return "file://" + SubscriptionCursor.getThumbnailFilename(getActivity(), _id); }

		public CompoundButton.OnCheckedChangeListener subscribeChange = (button, isChecked) -> {
			ContentValues values = new ContentValues(1);
			values.put(SubscriptionProvider.COLUMN_SINGLE_USE, !isChecked);
			ContentResolver contentResolver = button.getContext().getContentResolver();
			Uri subscriptionUri = SubscriptionProvider.getContentUri(_id);
			contentResolver.update(subscriptionUri, values, null, null);

			areNewEpisodesAddedToPlaylist.set(isChecked);
		};

		public CompoundButton.OnCheckedChangeListener addNewToPlaylistChange = (button, isChecked) -> {
			ContentValues values = new ContentValues(1);
			values.put(SubscriptionProvider.COLUMN_PLAYLIST_NEW, isChecked);
			ContentResolver contentResolver = button.getContext().getContentResolver();
			Uri subscriptionUri = SubscriptionProvider.getContentUri(_id);
			contentResolver.update(subscriptionUri, values, null, null);
		};
	}

	void setupHeader() {
		if (_subscriptionId == -1) {
			Log.e("EpisodeListFragment", "cannot set up header when subscription id is not set");
			return;
		}

		View view = getView();
		if (view == null)
			return;

		Context context = view.getContext();
		SubscriptionCursor subscriptionCursor = SubscriptionCursor.getCursor(context, _subscriptionId);
		if (subscriptionCursor == null)
			return;

		_model = new Model(subscriptionCursor);
		_binding.setModel(_model);
		subscriptionCursor.closeCursor();
	}

	private BroadcastReceiver _updateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			long updatingId = ContentUris.parseId(intent.getData());
			if (updatingId == -1 || updatingId != _subscriptionId) {
				_model.isCurrentlyUpdating.set(false);
				return;
			}

			if (intent.getAction().equals(Constants.ACTION_DONE_UPDATING_SUBSCRIPTION)) {
				setupHeader();
				showPodcasts(getPodcastsCursor(_subscriptionId));
				_model.isCurrentlyUpdating.set(false);
			} else {
				_model.isCurrentlyUpdating.set(true);
			}
		}
	};

	@SuppressWarnings("unused")
	public class ItemModel extends BaseObservable {
		private long _id;
		private final String _title;
		private final Date _releaseDate;
		private final Integer _duration;
		private Integer _playlistPosition = null;
		private final boolean _isDownloaded;

		public ItemModel(EpisodeCursor episode) {
			_id = episode.getId();
			_title = episode.getTitle();
			_releaseDate = episode.getPubDate();
			_duration = episode.getDuration();
			_playlistPosition = episode.getPlaylistPosition();
			_isDownloaded = episode.isDownloaded(getActivity());
		}

		public String getTitle() { return _title; }
		public String getReleaseDate() {
			return getActivity().getString(R.string.released_on) + " " + _pubDateFormat.format(_releaseDate);
		}
		public boolean hasDuration() { return _duration != null; }
		public String getDuration() {
			return Helper.getVerboseTimeString(getActivity(), _duration / 1000f, false) + " " + getActivity().getString(R.string.in_duration);
		}
		@Bindable public Integer getPlaylistPosition() { return _playlistPosition; }
		public boolean isDownloaded() { return _isDownloaded; }

		public void setPlaylistPosition(Integer newPosition) {
			_playlistPosition = newPosition;
			notifyPropertyChanged(BR.playlistPosition);
		}

		public View.OnClickListener onPlay = v -> {
			PlayerService.play(v.getContext(), _id);

			// put podcast on top of playlist
			ContentValues values = new ContentValues(1);
			values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, 0);
			v.getContext().getContentResolver().update(EpisodeProvider.getContentUri(_id), values, null, null);

			setPlaylistPosition(0);

			startActivity(PodaxFragmentActivity.createIntent(getActivity(), EpisodeDetailFragment.class, Constants.EXTRA_EPISODE_ID, _id));
		};

		public View.OnClickListener onPlaylist = button -> {
			Integer newPosition = (_playlistPosition == null) ? Integer.MAX_VALUE : null;
			ContentValues values = new ContentValues(1);
			values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, newPosition);
			button.getContext().getContentResolver().update(EpisodeProvider.getContentUri(_id), values, null, null);
		};

		public View.OnClickListener onClick = button -> {
			startActivity(PodaxFragmentActivity.createIntent(getActivity(), EpisodeDetailFragment.class, Constants.EXTRA_EPISODE_ID, _id));
		};
	}

	private void showPodcasts(Cursor cursor) {
		if (_listView == null)
			return;
		_listView.removeAllViews();

		_itemModels = new ItemModel[cursor.getCount()];
		int i = 0;
		if (!cursor.moveToFirst())
			return;

		do {
			EpisodeCursor episode = new EpisodeCursor(cursor);
			_itemModels[i] = new ItemModel(episode);

			EpisodelistItemBinding x = EpisodelistItemBinding.inflate(LayoutInflater.from(getActivity()), _listView, false);
			x.setModel(_itemModels[i]);

			_listView.addView(x.getRoot());
		} while (cursor.moveToNext());

		cursor.close();
	}

	@Override
	public void onPause() {
		super.onPause();

		if (getView() == null)
			return;
		LocalBroadcastManager.getInstance(getView().getContext()).unregisterReceiver(_updateReceiver);
	}

	@Override
	public void onResume() {
		super.onResume();

		boolean isCurrentlyUpdating = (UpdateService.getUpdatingSubscriptionId() == _subscriptionId);
		_model.isCurrentlyUpdating.set(isCurrentlyUpdating);

		if (getView() == null)
			return;
		IntentFilter intentFilter = new IntentFilter(Constants.ACTION_UPDATE_SUBSCRIPTION);
		intentFilter.addAction(Constants.ACTION_DONE_UPDATING_SUBSCRIPTION);
		intentFilter.addDataScheme("content");
		try {
			intentFilter.addDataType(SubscriptionProvider.ITEM_TYPE);
		} catch (IntentFilter.MalformedMimeTypeException ignored) { }
		LocalBroadcastManager.getInstance(getView().getContext()).registerReceiver(_updateReceiver, intentFilter);
	}

}
