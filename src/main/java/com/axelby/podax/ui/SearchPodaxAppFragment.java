package com.axelby.podax.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.app.SearchManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.axelby.podax.R;
import com.axelby.podax.podaxapp.PodaxAppClient;
import com.axelby.podax.podaxapp.Podcast;
import com.squareup.picasso.Picasso;

import java.util.List;

public class SearchPodaxAppFragment extends Fragment
	implements SearchActivity.QueryChangedHandler, LoaderManager.LoaderCallbacks<List<Podcast>> {

	private SearchPodaxAppAdapter _adapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.recyclerview, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		RecyclerView list = (RecyclerView) view.findViewById(R.id.recyclerview);
		list.setLayoutManager(new LinearLayoutManager(view.getContext()));
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
	public void onLoadFinished(Loader<List<Podcast>> listLoader, List<Podcast> podcasts) {
		_adapter.setData(podcasts);
	}

	@Override
	public void onLoaderReset(Loader<List<Podcast>> listLoader) {
		_adapter.setData(null);
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

	private class SearchPodaxAppAdapter extends RecyclerView.Adapter<SearchPodaxAppAdapter.PodaxAppViewHolder> {
		private List<Podcast> _podcasts = null;

		public class PodaxAppViewHolder extends RecyclerView.ViewHolder {
			public SearchPodaxAppTextView text;
			public PodaxAppViewHolder(View view) {
				super(view);
				text = (SearchPodaxAppTextView) view;
			}
		}

		public void setData(List<Podcast> podcasts) {
			_podcasts = podcasts;
			notifyDataSetChanged();
		}

		@Override
		public PodaxAppViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_podaxapp_item, parent, false);
			return new PodaxAppViewHolder(view);
		}

		@Override
		public int getItemCount() {
			return _podcasts == null ? 0 : _podcasts.size();
		}

		@Override
		public void onBindViewHolder(PodaxAppViewHolder holder, int position) {
			Podcast pod = _podcasts.get(position);
			holder.text.setText(pod.title);
			Picasso.with(holder.text.getContext()).load(pod.imageUrl).into(holder.text);
		}
	}
}
