package com.axelby.podax.ui;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class DataBoundViewHolder extends RecyclerView.ViewHolder {
	public final ViewDataBinding binding;

	public DataBoundViewHolder(View view) {
		super(view);
		binding = DataBindingUtil.bind(view);
	}

	public static DataBoundViewHolder from(ViewGroup parent, @LayoutRes int layoutId) {
		LayoutInflater inflater = LayoutInflater.from(parent.getContext());
		return new DataBoundViewHolder(inflater.inflate(layoutId, parent, false));
	}
}
