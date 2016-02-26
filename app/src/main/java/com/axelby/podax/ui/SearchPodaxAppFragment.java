package com.axelby.podax.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.app.SearchManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.axelby.podax.R;
import com.axelby.podax.podaxapp.PodaxAppClient;
import com.axelby.podax.podaxapp.Podcast;

import java.util.List;

public class SearchPodaxAppFragment extends Fragment
	implements SearchActivity.QueryChangedHandler, LoaderManager.LoaderCallbacks<List<Podcast>> {

	private List<Podcast> _podcasts = null;
	private SearchPodaxAppAdapter _adapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.recyclerview, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		RecyclerView list = (RecyclerView) view.findViewById(R.id.recyclerview);
		list.setLayoutManager(new GridLayoutManager(view.getContext(), 3));
		_adapter = new SearchPodaxAppAdapter();
		list.setAdapter(_adapter);

		getLoaderManager().initLoader(0, getArguments(), this);
	}

	@Override
	public void onQueryChanged(String query) {
		Bundle bundle = new Bundle(1);
		bundle.putString(SearchManager.QUERY, query);
		getLoaderManager().restartLoader(0, bundle, this);
	}

	@Override
	public Loader<List<Podcast>> onCreateLoader(int loader, Bundle bundle) {
		if (getActivity() == null)
			return null;
		return new PodaxAppSearcher(getActivity(), bundle.getString(SearchManager.QUERY));
	}

	@Override
	public void onLoadFinished(Loader<List<Podcast>> loader, List<Podcast> podcasts) {
		setPodcasts(podcasts);
	}

	@Override
	public void onLoaderReset(Loader<List<Podcast>> loader) {
		setPodcasts(null);
	}

	public void setPodcasts(List<Podcast> podcasts) {
		_podcasts = podcasts;
		_adapter.notifyDataSetChanged();
	}

	public static class PodaxAppSearcher extends AsyncTaskLoader<List<Podcast>> {
		private final String _query;
		private List<Podcast> _response;

		public PodaxAppSearcher(Context context, String query) {
			super(context);
			_query = query;
		}

		@Override
		public List<Podcast> loadInBackground() {
			return PodaxAppClient.get(getContext()).search(_query);
		}

		@Override
		protected void onStartLoading() {
			super.onStartLoading();
			if (_response != null)
				deliverResult(_response);
			if (takeContentChanged() || _response == null)
				forceLoad();
		}

		@Override
		protected void onReset() {
			super.onReset();
			_response = null;
		}
	}

	private class SearchPodaxAppAdapter extends RecyclerView.Adapter<SmallSubscriptionView.ViewHolder> {
		@Override
		public SmallSubscriptionView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			return new SmallSubscriptionView(parent.getContext()).getViewHolder();
		}

		@Override
		public int getItemCount() {
			return _podcasts == null ? 0 : _podcasts.size();
		}

		@Override
		public void onBindViewHolder(SmallSubscriptionView.ViewHolder holder, int position) {
			Podcast pod = _podcasts.get(position);
			holder.set(pod.title, pod.imageUrl, pod.rssUrl);
		}
	}
}
