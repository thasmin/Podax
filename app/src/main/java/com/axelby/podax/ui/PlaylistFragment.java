package com.axelby.podax.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.axelby.podax.BR;
import com.axelby.podax.EpisodeDownloadService;
import com.axelby.podax.Helper;
import com.axelby.podax.R;
import com.axelby.podax.databinding.PlaylistListItemBinding;
import com.axelby.podax.model.EpisodeDB;
import com.axelby.podax.model.EpisodeData;
import com.axelby.podax.model.PodaxDB;
import com.axelby.podax.model.SubscriptionData;
import com.trello.rxlifecycle.components.RxFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class PlaylistFragment extends RxFragment {
	private RecyclerView _listView;
	PlaylistListAdapter _adapter;

	ItemTouchHelper _itemTouchHelper = new ItemTouchHelper(
		new ItemTouchHelper.SimpleCallback(
				ItemTouchHelper.UP   | ItemTouchHelper.DOWN,
				ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
			@Override
			public boolean onMove(RecyclerView recyclerView,
								  RecyclerView.ViewHolder viewHolder,
								  RecyclerView.ViewHolder target) {
				int fromPos = viewHolder.getAdapterPosition();
				int toPos = target.getAdapterPosition();
				_adapter.moveItemByPosition(fromPos, toPos);

				return true;
			}

			@Override
			public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
				_adapter.removeFromPosition(viewHolder.getAdapterPosition());
			}

			@Override
			public boolean isLongPressDragEnabled() {
				return false;
			}
		});

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		setHasOptionsMenu(true);

		_adapter = new PlaylistListAdapter();
		PodaxDB.episodes.watchPlaylist()
			.compose(bindToLifecycle())
			.subscribe(
				_adapter::setEpisodes,
				e -> Log.e("PlaylistFragment", "unable to retrieve episodes", e)
			);
	}

	@Override
	public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.playlist, container, false);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		_listView = (RecyclerView) view.findViewById(R.id.list);
		_listView.setLayoutManager(new LinearLayoutManager(getActivity()));
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		_listView.setAdapter(_adapter);
		_itemTouchHelper.attachToRecyclerView(_listView);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.playlist_fragment, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.download) {
			EpisodeDownloadService.downloadEpisodes(getActivity());
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private class PlaylistListAdapter extends RecyclerView.Adapter<DataBoundViewHolder> {

		private List<EpisodeData> _episodes = new ArrayList<>(0);
		private TreeMap<Long, Integer> _ids = new TreeMap<>();
		private final Subscriber<EpisodeDB.EpisodeChange> _episodeSubscriber = new Subscriber<EpisodeDB.EpisodeChange>() {
			@Override public void onCompleted() { }

			@Override
			public void onError(Throwable e) {
				Log.e("FinishedEpisodeFragment", "error while updating episode", e);
			}

			@Override
			public void onNext(EpisodeDB.EpisodeChange change) {
				updateEpisode(change);
			}
		};

		PlaylistListAdapter() {
			setHasStableIds(true);
		}

		public void setEpisodes(List<EpisodeData> episodes) {
			_episodes = episodes;
			notifyDataSetChanged();

			_ids.clear();
			for (int i = 0; i < _episodes.size(); ++i)
				_ids.put(_episodes.get(i).getId(), i);

			_episodeSubscriber.unsubscribe();
			PodaxDB.episodes.watchAll()
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.filter(ep -> _ids.containsKey(ep.getId()))
				.compose(bindToLifecycle())
				.subscribe(_episodeSubscriber);
		}

		void updateEpisode(EpisodeDB.EpisodeChange change) {
			Integer position = _ids.get(change.getId());
			if (position == null)
				return;
			if (change.getNewData() == null) {
				_episodes.remove((int)position);
				notifyItemRemoved(position);
			} else {
				_episodes.set(position, change.getNewData());
				notifyItemChanged(position, change.getNewData());
			}
		}

		@Override
		public DataBoundViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());
			return new DataBoundViewHolder(inflater.inflate(R.layout.playlist_list_item, parent, false));
		}

        @Override
		public void onBindViewHolder(DataBoundViewHolder holder, int position) {
			EpisodeData episode = _episodes.get(position);
			holder.binding.setVariable(BR.episode, episode);

			PlaylistListItemBinding binding = (PlaylistListItemBinding) holder.binding;
			binding.drag.setOnTouchListener((v, event) -> {
				if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN)
					_itemTouchHelper.startDrag(holder);
				return false;
			});

			Palette.Swatch swatch = SubscriptionData.getThumbnailSwatch(episode.getSubscriptionId());
			if (swatch != null) {
				binding.card.setCardBackgroundColor(swatch.getRgb());
				binding.title.setTextColor(swatch.getBodyTextColor());
				binding.duration.setTextColor(swatch.getBodyTextColor());
				binding.play.setTextColor(swatch.getTitleTextColor());
				binding.remove.setTextColor(swatch.getTitleTextColor());
				binding.drag.setColorFilter(swatch.getTitleTextColor());
			} else {
				binding.card.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.cardBG));
				binding.title.setTextColor(0x99ffffff);
				binding.duration.setTextColor(0x99ffffff);
				binding.play.setTextColor(Helper.getAttributeColor(getContext(), R.attr.colorPrimary));
				binding.remove.setTextColor(Helper.getAttributeColor(getContext(), R.attr.colorPrimary));
				binding.drag.setColorFilter(null);
			}
		}

		@Override
		public int getItemCount() {
			return _episodes.size();
		}

		@Override
		public long getItemId(int position) {
			return _episodes.get(position).getId();
		}

		void moveItemByPosition(int from, int to) {
			_episodes.get(from).moveToPlaylistPosition(to);
			notifyItemMoved(from, to);
		}

		void removeFromPosition(int position) {
			_episodes.get(position).removeFromPlaylist();
			notifyItemRemoved(position);
		}
	}
}
