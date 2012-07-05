package com.axelby.podax.ui;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class FreezableViewPager extends ViewPager {

	public boolean _frozen;

	public FreezableViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		_frozen = false;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (!_frozen)
			return super.onInterceptTouchEvent(event);
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!_frozen)
			return super.onTouchEvent(event);
		return false;
	}

	public void freeze() {
		_frozen = true;
	}

	public void unfreeze() {
		_frozen = false;
	}
}
