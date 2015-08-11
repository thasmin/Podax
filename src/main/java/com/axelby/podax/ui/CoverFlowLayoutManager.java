package com.axelby.podax.ui;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

public class CoverFlowLayoutManager extends RecyclerView.LayoutManager {
	// measuring and layout needs to be done on all child views
	private int _firstPosition = 0;
	private int _selectedChild = 0;

	public interface SelectedChildChangedHandler {
		void onSelectedChildChanged(int position);
	}
	private SelectedChildChangedHandler _selectedChildChangedHandler;
	public void setOnSelectedChildChanged(SelectedChildChangedHandler handler) {
		_selectedChildChangedHandler = handler;
	}

	@Override
	public RecyclerView.LayoutParams generateDefaultLayoutParams() {
		return new RecyclerView.LayoutParams(
			RecyclerView.LayoutParams.MATCH_PARENT,
			RecyclerView.LayoutParams.WRAP_CONTENT
		);
	}

	@Override
	public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state,
						  int widthSpec, int heightSpec) {
		final int widthMode = View.MeasureSpec.getMode(widthSpec);
		final int heightMode = View.MeasureSpec.getMode(heightSpec);
		final int widthSize = View.MeasureSpec.getSize(widthSpec);
		final int heightSize = View.MeasureSpec.getSize(heightSpec);
		int width = 0;
		int height = 0;

		// ignore exactly - wrap contents is giving exactly for some reason
		if (widthMode == View.MeasureSpec.EXACTLY && heightMode == View.MeasureSpec.EXACTLY) {
			setMeasuredDimension(widthSize, heightSize);
			return;
		}

		int[] size = new int[2];
		calculateSize(recycler, widthSpec, heightSpec, size);

		switch (widthMode) {
			case View.MeasureSpec.EXACTLY:
				width = widthSize;
				break;
			case View.MeasureSpec.AT_MOST:
				width = Math.min(widthSize, size[0]);
				break;
			case View.MeasureSpec.UNSPECIFIED:
				width = size[0];
				break;
		}

		switch (heightMode) {
			case View.MeasureSpec.EXACTLY:
				height = heightSize;
				break;
			case View.MeasureSpec.AT_MOST:
				height = Math.min(heightSize, size[1]);
				break;
			case View.MeasureSpec.UNSPECIFIED:
				height = size[1];
				break;
		}

		setMeasuredDimension(width, height);
	}

	private void calculateSize(RecyclerView.Recycler recycler, int widthSpec, int heightSpec, int[] size) {
		for (int i = 0; i < getItemCount(); ++i) {
			View view = recycler.getViewForPosition(i);

			RecyclerView.LayoutParams p = (RecyclerView.LayoutParams) view.getLayoutParams();
			int childWidthSpec = ViewGroup.getChildMeasureSpec(widthSpec,
					getPaddingLeft() + getPaddingRight() + getLeftDecorationWidth(view) + getRightDecorationWidth(view), p.width);
			int childHeightSpec = ViewGroup.getChildMeasureSpec(heightSpec,
					getPaddingTop() + getPaddingBottom() + getTopDecorationHeight(view) + getBottomDecorationHeight(view), p.height);
			view.measure(childWidthSpec, childHeightSpec);

			size[0] = Math.max(size[0], getDecoratedMeasuredWidth(view));
			size[1] = Math.max(size[1], getDecoratedMeasuredHeight(view));
			removeAndRecycleView(view, recycler);
		}
	}

	@Override
	public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
		if (getItemCount() == 0) {
			removeAndRecycleAllViews(recycler);
			return;
		}

		detachAndScrapAttachedViews(recycler);

		View sel = recycler.getViewForPosition(_selectedChild);
		addView(sel);
		measureChildWithMargins(sel, 0, 0);
		int left = getWidth() / 2 - getDecoratedMeasuredWidth(sel) / 2;
		int right = left + getDecoratedMeasuredWidth(sel);
		layoutDecorated(sel, left, 0, right, getDecoratedMeasuredHeight(sel));

		drawLeftChildren(recycler, left);
		drawRightChildren(recycler, right);
		rotateChildren();
	}

	private void drawLeftChildren(RecyclerView.Recycler recycler, int initialRight) {
		int left = initialRight;
		for (int i = _selectedChild - 1; i >= 0; --i) {
			left = addChildOnLeft(recycler, left, i, 0.75f);
			if (left <= 0)
				break;
		}
	}

	private int addChildOnLeft(RecyclerView.Recycler recycler, int right, int position, float scale) {
		View v = recycler.getViewForPosition(position);
		addView(v, 0);
		measureChildWithMargins(v, 0, 0);

		int top = (int) ((1 - scale) / 2 * getDecoratedMeasuredHeight(v));
		int bottom = (int) (top + getDecoratedMeasuredHeight(v) * scale);
		int left = (int) (right - getDecoratedMeasuredWidth(v) * scale);

		layoutDecorated(v, left, top, right, bottom);
		return left;
	}

	private void drawRightChildren(RecyclerView.Recycler recycler, int initialLeft) {
		int right = initialLeft;
		for (int i = _selectedChild + 1; i < getItemCount(); ++i) {
			right = addChildOnRight(recycler, right, i, 0.75f);
			if (right >= getWidth())
				break;
		}
	}

	private int addChildOnRight(RecyclerView.Recycler recycler, int left, int position, float scale) {
		View v = recycler.getViewForPosition(position);
		addView(v);
		measureChildWithMargins(v, 0, 0);

		int top = (int) ((1 - scale) / 2 * getDecoratedMeasuredHeight(v));
		int bottom = (int) (top + getDecoratedMeasuredHeight(v) * scale);
		int right = (int) (left + getDecoratedMeasuredWidth(v) * scale);

		layoutDecorated(v, left, top, right, bottom);
		return right;
	}

	@Override
	public boolean canScrollHorizontally() { return true; }

	@Override
	public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
		if (getChildCount() == 0)
			return 0;

		//todo: Optimize the case where the entire data set is too small to scroll

		// check bounds
		int centeredPosition = determinePositionInCenter();
		int delta = -dx;
		if (dx < 0 && centeredPosition == 0)
			delta = Math.min(delta, distanceFromCenter(centeredPosition));
		if (dx > 0 && centeredPosition == getItemCount() - 1)
			delta = Math.max(delta, -distanceFromCenter(centeredPosition));
		if (delta == 0)
			return 0;

		// add children that were off screen
		if (dx < 0) {
			View leftest = getChildAt(0);
			while (getDecoratedLeft(leftest) > 0 && _firstPosition > 0) {
				_firstPosition--;
				addChildOnLeft(recycler, getDecoratedLeft(leftest), _firstPosition, 0.75f);
				Log.d("CoverFlowLayoutManager", "adding new view on left from position: " + _firstPosition);
				leftest = getChildAt(0);
			}
		} else {
			View rightest = getChildAt(getChildCount() - 1);
			while (getDecoratedRight(rightest) < getWidth() && _firstPosition + getChildCount() < getItemCount()) {
				addChildOnRight(recycler, getDecoratedRight(rightest), _firstPosition + getChildCount(), 0.75f);
				Log.d("CoverFlowLayoutManager", "adding new view on right from position: " + _firstPosition + getChildCount());
				rightest = getChildAt(getChildCount() - 1);
			}
		}

		// remove children that are now off screen
		if (dx < 0) {
			while (getDecoratedLeft(getChildAt(getChildCount() - 1)) > getWidth()) {
				Log.d("CoverFlowLayoutManager", "removing child: " + (getChildCount() - 1));
				removeAndRecycleViewAt(getChildCount() - 1, recycler);
			}
		} else {
			while (getDecoratedRight(getChildAt(0)) < 0) {
				_firstPosition++;
				removeAndRecycleViewAt(0, recycler);
			}
		}

		offsetChildrenHorizontal(delta);

		centeredPosition = determinePositionInCenter();
		if (centeredPosition != -1 && centeredPosition != _selectedChild) {
			float scale = 0.75f;

			View old = getChildAtPosition(_selectedChild);
			int oldTop = (int) ((1 - scale) / 2 * getDecoratedMeasuredHeight(old));
			int oldBottom = (int) (oldTop + getDecoratedMeasuredHeight(old) * scale);

			View sel = getChildAtPosition(centeredPosition);
			int selTop = 0;
			int selBottom = getDecoratedMeasuredHeight(sel);

			if (dx > 0) {
				int left = getDecoratedLeft(old);
				int right = (int) (left + getDecoratedMeasuredHeight(old) * scale);
				layoutDecorated(old, left, oldTop, right, oldBottom);

				left = right;
				right = left + getDecoratedMeasuredWidth(sel);
				layoutDecorated(sel, left, selTop, right, selBottom);
			} else {
				int right = getDecoratedRight(old);
				int left = (int) (right - getDecoratedMeasuredHeight(old) * scale);
				layoutDecorated(old, left, oldTop, right, oldBottom);

				right = left;
				left = right - getDecoratedMeasuredWidth(sel);
				layoutDecorated(sel, left, selTop, right, selBottom);
			}

			Log.d("CoverFlowLayoutManager", "centered view position: " + centeredPosition);
			_selectedChild = centeredPosition;
			if (_selectedChildChangedHandler != null)
				_selectedChildChangedHandler.onSelectedChildChanged(centeredPosition);
		}

		rotateChildren();

		return -delta;
	}

	private void rotateChildren() {
		// don't rotate children for now
		/*
		for (int i = 0; i < getChildCount(); ++i) {
			if (i + _firstPosition < _selectedChild)
				getChildAt(i).setRotationY(30);
			else if (i + _firstPosition == _selectedChild)
				getChildAt(i).setRotationY(0);
			else
				getChildAt(i).setRotationY(-30);
		}
		*/
	}

	protected View getChildAtPosition(int position) {
		return getChildAt(position - _firstPosition);
	}

	private int distanceFromCenter(int child) {
		int listCenter = getWidth() / 2;
		View v = getChildAtPosition(child);
		int viewCenter = (getDecoratedLeft(v) + getDecoratedRight(v)) / 2;
		return Math.abs(listCenter - viewCenter);
	}

	private int determinePositionInCenter() {
		int center = getWidth() / 2;
		// todo: binary search
		for (int i = 0; i < getChildCount(); ++i) {
			View v = getChildAt(i);
			if (getDecoratedLeft(v) <= center && getDecoratedRight(v) >= center)
				return i + _firstPosition;
		}
		return -1;
	}
}
