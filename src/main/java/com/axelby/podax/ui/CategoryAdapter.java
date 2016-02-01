package com.axelby.podax.ui;

import android.content.Context;
import android.support.annotation.StringRes;
import android.support.v4.view.PagerAdapter;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;

import rx.functions.Func1;

public class CategoryAdapter extends PagerAdapter {
	private final Context _context;
	private final int[] _titles;
	private final ArrayList<WeakReference<RecyclerView>> _recyclerViews;
	private final Func1<Integer, RecyclerView.Adapter<DataBoundViewHolder>> _getAdapter;

	public CategoryAdapter(Context context, @StringRes int[] titles,
						   Func1<Integer, RecyclerView.Adapter<DataBoundViewHolder>> getAdapter) {
		_context = context;
		_titles = titles;
		_recyclerViews = new ArrayList<>(Collections.nCopies(_titles.length, new WeakReference<>(null)));
		_getAdapter = getAdapter;
	}

	@Override
	public int getCount() {
		return _titles.length;
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		if (_recyclerViews.get(position).get() != null) {
			container.addView(_recyclerViews.get(position).get());
			return _recyclerViews.get(position).get();
		}

		RecyclerView list = new RecyclerView(container.getContext());
		list.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		list.setLayoutManager(new GridLayoutManager(container.getContext(), 3));
		list.setAdapter(_getAdapter.call(position));
		_recyclerViews.set(position, new WeakReference<>(list));
		container.addView(list);
		return list;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		container.removeView((View) object);
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == object;
	}

	@Override
	public CharSequence getPageTitle(int position) {
		return _context.getString(_titles[position]).toUpperCase();
	}
}
