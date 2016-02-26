package com.axelby.podax.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.axelby.podax.R;

public class AddSubscriptionFrame extends LinearLayout {
	Drawable _headerIcon = null;

	public AddSubscriptionFrame(Context context) {
		this(context, null);
	}

	public AddSubscriptionFrame(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AddSubscriptionFrame(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		// should have add_subscription_box background
		if (getBackground() == null)
			setBackgroundResource(R.drawable.add_subscription_box);

		TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.AddSubscriptionFrame, defStyleAttr, 0);
		_headerIcon = a.getDrawable(R.styleable.AddSubscriptionFrame_headerIcon);
		a.recycle();
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		// should have padding of 100dp
		if (changed) {
			int topPadding = (int) (100 * getResources().getDisplayMetrics().density);
			setPadding(getPaddingLeft(), topPadding, getPaddingRight(), getPaddingBottom());
		}

		super.onLayout(changed, left, top, right, bottom);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int topPadding = (int) (100 * getResources().getDisplayMetrics().density);
		setPadding(getPaddingLeft(), topPadding, getPaddingRight(), getPaddingBottom());

		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (_headerIcon != null) {
			int top = (int) (28 * getResources().getDisplayMetrics().density);
			int left = (getWidth() - _headerIcon.getIntrinsicWidth()) / 2;
			_headerIcon.setBounds(left, top, left + _headerIcon.getIntrinsicWidth(), top + _headerIcon.getIntrinsicHeight());
			_headerIcon.draw(canvas);
		}
	}
}
