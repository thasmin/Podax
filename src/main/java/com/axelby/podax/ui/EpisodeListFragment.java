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
import android.databinding.DataBindingUtil;
import android.databinding.ObservableArrayList;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableList;
import android.databinding.ViewDataBinding;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import javax.annotation.Nonnull;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class EpisodeListFragment extends RxFragment {
	private long _subscriptionId = -1;

	private LinearLayout _listView;

	private final DateFormat _pubDateFormat = DateFormat.getDateInstance();
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
		String rssUrl = getArguments().getString(Constants.EXTRA_RSSURL);
		if (_subscriptionId == -1 && itunesIdUrl == null && rssUrl == null)
			throw new IllegalArgumentException("EpisodeListFragment needs a subscription id, itunes id url, or rss url");

		_model = new Model(getArguments().getString(Constants.EXTRA_SUBSCRIPTION_NAME));

		Observable<Long> subIdObservable;
		if (_subscriptionId != -1) {
			subIdObservable = Observable.just(_subscriptionId);
			setupHeader(_subscriptionId);
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
			.map(this::getPodcastsCursor)
			.observeOn(AndroidSchedulers.mainThread())
			.compose(RxLifecycle.bindFragment(lifecycle()))
			.subscribe(
				this::showPodcasts,
				e -> Log.e("itunesloader", "error while getting rss url from itunes", e)
			);
	}

	private Observable<Long> getSubscriptionIdFromRSSUrl(String rssUrl) {
		String[] projection = new String[] { SubscriptionProvider.COLUMN_ID };
		Cursor c = getActivity().getContentResolver().query(SubscriptionProvider.URI, projection, null, null, null);
		if (c != null) {
			try {
				if (!c.moveToFirst() && c.isNull(0))
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
		Cursor c = db.rawQuery("SELECT " + SubscriptionProvider.COLUMN_URL + " FROM subscriptions WHERE id = (SELECT subscriptionID FROM itunes WHERE idUrl = ?)", new String[]{iTunesUrl});
		if (c != null) {
			if (c.moveToFirst() && c.isNull(0)) {
				String url = c.getString(0);
				c.close();
				return Observable.just(url);
			}
			c.close();
		}

		return new RSSUrlFetcher(getActivity(), iTunesUrl).getRSSUrl();
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
		_binding.setModel(_model);
		return _binding.getRoot();
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		_listView = (LinearLayout) view.findViewById(R.id.list);
	}

	@BindingAdapter("android:onChange")
	@SuppressWarnings("unused")
	public static void setOnChangeListener(CompoundButton button, CompoundButton.OnCheckedChangeListener listener) {
		button.setOnCheckedChangeListener(listener);
	}

	@BindingAdapter({"app:children", "app:childLayout"})
	@SuppressWarnings("unused")
	public static <T> void setChildren(ViewGroup parent, Collection<T> children, @LayoutRes int layoutId) {
		if (children == null) {
			parent.removeAllViews();
			return;
		}

		for (T child : children)
			addBoundChild(parent, layoutId, child, -1);

		if (children instanceof ObservableList<?>) {
			ObservableList<T> observables = (ObservableList<T>) children;
			observables.addOnListChangedCallback(new ObservableList.OnListChangedCallback<ObservableList<T>>() {
				@Override
				public void onChanged(ObservableList<T> sender) {
					for (T child : sender)
						addBoundChild(parent, layoutId, child, -1);
				}

				@Override
				public void onItemRangeChanged(ObservableList<T> sender, int positionStart, int itemCount) {
					for (int i = positionStart; i < positionStart + itemCount; ++i) {
						parent.removeViewAt(i);
						addBoundChild(parent, layoutId, sender.get(i), i);
					}
				}

				@Override
				public void onItemRangeInserted(ObservableList<T> sender, int positionStart, int itemCount) {
					for (int i = positionStart; i < positionStart + itemCount; ++i)
						addBoundChild(parent, layoutId, sender.get(i), i);
				}

				@Override
				public void onItemRangeMoved(ObservableList<T> sender, int fromPosition, int toPosition, int itemCount) {
					for (int i = fromPosition; i < fromPosition + itemCount; ++i)
						parent.removeViewAt(i);
					for (int i = toPosition; i < toPosition + itemCount; ++i)
						addBoundChild(parent, layoutId, sender.get(i), i);
				}

				@Override
				public void onItemRangeRemoved(ObservableList<T> sender, int positionStart, int itemCount) {
					for (int i = positionStart; i < positionStart + itemCount; ++i)
						parent.removeViewAt(i);
				}
			});
		}

	}

	private static <T> void addBoundChild(ViewGroup parent, @LayoutRes int layoutId, T child, int position) {
		ViewDataBinding v = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), layoutId, parent, false);
		v.setVariable(BR.podcast, child);
		v.executePendingBindings();
		if (position == -1)
			parent.addView(v.getRoot());
		else
			parent.addView(v.getRoot(), position);
	}

	@SuppressWarnings("unused")
	public class Model extends BaseObservable {
		private long _id;
		public final ObservableField<String> title = new ObservableField<>("");
		public final ObservableBoolean isCurrentlyUpdating = new ObservableBoolean(false);
		public final ObservableBoolean isSubscribed = new ObservableBoolean(false);
		public final ObservableBoolean areNewEpisodesAddedToPlaylist = new ObservableBoolean(false);
		public final ObservableArrayList<PodcastModel> podcasts = new ObservableArrayList<>();

		public Model (String name) {
			title.set(name);
		}

		public Model(@Nonnull SubscriptionCursor sub) {
			_id = sub.getId();
			title.set(sub.getTitle());
			isCurrentlyUpdating.set(false);
			isSubscribed.set(!sub.isSingleUse());
			areNewEpisodesAddedToPlaylist.set(sub.areNewEpisodesAddedToPlaylist());
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

	@SuppressWarnings("unused")
	public class PodcastModel extends BaseObservable {
		private long _id;
		private final String _title;
		private final Date _releaseDate;
		private final Integer _duration;
		private Integer _playlistPosition = null;
		private final boolean _isDownloaded;

		public PodcastModel(EpisodeCursor episode) {
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

		public View.OnClickListener onClick = button ->
			startActivity(PodaxFragmentActivity.createIntent(getActivity(), EpisodeDetailFragment.class, Constants.EXTRA_EPISODE_ID, _id));
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

		_model = new Model(subscriptionCursor);
		_binding.setModel(_model);
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
				setupHeader(_subscriptionId);
				showPodcasts(getPodcastsCursor(_subscriptionId));
				_model.isCurrentlyUpdating.set(false);
			} else {
				_model.isCurrentlyUpdating.set(true);
			}
		}
	};

	private void showPodcasts(Cursor cursor) {
		if (_listView == null)
			return;
		_listView.removeAllViews();

		_model.podcasts.clear();
		if (!cursor.moveToFirst())
			return;

		ArrayList<PodcastModel> models = new ArrayList<>(cursor.getCount());
		do {
			EpisodeCursor episode = new EpisodeCursor(cursor);
			PodcastModel podcastModel = new PodcastModel(episode);
			models.add(podcastModel);

			EpisodelistItemBinding x = EpisodelistItemBinding.inflate(LayoutInflater.from(getActivity()), _listView, false);
			x.setPodcast(podcastModel);

			_listView.addView(x.getRoot());
		} while (cursor.moveToNext());

		_model.podcasts.addAll(models);
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
