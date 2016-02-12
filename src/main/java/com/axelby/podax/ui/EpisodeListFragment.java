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
import android.databinding.ObservableArrayList;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import com.axelby.podax.AppFlow;
import com.axelby.podax.BR;
import com.axelby.podax.Constants;
import com.axelby.podax.DBAdapter;
import com.axelby.podax.EpisodeCursor;
import com.axelby.podax.EpisodeData;
import com.axelby.podax.EpisodeProvider;
import com.axelby.podax.Helper;
import com.axelby.podax.PlayerService;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.UpdateService;
import com.axelby.podax.databinding.EpisodelistFragmentBinding;
import com.axelby.podax.itunes.RSSUrlFetcher;
import com.trello.rxlifecycle.RxLifecycle;
import com.trello.rxlifecycle.components.RxFragment;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class EpisodeListFragment extends RxFragment {
	private long _subscriptionId = -1;

	private View _layout;
	private LinearLayout _listView;

	private final DateFormat _pubDateFormat = DateFormat.getDateInstance();
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
		String rssUrl = getArguments().getString(Constants.EXTRA_RSSURL);
		if (_subscriptionId == -1 && itunesIdUrl == null && rssUrl == null)
			throw new IllegalArgumentException("EpisodeListFragment needs a subscription id, itunes id url, or rss url");

		_model = new Model(getArguments().getString(Constants.EXTRA_SUBSCRIPTION_NAME));

		Observable<Long> subIdObservable;
		if (_subscriptionId != -1) {
			subIdObservable = Observable.just(_subscriptionId);
		} else {
			// get subscription id from either rss url or itunes id url
			Observable<String> rssUrlObservable;
			if (itunesIdUrl != null)
				rssUrlObservable = getRSSUrlFromITunesUrl(itunesIdUrl);
			else
				rssUrlObservable = Observable.just(rssUrl);

			subIdObservable = rssUrlObservable
				.subscribeOn(Schedulers.io())
				.flatMap(this::getSubscriptionIdFromRSSUrl);
		}

		subIdObservable
			.observeOn(AndroidSchedulers.mainThread())
			.map(this::setupHeader)
			.observeOn(Schedulers.io())
			.flatMap(this::getEpisodes)
			.observeOn(AndroidSchedulers.mainThread())
			.compose(RxLifecycle.bindFragment(lifecycle()))
			.subscribe(
				this::showPodcasts,
				e -> Log.e("EpisodeListFragment", "error while retrieving episodes", e)
			);
	}

	private Observable<Long> getSubscriptionIdFromRSSUrl(String rssUrl) {
		String[] projection = new String[] { SubscriptionProvider.COLUMN_ID };
		String selection = SubscriptionProvider.COLUMN_URL + "=?";
		String[] selectionArgs = new String[] { rssUrl };
		Cursor c = getActivity().getContentResolver().query(SubscriptionProvider.URI, projection, selection, selectionArgs, null);
		if (c != null) {
			try {
				if (c.moveToFirst() && !c.isNull(0))
					return Observable.just(c.getLong(0));
			} finally {
				c.close();
			}
		}

		Uri newUri = SubscriptionProvider.addSingleUseSubscription(getActivity(), rssUrl);
		return Observable.just(ContentUris.parseId(newUri));
	}

	private Observable<String> getRSSUrlFromITunesUrl(String iTunesUrl) {
		SQLiteDatabase db = new DBAdapter(getActivity()).getReadableDatabase();
		Cursor c = db.rawQuery("SELECT " + SubscriptionProvider.COLUMN_URL + " FROM subscriptions WHERE _id = (SELECT subscriptionID FROM itunes WHERE idUrl = ?)", new String[]{iTunesUrl});
		if (c != null) {
			if (c.moveToFirst() && c.isNull(0)) {
				String url = c.getString(0);
				c.close();
				db.close();
				return Observable.just(url);
			}
			c.close();
		}
		db.close();

		return new RSSUrlFetcher(getActivity(), iTunesUrl).getRSSUrl();
	}

	public Observable<List<EpisodeData>> getEpisodes(long subId) {
		_subscriptionId = subId;
		return EpisodeData.getForSubscriptionId(getActivity(), new String[]{String.valueOf(subId)});
	}

	@Override
	public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		EpisodelistFragmentBinding binding = EpisodelistFragmentBinding.inflate(inflater, container, false);
		binding.setModel(_model);
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		_layout = view;
		_listView = (LinearLayout) view.findViewById(R.id.list);
	}

	@SuppressWarnings("unused")
	public class Model extends BaseObservable {
		private long _id;
		public final ObservableField<String> title = new ObservableField<>("");
		public final ObservableField<String> imageSrc = new ObservableField<>("");
		public final ObservableBoolean isCurrentlyUpdating = new ObservableBoolean(false);
		public final ObservableBoolean isSubscribed = new ObservableBoolean(false);
		public final ObservableBoolean areNewEpisodesAddedToPlaylist = new ObservableBoolean(false);
		public final ObservableArrayList<EpisodeData> podcasts = new ObservableArrayList<>();

		public Model (String name) {
			title.set(name);
		}

		public void set(SubscriptionCursor sub) {
			_id = sub.getId();
			imageSrc.set("file://" + SubscriptionCursor.getThumbnailFilename(getActivity(), _id));
			title.set(sub.getTitle());
			isCurrentlyUpdating.set(false);
			isSubscribed.set(!sub.isSingleUse());
			areNewEpisodesAddedToPlaylist.set(sub.areNewEpisodesAddedToPlaylist());
		}

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

	@SuppressWarnings("unused")
	public class PodcastModel extends BaseObservable {
		private long _id;
		private final String _title;
		private final Date _releaseDate;
		private final Integer _duration;
		private Integer _playlistPosition = null;
		private final boolean _isDownloaded;

		public PodcastModel(Cursor cursor) {
			EpisodeCursor episode = new EpisodeCursor(cursor);
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
		public boolean hasDuration() { return _duration != null && _duration != 0; }
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

			AppFlow.get(getActivity()).displayEpisode(_id);
		};

		public View.OnClickListener onPlaylist = button -> {
			Integer newPosition = (_playlistPosition == null) ? Integer.MAX_VALUE : null;
			ContentValues values = new ContentValues(1);
			values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, newPosition);
			button.getContext().getContentResolver().update(EpisodeProvider.getContentUri(_id), values, null, null);
		};

		public View.OnClickListener onClick = button -> {
			View thumbnail = _layout.findViewById(R.id.thumbnail);
			AppFlow.get(getActivity()).displayEpisode(_id, thumbnail);
		};
	}

	private long setupHeader(long subscriptionId) {
		_subscriptionId = subscriptionId;

		if (_subscriptionId == -1) {
			Log.e("EpisodeListFragment", "cannot set up header when subscription id is not set");
			return _subscriptionId;
		}

		View view = getView();
		if (view == null)
			return _subscriptionId;

		Context context = view.getContext();
		SubscriptionCursor subscriptionCursor = SubscriptionCursor.getCursor(context, _subscriptionId);
		if (subscriptionCursor == null)
			return _subscriptionId;

		_model.set(subscriptionCursor);
		subscriptionCursor.closeCursor();

		return _subscriptionId;
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
				_model.isCurrentlyUpdating.set(false);
				setupHeader(_subscriptionId);
				getEpisodes(_subscriptionId)
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(
						EpisodeListFragment.this::showPodcasts,
						e -> Log.e("EpisodeListFragment", "error while refreshing episodes after update", e)
					);
			} else {
				_model.isCurrentlyUpdating.set(true);
			}
		}
	};

	private void showPodcasts(List<EpisodeData> episodes) {
		if (episodes.size() != _model.podcasts.size()) {
			_model.podcasts.clear();
			_model.podcasts.addAll(episodes);
		}
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

		// refresh changes from subscription settings fragment
		setupHeader(_subscriptionId);
	}

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.episode_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.settings) {
            AppFlow.get(getActivity()).displaySubscriptionSettings(_subscriptionId);
            return true;
        }
        return false;
    }
}
