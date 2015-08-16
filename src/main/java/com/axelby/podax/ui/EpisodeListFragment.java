package com.axelby.podax.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.EpisodeCursor;
import com.axelby.podax.EpisodeProvider;
import com.axelby.podax.Helper;
import com.axelby.podax.PlayerService;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.UpdateService;

import java.text.DateFormat;

import javax.annotation.Nonnull;

public class EpisodeListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private PodcastAdapter _adapter = null;
	private long _subscriptionId;

	private View _currentlyUpdatingMessage;
	private View _top;
	private RecyclerView _listView;
	private CheckBox _subscribed;
	private CheckBox _addNewEpisodes;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		// extract subscription id
		if (getArguments() == null)
			throw new IllegalArgumentException("EpisodeListFragment needs a subscription id argument");
		_subscriptionId = getArguments().getLong(Constants.EXTRA_SUBSCRIPTION_ID, -1);
		if (_subscriptionId == -1)
			throw new IllegalArgumentException("EpisodeListFragment needs a subscription id argument");

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.episodelist_fragment, container, false);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		Context context = view.getContext();

		_top = view.findViewById(R.id.top);
		ViewTreeObserver.OnGlobalLayoutListener layoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				_listView.setPadding(0, _top.getHeight(), 0, 0);
				_top.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			}
		};
		_top.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);

		_listView = (RecyclerView) view.findViewById(R.id.list);
		_listView.setLayoutManager(new LinearLayoutManager(context));
		_listView.setItemAnimator(new DefaultItemAnimator());
		_listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				_top.setTranslationY(_top.getTranslationY() - dy);
			}
		});

		_adapter = new PodcastAdapter();
		_listView.setAdapter(_adapter);

		_currentlyUpdatingMessage = view.findViewById(R.id.currently_updating);

		_subscribed = (CheckBox) view.findViewById(R.id.subscribe);
		_subscribed.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton button, boolean isChecked) {
				ContentValues values = new ContentValues(1);
				values.put(SubscriptionProvider.COLUMN_SINGLE_USE, !isChecked);
				ContentResolver contentResolver = button.getContext().getContentResolver();
				Uri subscriptionUri = SubscriptionProvider.getContentUri(_subscriptionId);
				contentResolver.update(subscriptionUri, values, null, null);

				_addNewEpisodes.setChecked(isChecked);
			}
		});

		_addNewEpisodes = (CheckBox) view.findViewById(R.id.add_new_episodes);
		_addNewEpisodes.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton button, boolean isChecked) {
				ContentValues values = new ContentValues(1);
				values.put(SubscriptionProvider.COLUMN_PLAYLIST_NEW, isChecked);
				ContentResolver contentResolver = button.getContext().getContentResolver();
				Uri subscriptionUri = SubscriptionProvider.getContentUri(_subscriptionId);
				contentResolver.update(subscriptionUri, values, null, null);
			}
		});

		setupHeader();
	}

	void setupHeader() {
		View view = getView();
		if (view == null)
			return;

		Context context = view.getContext();
		SubscriptionCursor subscriptionCursor = SubscriptionCursor.getCursor(context, _subscriptionId);
		if (subscriptionCursor == null)
			return;

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
		_addNewEpisodes.setChecked(subscriptionCursor.areNewEpisodesAddedToPlaylist());

		subscriptionCursor.closeCursor();
	}

	private BroadcastReceiver _updateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			long updatingId = intent.getLongExtra(Constants.EXTRA_SUBSCRIPTION_ID, -1);
			if (updatingId == _subscriptionId) {
				_currentlyUpdatingMessage.setVisibility(View.VISIBLE);
			} else {
				if (_currentlyUpdatingMessage.getVisibility() == View.VISIBLE)
					setupHeader();
				_currentlyUpdatingMessage.setVisibility(View.GONE);
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
		intentFilter.addDataScheme("content");
		LocalBroadcastManager.getInstance(getView().getContext()).registerReceiver(_updateReceiver, intentFilter);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		if (getActivity() == null)
			return null;

		Uri uri = ContentUris.withAppendedId(SubscriptionProvider.URI, _subscriptionId);
		if (uri == null)
			return null;

		uri = Uri.withAppendedPath(uri, "podcasts");
		String[] projection = {
				EpisodeProvider.COLUMN_ID,
				EpisodeProvider.COLUMN_TITLE,
				EpisodeProvider.COLUMN_PUB_DATE,
				EpisodeProvider.COLUMN_DURATION,
				EpisodeProvider.COLUMN_MEDIA_URL,
				EpisodeProvider.COLUMN_FILE_SIZE,
				EpisodeProvider.COLUMN_PLAYLIST_POSITION,
		};
		return new CursorLoader(getActivity(), uri, projection, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		_adapter.changeCursor(cursor);
		setupHeader();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		_adapter.changeCursor(null);
	}

	private class PodcastAdapter extends RecyclerView.Adapter<PodcastAdapter.ViewHolder> {

		private final DateFormat _pubDateFormat = DateFormat.getDateInstance();
		private Cursor _cursor;

		class ViewHolder extends RecyclerView.ViewHolder {
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

				play.setOnClickListener(_playHandler);
				playlist.setOnClickListener(_playlistHandler);
				view.setOnClickListener(_clickHandler);
			}
		}

		public PodcastAdapter() {
			setHasStableIds(true);
		}

		public void changeCursor(Cursor cursor) {
			_cursor = cursor;
			notifyDataSetChanged();
		}

		final OnClickListener _playHandler = new OnClickListener() {
			@Override
			public void onClick(View view) {
				long episodeId = (Long) view.getTag();
				PlayerService.play(view.getContext(), episodeId);

				// put podcast on top of playlist
				ContentValues values = new ContentValues(1);
				values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, 0);
				view.getContext().getContentResolver().update(EpisodeProvider.getContentUri(episodeId), values, null, null);

				startActivity(PodaxFragmentActivity.createIntent(getActivity(), EpisodeDetailFragment.class, Constants.EXTRA_EPISODE_ID, episodeId));
			}
		};

		final OnClickListener _playlistHandler = new OnClickListener() {
			@Override
			public void onClick(View view) {
				long episodeId = (Long) view.getTag(R.id.episodeId);
				Integer position = (Integer) view.getTag(R.id.playlist);

				ContentValues values = new ContentValues(1);
				values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, position);
				view.getContext().getContentResolver().update(EpisodeProvider.getContentUri(episodeId), values, null, null);
			}
		};

		final OnClickListener _clickHandler = new OnClickListener() {
			@Override
			public void onClick(View view) {
				long episodeId = (Long) view.getTag();
				startActivity(PodaxFragmentActivity.createIntent(getActivity(), EpisodeDetailFragment.class, Constants.EXTRA_EPISODE_ID, episodeId));
			}
		};

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
