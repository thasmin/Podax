package com.axelby.podax.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.axelby.podax.BR;
import com.axelby.podax.model.EpisodeData;
import com.axelby.podax.model.EpisodeDB;
import com.axelby.podax.R;
import com.trello.rxlifecycle.components.RxFragment;

import java.util.List;

import javax.annotation.Nonnull;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class FinishedEpisodeFragment extends RxFragment {
	private PodcastAdapter _adapter = null;
	private RecyclerView _listView;
	private View _emptyView;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		setHasOptionsMenu(true);

		_adapter = new PodcastAdapter();
		EpisodeDB.getFinished()
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
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

		_emptyView = view.findViewById(R.id.empty);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		_listView.setAdapter(_adapter);
	}

	private class PodcastAdapter extends RecyclerView.Adapter<DataBoundViewHolder> {

		private List<EpisodeData> _episodes;

		public PodcastAdapter() {
			setHasStableIds(true);
        }

		public void setEpisodes(List<EpisodeData> episodes) {
			_episodes = episodes;
			notifyDataSetChanged();

			EpisodeDB.getEpisodeWatcher()
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.compose(bindToLifecycle())
				.subscribe(
					this::updateEpisode,
					e -> Log.e("FinishedEpisodeFragment", "error while updating episode", e)
				);

			boolean isEmpty = episodes.size() == 0;
			_emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
			_listView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
		}

		public void updateEpisode(EpisodeData episode) {
			for (int i = 0; i <= _episodes.size(); ++i) {
				if (_episodes.get(i).getId() == episode.getId()) {
					_episodes.set(i, episode);
					notifyItemChanged(i, episode);
					return;
				}
			}
		}

		@Override
		public DataBoundViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			return DataBoundViewHolder.from(parent, R.layout.fragment_finished_episodes_item);
		}

		@Override
		public void onBindViewHolder(DataBoundViewHolder holder, int position) {
			holder.binding.setVariable(BR.episode, _episodes.get(position));
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
