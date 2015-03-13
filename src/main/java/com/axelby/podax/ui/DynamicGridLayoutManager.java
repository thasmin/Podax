package com.axelby.podax.ui;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

public class DynamicGridLayoutManager extends GridLayoutManager {
	public DynamicGridLayoutManager(Context context, int spanCount) {
		super(context, spanCount);
	}

	private int[] _measuredDimension = new int[2];

	@Override
	public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state,
						  int widthSpec, int heightSpec) {
		final int widthMode = View.MeasureSpec.getMode(widthSpec);
		final int heightMode = View.MeasureSpec.getMode(heightSpec);
		final int widthSize = View.MeasureSpec.getSize(widthSpec);
		final int heightSize = View.MeasureSpec.getSize(heightSpec);
		int width = 0;
		int height = 0;

		if (getItemCount() > 0) {
			// assume all children are same size
			measureScrapChild(recycler, 0, widthSpec, heightSpec, _measuredDimension);
			// assume vertical orientation
			int rows = (int) Math.ceil((float)getItemCount() / getSpanCount());
			width = getSpanCount() * _measuredDimension[0];
			height = rows * _measuredDimension[1];
		}

		switch (widthMode) {
			case View.MeasureSpec.EXACTLY:
				width = widthSize;
			case View.MeasureSpec.AT_MOST:
			case View.MeasureSpec.UNSPECIFIED:
		}

		switch (heightMode) {
			case View.MeasureSpec.EXACTLY:
				height = heightSize;
			case View.MeasureSpec.AT_MOST:
			case View.MeasureSpec.UNSPECIFIED:
		}

		setMeasuredDimension(width, height);
	}

	private void measureScrapChild(RecyclerView.Recycler recycler, int position, int widthSpec,
								   int heightSpec, int[] measuredDimension) {
		View view = recycler.getViewForPosition(position);
		if (view == null)
			return;

		RecyclerView.LayoutParams p = (RecyclerView.LayoutParams) view.getLayoutParams();
		int childWidthSpec = ViewGroup.getChildMeasureSpec(widthSpec,
				getPaddingLeft() + getPaddingRight() + getLeftDecorationWidth(view) + getRightDecorationWidth(view), p.width);
		int childHeightSpec = ViewGroup.getChildMeasureSpec(heightSpec,
				getPaddingTop() + getPaddingBottom() + getTopDecorationHeight(view) + getBottomDecorationHeight(view), p.height);
		view.measure(childWidthSpec, childHeightSpec);
		measuredDimension[0] = view.getMeasuredWidth();
		measuredDimension[1] = view.getMeasuredHeight();
		recycler.recycleView(view);
	}
}
