package com.axelby.podax.ui;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.EpisodeData;
import com.axelby.podax.EpisodeProvider;
import com.axelby.podax.PlayerService;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
import com.trello.rxlifecycle.components.RxFragment;

import java.text.DateFormat;
import java.util.List;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.BehaviorSubject;

public class FinishedEpisodeFragment extends RxFragment {
	private PodcastAdapter _adapter = null;
	private RecyclerView _listView;
	private View _emptyView;

	private BehaviorSubject<EpisodeData> _timing = BehaviorSubject.create();

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		setHasOptionsMenu(true);

		_adapter = new PodcastAdapter();
		EpisodeData.getObservables(activity, EpisodeData.FINISHED)
			.concatWith(_timing)
			.toList()
			.compose(bindToLifecycle())
			.subscribe(
				_adapter::setEpisodes,
				e -> Log.e("FinishedEpisodeFragment", "error while retrieving finished episodes", e)
			);
	}

	@Override
	public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_finished_episodes, container, false);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		_listView = (RecyclerView) view.findViewById(R.id.list);
		_listView.setLayoutManager(new LinearLayoutManager(getActivity()));
		_listView.setItemAnimator(new DefaultItemAnimator());

		_emptyView = view.findViewById(R.id.empty);

		_timing.onCompleted();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		_listView.setAdapter(_adapter);
	}

	private class PodcastAdapter extends RecyclerView.Adapter<PodcastAdapter.ViewHolder> {

        private final DateFormat _finishedDateFormat = DateFormat.getDateInstance();

		private List<EpisodeData> _episodes;
		private TreeMap<Long, Integer> _ids = new TreeMap<>();
		private final Subscriber<EpisodeData> _episodeSubscriber = new Subscriber<EpisodeData>() {
			@Override public void onCompleted() { }

			@Override
			public void onError(Throwable e) {
				Log.e("FinishedEpisodeFragment", "error while updating episode", e);
			}

			@Override
			public void onNext(EpisodeData episodeData) {
				updateEpisode(episodeData);
			}
		};

		class ViewHolder extends RecyclerView.ViewHolder {
			public final View container;
			public final ImageView thumbnail;
			public final TextView subscriptionTitle;
            public final TextView title;
            public final TextView date;
            public final TextView play;
            public final TextView playlist;

            public ViewHolder (View view) {
				super(view);

				container = view;
                title = (TextView) view.findViewById(R.id.title);
                date = (TextView) view.findViewById(R.id.date);
                play = (TextView) view.findViewById(R.id.play);
                playlist = (TextView) view.findViewById(R.id.playlist);
				thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
				subscriptionTitle = (TextView) view.findViewById(R.id.subscription_title);

				play.setOnClickListener(_playHandler);
				playlist.setOnClickListener(_playlistHandler);
				view.setOnClickListener(_clickHandler);
            }
        }

		public PodcastAdapter() {
			setHasStableIds(true);
        }

		public void setEpisodes(List<EpisodeData> episodes) {
			_episodes = episodes;
			notifyDataSetChanged();

			_ids.clear();
			for (int i = 0; i < _episodes.size(); ++i)
				_ids.put(_episodes.get(i).getId(), i);

			_episodeSubscriber.unsubscribe();
			EpisodeData.getEpisodeWatcher()
				.compose(bindToLifecycle())
				.subscribe(_episodeSubscriber);

			boolean isEmpty = episodes.size() == 0;
			_emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
			_listView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
		}

		public void updateEpisode(EpisodeData episode) {
			Integer position = _ids.get(episode.getId());
			if (position == null)
				return;
			_episodes.set(position, episode);
			notifyItemChanged(position);
		}

        final View.OnClickListener _playHandler = view -> {
			long episodeId = (Long) view.getTag();
			PlayerService.play(view.getContext(), episodeId);

			// put podcast on top of playlist
			ContentValues values = new ContentValues(1);
			values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, 0);
			view.getContext().getContentResolver().update(EpisodeProvider.getContentUri(episodeId), values, null, null);

			startActivity(PodaxFragmentActivity.createIntent(getActivity(), EpisodeDetailFragment.class, Constants.EXTRA_EPISODE_ID, episodeId));
		};

        final View.OnClickListener _playlistHandler = view -> {
			long episodeId = (Long) view.getTag(R.id.episodeId);
			Integer position = (Integer) view.getTag(R.id.playlist);

			ContentValues values = new ContentValues(1);
			values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, position);
			view.getContext().getContentResolver().update(EpisodeProvider.getContentUri(episodeId), values, null, null);
		};

		final View.OnClickListener _clickHandler = view -> {
			long episodeId = (Long) view.getTag();
			startActivity(PodaxFragmentActivity.createIntent(getActivity(), EpisodeDetailFragment.class, Constants.EXTRA_EPISODE_ID, episodeId));
		};

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_finished_episodes_item, parent, false);
			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			if (_episodes == null)
				return;
			Context context = holder.container.getContext();

			EpisodeData episode = _episodes.get(position);

			holder.container.setTag(episode.getId());
			holder.subscriptionTitle.setText(episode.getSubscriptionTitle());
			holder.thumbnail.setImageBitmap(SubscriptionCursor.getThumbnailImage(getActivity(), episode.getSubscriptionId()));
            holder.title.setText(episode.getTitle());
            holder.date.setText(context.getString(R.string.finished_on, _finishedDateFormat.format(episode.getFinishedDate())));
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
            return _episodes.get(position).getId();
        }

		@Override
		public int getItemCount() {
			if (_episodes == null)
				return 0;
			return _episodes.size();
		}
	}
}
