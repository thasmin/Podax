package com.axelby.podax.ui;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.axelby.podax.SubscriptionProvider;

import java.text.DateFormat;

import javax.annotation.Nonnull;

public class EpisodeListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private PodcastAdapter _adapter = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		getLoaderManager().initLoader(0, getArguments(), this);
		_adapter = new PodcastAdapter(getActivity(), null);
		setListAdapter(_adapter);
	}

	@Override
	public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_episodelist, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

        setTitle();
	}

	public void setTitle() {
		if (getView() == null)
			return;

        long subscriptionId = getArguments().getLong(Constants.EXTRA_SUBSCRIPTION_ID, -1);
        if (subscriptionId == -1)
            return;

		Uri subscriptionUri = ContentUris.withAppendedId(SubscriptionProvider.URI, subscriptionId);
		String[] subscriptionProjection = {
				SubscriptionProvider.COLUMN_ID,
				SubscriptionProvider.COLUMN_TITLE,
				SubscriptionProvider.COLUMN_URL,
		};
		Cursor subscriptionCursor = getActivity().getContentResolver().query(subscriptionUri, subscriptionProjection, null, null, null);
		if (subscriptionCursor == null)
			return;

        if (subscriptionCursor.moveToNext()) {
            SubscriptionCursor cursor = new SubscriptionCursor(subscriptionCursor);

            TextView subscription = (TextView) getView().findViewById(R.id.subscription);
            subscription.setText(cursor.getTitle());

            ImageView thumbnail = (ImageView) getView().findViewById(R.id.thumbnail);
            thumbnail.setImageBitmap(SubscriptionCursor.getThumbnailImage(getActivity(), subscriptionId));
        }

        subscriptionCursor.close();
	}

	@Override
	public void onListItemClick(ListView list, View view, int position, long id) {
        Helper.changeFragment(getActivity(), EpisodeDetailFragment.class, Constants.EXTRA_EPISODE_ID, id);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Uri uri = ContentUris.withAppendedId(SubscriptionProvider.URI, args.getLong(Constants.EXTRA_SUBSCRIPTION_ID));
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
		if (getActivity() == null)
			return;

		_adapter.changeCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		_adapter.changeCursor(null);
	}

	private class PodcastAdapter extends ResourceCursorAdapter {

        private final DateFormat _pubDateFormat = DateFormat.getDateInstance();

        class ViewHolder {
            public TextView title;
            public TextView date;
            public TextView duration;
            public Button play;
            public Button playlist;

            public ViewHolder (View view) {
                title = (TextView) view.findViewById(R.id.title);
                date = (TextView) view.findViewById(R.id.date);
                duration = (TextView) view.findViewById(R.id.duration);
                play = (Button) view.findViewById(R.id.play);
                playlist = (Button) view.findViewById(R.id.playlist);
            }
        }

		public PodcastAdapter(Context context, Cursor cursor) {
			super(context, R.layout.fragment_episoidelist_item, cursor, true);
        }

        OnClickListener _playListener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                long episodeId = (Long) view.getTag();
                PlayerService.play(view.getContext(), episodeId);
                Helper.changeFragment(getActivity(), EpisodeDetailFragment.class, Constants.EXTRA_EPISODE_ID, episodeId);
            }
        };

        OnClickListener _playlistListener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                long episodeId = (Long) view.getTag(R.id.episode);
                Integer position = (Integer) view.getTag(R.id.playlist);

                ContentValues values = new ContentValues(1);
                values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, position);
                view.getContext().getContentResolver().update(EpisodeProvider.getContentUri(episodeId), values, null, null);
            }
        };

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = super.newView(context, cursor, parent);
            ViewHolder holder = new ViewHolder(view);
            holder.play.setOnClickListener(_playListener);
            holder.playlist.setOnClickListener(_playlistListener);
            view.setTag(holder);
            return view;
        }

        @Override
        public long getItemId(int position) {
            return new EpisodeCursor((Cursor)getItem(position)).getId();
        }

        @Override
		public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder holder = (ViewHolder) view.getTag();
            EpisodeCursor episode = new EpisodeCursor(cursor);
            holder.title.setText(episode.getTitle());
            holder.date.setText(context.getString(R.string.released_on) + " " + _pubDateFormat.format(episode.getPubDate()));
            if (episode.getDuration() > 0) {
                holder.duration.setText(Helper.getVerboseTimeString(context, episode.getDuration() / 1000f) + " " + context.getString(R.string.in_duration));
                holder.duration.setVisibility(View.VISIBLE);
            } else
                holder.duration.setVisibility(View.GONE);
            holder.play.setTag(episode.getId());
            holder.playlist.setTag(R.id.episode, episode.getId());
            Integer position = episode.getPlaylistPosition();
            if (position == null) {
                holder.playlist.setTag(R.id.playlist, Integer.MAX_VALUE);
                holder.playlist.setText(R.string.add_to_playlist);
                holder.play.setVisibility(View.GONE);
            } else {
                holder.playlist.setTag(R.id.playlist, null);
                holder.playlist.setText(R.string.remove_from_playlist);
                holder.play.setVisibility(View.VISIBLE);
            }
		}
	}
}
