package com.axelby.podax.ui;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

public class WrappingLinearLayoutManager extends LinearLayoutManager {

	public WrappingLinearLayoutManager(Context context, int orientation, boolean reverseLayout)    {
		super(context, orientation, reverseLayout);
	}

	private int[] mMeasuredDimension = new int[2];

	@Override
	public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state,
						  int widthSpec, int heightSpec) {
		final int widthMode = View.MeasureSpec.getMode(widthSpec);
		final int heightMode = View.MeasureSpec.getMode(heightSpec);
		final int widthSize = View.MeasureSpec.getSize(widthSpec);
		final int heightSize = View.MeasureSpec.getSize(heightSpec);
		int width = 0;
		int height = 0;
		for (int i = 0; i < getItemCount(); i++) {
			measureScrapChild(recycler, i, state,
					View.MeasureSpec.makeMeasureSpec(i, View.MeasureSpec.UNSPECIFIED),
					View.MeasureSpec.makeMeasureSpec(i, View.MeasureSpec.UNSPECIFIED),
					mMeasuredDimension);

			if (getOrientation() == HORIZONTAL) {
				width = width + mMeasuredDimension[0];
				if (i == 0) {
					height = Math.max(height, mMeasuredDimension[1]);
				}
			} else {
				height = height + mMeasuredDimension[1];
				if (i == 0) {
					width = Math.max(width, mMeasuredDimension[0]);
				}
			}
		}

		switch (widthMode) {
			case View.MeasureSpec.EXACTLY:
				width = widthSize;
				break;
			case View.MeasureSpec.AT_MOST:
				width = Math.min(width, widthSize);
				break;
			case View.MeasureSpec.UNSPECIFIED:
		}

		switch (heightMode) {
			case View.MeasureSpec.EXACTLY:
				height = heightSize;
				break;
			case View.MeasureSpec.AT_MOST:
				height = Math.min(height, heightSize);
				break;
			case View.MeasureSpec.UNSPECIFIED:
		}

		setMeasuredDimension(width, height);
	}

	private void measureScrapChild(RecyclerView.Recycler recycler, int position, RecyclerView.State state,
								   int widthSpec, int heightSpec, int[] measuredDimension) {
		if (state.getItemCount() <= position)
			return;

		View view = recycler.getViewForPosition(position);

		// For adding Item Decor Insets to view
		super.measureChildWithMargins(view, 0, 0);

		RecyclerView.LayoutParams p = (RecyclerView.LayoutParams) view.getLayoutParams();
		int childWidthSpec = ViewGroup.getChildMeasureSpec(widthSpec,
				getPaddingLeft() + getPaddingRight() + getDecoratedLeft(view) + getDecoratedRight(view), p.width);
		int childHeightSpec = ViewGroup.getChildMeasureSpec(heightSpec,
				getPaddingTop() + getPaddingBottom() + getPaddingBottom() + getDecoratedBottom(view), p.height);
		view.measure(childWidthSpec, childHeightSpec);

		// Get decorated measurements
		measuredDimension[0] = getDecoratedMeasuredWidth(view) + p.leftMargin + p.rightMargin;
		measuredDimension[1] = getDecoratedMeasuredHeight(view) + p.bottomMargin + p.topMargin;
		recycler.recycleView(view);
	}
}
