package com.axelby.podax.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.LinearLayout;

public class CheckedLinearLayout extends LinearLayout implements Checkable {

	boolean _isChecked = false;
	Drawable _background = null;

	public CheckedLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		_background = this.getBackground();
		this.setBackgroundDrawable(null);
	}

	@Override
	public boolean isChecked() {
		return _isChecked;
	}

	@Override
	public void setChecked(boolean isChecked) {
		_isChecked = isChecked;
		this.setBackgroundDrawable(_isChecked ? _background : null);
	}

	@Override
	public void toggle() {
		_isChecked = !_isChecked;
		this.setBackgroundDrawable(_isChecked ? _background : null);
	}

}
