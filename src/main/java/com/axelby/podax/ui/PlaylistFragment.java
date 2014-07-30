package com.axelby.podax.ui;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.EpisodeCursor;
import com.axelby.podax.EpisodeProvider;
import com.axelby.podax.Helper;
import com.axelby.podax.PlayerService;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.UpdateService;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.DragSortListView.DragListener;
import com.mobeta.android.dslv.DragSortListView.DropListener;
import com.mobeta.android.dslv.DragSortListView.RemoveListener;

import java.io.File;

import javax.annotation.Nonnull;

public class PlaylistFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	// make sure list is refreshed to update downloading files
	Runnable _refresher = new Runnable() {
		public void run() {
			if (getActivity() == null)
				return;

			// make sure the listview is populated
			if (getListView().getChildCount() == 0) {
				_handler.postDelayed(_refresher, 1000);
				return;
			}

			boolean repost = false;
			for (int i = 0; i < getListAdapter().getCount(); ++i) {
				View view = getListView().getChildAt(i);
				if (view == null)
					continue;
                PlaylistListAdapter.ViewHolder holder = (PlaylistListAdapter.ViewHolder) view.getTag();
                if (holder == null)
                    continue;
                if (holder.downloaded.getText().subSequence(0, 3).equals("100"))
					continue;

				EpisodeCursor episode = new EpisodeCursor((Cursor) getListAdapter().getItem(i));
                int filesizePct = 0;
                if (episode.getFileSize() != null) {
                    float downloaded = new File(episode.getFilename(getActivity())).length();
                    filesizePct = Math.round(100.0f * downloaded / episode.getFileSize());
                }
                holder.downloaded.setText(String.valueOf(filesizePct) + "% downloaded");

                if (filesizePct < 100)
					repost = true;
			}

			if (repost)
				_handler.postDelayed(_refresher, 1000);
		}
	};
	Handler _handler = new Handler();
	PlaylistListAdapter _adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		getLoaderManager().initLoader(0, null, this);

		_adapter = new PlaylistListAdapter(getActivity(), null);
		setListAdapter(_adapter);
	}

	@Override
	public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.playlist, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getListView().setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Helper.changeFragment(getActivity(), EpisodeDetailFragment.class, Constants.EXTRA_EPISODE_ID, id);
			}
		});

        DragSortListView dragSortListView = (DragSortListView) getListView();
        DragSortController dragSortController = (DragSortController) dragSortListView.getFloatViewManager();
        dragSortController.setBackgroundColor(Color.WHITE);

		// disable swipe to remove according to preference
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		if (!preferences.getBoolean("allowPlaylistSwipeToRemove", true))
			dragSortController.setRemoveEnabled(false);
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

    @Override
    public void onListItemClick(ListView listview, View view, int position, long id) {
        Helper.changeFragment(getActivity(), EpisodeDetailFragment.class, Constants.EXTRA_EPISODE_ID, id);
    }

    @Override
	public void onPause() {
		super.onPause();
		_handler.removeCallbacks(_refresher);
	}

	@Override
	public void onResume() {
		super.onResume();
		_refresher.run();
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

	private class PlaylistListAdapter extends ResourceCursorAdapter implements DragListener, DropListener, RemoveListener {

        class ViewHolder {
            public TextView title;
            public TextView subscription;
            public ImageView thumbnail;
            public TextView downloaded;

            public ViewHolder(View view) {
                title = (TextView) view.findViewById(R.id.title);
                subscription = (TextView) view.findViewById(R.id.subscription);
                thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
                downloaded = (TextView) view.findViewById(R.id.downloaded);
            }
        }

        private OnClickListener _playHandler = new OnClickListener() {
            @Override
            public void onClick(View view) {
                long episodeId = (Long) view.getTag();
				PlayerService.play(getActivity(), episodeId);
                Helper.changeFragment(getActivity(), EpisodeDetailFragment.class, Constants.EXTRA_EPISODE_ID, episodeId);
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

		public PlaylistListAdapter(Context context, Cursor cursor) {
			super(context, R.layout.playlist_list_item, cursor, true);
		}

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = super.newView(context, cursor, parent);
            view.setTag(new ViewHolder(view));

            EpisodeCursor episode = new EpisodeCursor(cursor);

            View play = view.findViewById(R.id.play);
            play.setTag(episode.getId());
            play.setOnClickListener(_playHandler);

            View remove = view.findViewById(R.id.remove);
            remove.setTag(episode.getId());
            remove.setOnClickListener(_removeHandler);

            return view;
        }

        @Override
		public void bindView(View view, Context context, Cursor cursor) {
			EpisodeCursor episode = new EpisodeCursor(cursor);

			//view.setTag(episode.getId());
            ViewHolder holder = (ViewHolder) view.getTag();
            holder.title.setText(episode.getTitle());
            holder.subscription.setText(episode.getSubscriptionTitle());
            holder.thumbnail.setImageBitmap(SubscriptionCursor.getThumbnailImage(getActivity(), episode.getSubscriptionId()));

            int filesizePct = 0;
            if (episode.getFileSize() != null) {
                long downloaded = new File(episode.getFilename(getActivity())).length();
                filesizePct = Math.round(100.0f * downloaded / episode.getFileSize());
            }
            holder.downloaded.setText(String.valueOf(filesizePct) + "% downloaded");

            if (filesizePct != 100) {
                // make sure list is refreshed to update downloading files
                _handler.removeCallbacks(_refresher);
                _handler.postDelayed(_refresher, 1000);
            }
		}

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
	}
}
