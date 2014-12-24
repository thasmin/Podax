package com.axelby.podax.ui;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.EpisodeCursor;
import com.axelby.podax.EpisodeProvider;
import com.axelby.podax.PlayerService;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.UpdateService;

import java.io.File;

import javax.annotation.Nonnull;

public class PlaylistFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	PlaylistListAdapter _adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		_adapter = new PlaylistListAdapter();
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.playlist, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		RecyclerView listView = (RecyclerView) getActivity().findViewById(R.id.list);
		listView.setLayoutManager(new LinearLayoutManager(getActivity()));
		listView.setItemAnimator(new DefaultItemAnimator());
		listView.setAdapter(_adapter);

		/* TODO
		// disable swipe to remove according to preference
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		if (!preferences.getBoolean("allowPlaylistSwipeToRemove", true))
			dragSortController.setRemoveEnabled(false);
		*/
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.playlist_fragment, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.download) {
			UpdateService.downloadEpisodes(getActivity());
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = new String[]{
				EpisodeProvider.COLUMN_ID,
				EpisodeProvider.COLUMN_TITLE,
				EpisodeProvider.COLUMN_SUBSCRIPTION_ID,
				EpisodeProvider.COLUMN_SUBSCRIPTION_TITLE,
				EpisodeProvider.COLUMN_PLAYLIST_POSITION,
				EpisodeProvider.COLUMN_MEDIA_URL,
				EpisodeProvider.COLUMN_FILE_SIZE,
				EpisodeProvider.COLUMN_SUBSCRIPTION_ID,
		};
		return new CursorLoader(getActivity(), EpisodeProvider.PLAYLIST_URI, projection, null, null, null);
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		_adapter.changeCursor(cursor);
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		if (getActivity() == null)
			return;

		_adapter.changeCursor(null);
	}

	private class PlaylistListAdapter extends RecyclerView.Adapter<PlaylistListAdapter.ViewHolder> {

		private Cursor _cursor = null;

		class ViewHolder extends RecyclerView.ViewHolder {
			public View container;
            public TextView title;
            public TextView subscription;
            public ImageView thumbnail;
            public TextView downloaded;
			public View play;
			public View remove;

            public ViewHolder(View view) {
				super(view);

				container = view;
                title = (TextView) view.findViewById(R.id.title);
                subscription = (TextView) view.findViewById(R.id.subscription);
                thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
                downloaded = (TextView) view.findViewById(R.id.downloaded);

				container.setOnClickListener(_clickHandler);

				play = view.findViewById(R.id.play);
				play.setOnClickListener(_playHandler);

				remove = view.findViewById(R.id.remove);
				remove.setOnClickListener(_removeHandler);

            }
        }

		private OnClickListener _clickHandler = new OnClickListener() {
			@Override
			public void onClick(View view) {
				long episodeId = (Long) view.getTag();
				startActivity(PodaxFragmentActivity.createIntent(getActivity(), EpisodeDetailFragment.class, Constants.EXTRA_EPISODE_ID, episodeId));
			}
		};

        private OnClickListener _playHandler = new OnClickListener() {
            @Override
            public void onClick(View view) {
                long episodeId = (Long) view.getTag();
				PlayerService.play(getActivity(), episodeId);
            }
        };

        private OnClickListener _removeHandler = new OnClickListener() {
            @Override
            public void onClick(View view) {
                long episodeId = (Long) view.getTag();

                ContentValues values = new ContentValues();
                values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, (Integer) null);
                getActivity().getContentResolver().update(EpisodeProvider.getContentUri(episodeId), values, null, null);
            }
        };

		public PlaylistListAdapter() {
			setHasStableIds(true);
		}

		public void changeCursor(Cursor cursor) {
			_cursor = cursor;
			notifyDataSetChanged();
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_list_item, parent, false);
			return new ViewHolder(view);
		}

        @Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			if (_cursor == null)
				return;
			_cursor.moveToPosition(position);
			EpisodeCursor episode = new EpisodeCursor(_cursor);

			holder.play.setTag(episode.getId());
			holder.remove.setTag(episode.getId());
			holder.container.setTag(episode.getId());

            holder.title.setText(episode.getTitle());
            holder.subscription.setText(episode.getSubscriptionTitle());
            holder.thumbnail.setImageBitmap(SubscriptionCursor.getThumbnailImage(getActivity(), episode.getSubscriptionId()));

            float downloaded = new File(episode.getFilename(getActivity())).length();
            if (episode.getFileSize() != downloaded) {
                holder.downloaded.setTextColor(0xffcc0000); //android.R.color.holo_red_dark
                holder.downloaded.setText(R.string.not_downloaded);
            } else {
                holder.downloaded.setTextColor(0xff669900); //android.R.color.holo_green_dark
                holder.downloaded.setText(R.string.downloaded);
            }
		}

		@Override
		public int getItemCount() {
			if (_cursor == null)
				return 0;
			return _cursor.getCount();
		}

		@Override
		public long getItemId(int position) {
			_cursor.moveToPosition(position);
			return new EpisodeCursor(_cursor).getId();
		}

		/* TODO
		@Override
		public void drop(int from, int to) {
			Long podcastId = _adapter.getItemId(from);
			ContentValues values = new ContentValues();
			values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, to);
			Uri podcastUri = ContentUris.withAppendedId(EpisodeProvider.URI, podcastId);
			getActivity().getContentResolver().update(podcastUri, values, null, null);
		}

		@Override
		public void drag(int from, int to) {
		}

		@Override
		public void remove(int which) {
			Long podcastId = _adapter.getItemId(which);
			ContentValues values = new ContentValues();
			values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, (Integer) null);
			Uri podcastUri = ContentUris.withAppendedId(EpisodeProvider.URI, podcastId);
			getActivity().getContentResolver().update(podcastUri, values, null, null);
		}
		*/
	}
}
