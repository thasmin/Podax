package com.axelby.podax.podcastlist;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ViewGroup;

import com.axelby.podax.BR;
import com.axelby.podax.R;
import com.axelby.podax.ui.DataBoundViewHolder;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

public class ListAdapter extends RecyclerView.Adapter<DataBoundViewHolder>  {
	private List<ItemModel> _podcasts = new ArrayList<>(0);

	public ListAdapter(Observable<List<ItemModel>> models) {
		setHasStableIds(true);

		models
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(
				podcasts -> {
					_podcasts = podcasts;
					notifyDataSetChanged();
				},
				e -> Log.e("podaxappadapter", "error while retrieving network", e)
			);
	}

	@Override
	public DataBoundViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		return DataBoundViewHolder.from(parent, R.layout.subscription_list_item);
	}

	@Override
	public void onBindViewHolder(DataBoundViewHolder holder, int position) {
		holder.binding.setVariable(BR.model, _podcasts.get(position));
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getItemCount() {
		return _podcasts.size();
	}

}
