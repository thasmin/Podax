package com.axelby.podax.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.EpisodeCursor;
import com.axelby.podax.EpisodeProvider;
import com.axelby.podax.Helper;
import com.axelby.podax.ITunesPodcastLoader;
import com.axelby.podax.PlayerService;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.UpdateService;
import com.trello.rxlifecycle.RxLifecycle;
import com.trello.rxlifecycle.components.RxFragment;

import java.text.DateFormat;

import javax.annotation.Nonnull;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class EpisodeListFragment extends RxFragment {
	private PodcastAdapter _adapter = null;
	private long _subscriptionId = -1;

	private View _currentlyUpdatingMessage;
	private View _top;
	private RecyclerView _listView;
	private CheckBox _subscribed;
	private CheckBox _addNewEpisodes;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		_adapter = new PodcastAdapter();

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
				.subscribe(new Subscriber<Cursor>() {
					@Override
					public void onCompleted() { }

					@Override
					public void onError(Throwable e) {
						Log.e("episodelistfragment", "error while loading podcasts from db", e);
					}

					@Override
					public void onNext(Cursor cursor) {
						_adapter.changeCursor(cursor);
					}
				});
		} else {
			ITunesPodcastLoader.getRSSUrl(itunesIdUrl)
				.first()
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
				.subscribe(new Subscriber<Cursor>() {
					@Override
					public void onCompleted() {
					}

					@Override
					public void onError(Throwable e) {
						Log.e("itunesloader", "error while getting rss url from itunes", e);
					}

					@Override
					public void onNext(Cursor cursor) {
						_adapter.changeCursor(cursor);
					}
				});
		}
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
		return inflater.inflate(R.layout.episodelist_fragment, container, false);
	}

	@Override
	public void onDetach() {
		super.onDetach();

		if (_adapter != null)
			_adapter.closeCursor();
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		Context context = view.getContext();

		_top = view.findViewById(R.id.top);
		_listView = (RecyclerView) view.findViewById(R.id.list);
		_currentlyUpdatingMessage = view.findViewById(R.id.currently_updating);
		_subscribed = (CheckBox) view.findViewById(R.id.subscribe);
		_addNewEpisodes = (CheckBox) view.findViewById(R.id.add_new_episodes);

		if (_subscriptionId != -1)
			setupHeader();

		_listView.setLayoutManager(new LinearLayoutManager(context));
		_listView.setItemAnimator(new DefaultItemAnimator());
		_listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				_top.setTranslationY(_top.getTranslationY() - dy);
			}
		});
		_listView.setAdapter(_adapter);
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

		// make sure listview is in proper spot below top part
		ViewTreeObserver.OnGlobalLayoutListener layoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				_listView.setPadding(0, _top.getHeight(), 0, 0);
				_top.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			}
		};
		_top.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);

		ImageView thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
		Bitmap thumbnailImage = SubscriptionCursor.getThumbnailImage(context, _subscriptionId);
		thumbnail.setImageBitmap(thumbnailImage);

		TextView title = (TextView) view.findViewById(R.id.subscription);
		title.setText(subscriptionCursor.getTitle());
		if (thumbnailImage != null) {
			Palette palette = Palette.from(thumbnailImage).generate();
			title.setTextColor(palette.getVibrantColor(title.getTextColors().getDefaultColor()));
		} else {
			thumbnail.setVisibility(View.GONE);
		}

		_subscribed.setChecked(!subscriptionCursor.isSingleUse());
		_subscribed.setOnCheckedChangeListener((button, isChecked) -> {
			ContentValues values = new ContentValues(1);
			values.put(SubscriptionProvider.COLUMN_SINGLE_USE, !isChecked);
			ContentResolver contentResolver = button.getContext().getContentResolver();
			Uri subscriptionUri = SubscriptionProvider.getContentUri(_subscriptionId);
			contentResolver.update(subscriptionUri, values, null, null);

			_addNewEpisodes.setChecked(isChecked);
		});

		_addNewEpisodes.setChecked(subscriptionCursor.areNewEpisodesAddedToPlaylist());
		_addNewEpisodes.setOnCheckedChangeListener((button, isChecked) -> {
			ContentValues values = new ContentValues(1);
			values.put(SubscriptionProvider.COLUMN_PLAYLIST_NEW, isChecked);
			ContentResolver contentResolver = button.getContext().getContentResolver();
			Uri subscriptionUri = SubscriptionProvider.getContentUri(_subscriptionId);
			contentResolver.update(subscriptionUri, values, null, null);
		});

		subscriptionCursor.closeCursor();
	}

	private BroadcastReceiver _updateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Constants.ACTION_DONE_UPDATING_SUBSCRIPTION)) {
				if (_currentlyUpdatingMessage.getVisibility() == View.VISIBLE)
					setupHeader();
				_currentlyUpdatingMessage.setVisibility(View.GONE);
				return;
			}

			long updatingId = intent.getLongExtra(Constants.EXTRA_SUBSCRIPTION_ID, -1);
			if (updatingId == -1 || updatingId != _subscriptionId) {
				_currentlyUpdatingMessage.setVisibility(View.GONE);
			} else {
				_currentlyUpdatingMessage.setVisibility(View.VISIBLE);
			}
		}
	};

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
		_currentlyUpdatingMessage.setVisibility(isCurrentlyUpdating ? View.VISIBLE : View.GONE);

		if (getView() == null)
			return;
		IntentFilter intentFilter = new IntentFilter(Constants.ACTION_UPDATE_SUBSCRIPTION);
		intentFilter.addAction(Constants.ACTION_DONE_UPDATING_SUBSCRIPTION);
		intentFilter.addDataScheme("content");
		LocalBroadcastManager.getInstance(getView().getContext()).registerReceiver(_updateReceiver, intentFilter);
	}

	public class ViewHolder extends RecyclerView.ViewHolder {
		public final View container;
		public final TextView title;
		public final TextView date;
		public final TextView duration;
		public final Button play;
		public final Button playlist;

		public ViewHolder (View view) {
			super(view);

			container = view;
			title = (TextView) view.findViewById(R.id.title);
			date = (TextView) view.findViewById(R.id.date);
			duration = (TextView) view.findViewById(R.id.duration);
			play = (Button) view.findViewById(R.id.play);
			playlist = (Button) view.findViewById(R.id.playlist);

			play.setOnClickListener(button -> {
				long episodeId = (Long) button.getTag();
				PlayerService.play(button.getContext(), episodeId);

				// put podcast on top of playlist
				ContentValues values = new ContentValues(1);
				values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, 0);
				button.getContext().getContentResolver().update(EpisodeProvider.getContentUri(episodeId), values, null, null);

				startActivity(PodaxFragmentActivity.createIntent(getActivity(), EpisodeDetailFragment.class, Constants.EXTRA_EPISODE_ID, episodeId));
			});

			playlist.setOnClickListener(button -> {
				long episodeId = (Long) button.getTag(R.id.episodeId);
				Integer position = (Integer) button.getTag(R.id.playlist);

				ContentValues values = new ContentValues(1);
				values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, position);
				button.getContext().getContentResolver().update(EpisodeProvider.getContentUri(episodeId), values, null, null);
			});

			view.setOnClickListener(button -> {
				long episodeId = (Long) button.getTag();
				startActivity(PodaxFragmentActivity.createIntent(getActivity(), EpisodeDetailFragment.class, Constants.EXTRA_EPISODE_ID, episodeId));
			});
		}
	}

	private class PodcastAdapter extends RecyclerView.Adapter<ViewHolder> {

		private final DateFormat _pubDateFormat = DateFormat.getDateInstance();
		private Cursor _cursor;

		private ContentObserver _podcastCursorObserver = new ContentObserver(new Handler()) {
			@Override public boolean deliverSelfNotifications() { return true; }
			@Override public void onChange(boolean selfChange) { onChange(selfChange, null); }
			@Override public void onChange(boolean selfChange, Uri uri) { notifyDataSetChanged(); }
		};

		public PodcastAdapter() {
			setHasStableIds(true);
		}

		public void changeCursor(Cursor cursor) {
			closeCursor();
			_cursor = cursor;
			_cursor.registerContentObserver(_podcastCursorObserver);
			notifyDataSetChanged();
		}

		public void closeCursor() {
			if (_cursor == null)
				return;
			_cursor.unregisterContentObserver(_podcastCursorObserver);
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.episodelist_item, parent, false);
			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			if (_cursor == null)
				return;
			Context context = holder.container.getContext();

			_cursor.moveToPosition(position);
			EpisodeCursor episode = new EpisodeCursor(_cursor);

			holder.container.setTag(episode.getId());
			holder.title.setText(episode.getTitle());
			holder.date.setText(context.getString(R.string.released_on) + " " + _pubDateFormat.format(episode.getPubDate()));
			if (episode.getDuration() > 0) {
				holder.duration.setText(Helper.getVerboseTimeString(context, episode.getDuration() / 1000f, false) + " " + context.getString(R.string.in_duration));
				holder.duration.setVisibility(View.VISIBLE);
			} else
				holder.duration.setVisibility(View.GONE);
			holder.play.setTag(episode.getId());
			holder.playlist.setTag(R.id.episodeId, episode.getId());

			Integer inPlaylist = episode.getPlaylistPosition();
			if (inPlaylist == null) {
				holder.playlist.setTag(R.id.playlist, Integer.MAX_VALUE);
				holder.playlist.setText(R.string.add_to_playlist);
			} else {
				holder.playlist.setTag(R.id.playlist, null);
				holder.playlist.setText(R.string.remove_from_playlist);
			}

			if (episode.isDownloaded(context))
				holder.play.setText(R.string.play);
			else
				holder.play.setText(R.string.stream);
		}

		@Override
		public long getItemId(int position) {
			_cursor.moveToPosition(position);
			return new EpisodeCursor(_cursor).getId();
		}

		@Override
		public int getItemCount() {
			if (_cursor == null)
				return 0;
			return _cursor.getCount();
		}
	}
}
