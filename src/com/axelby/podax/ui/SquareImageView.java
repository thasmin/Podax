package com.axelby.podax.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.android.volley.toolbox.NetworkImageView;

public class SquareImageView extends NetworkImageView {

	public SquareImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public SquareImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SquareImageView(Context context) {
		super(context);
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int square = getMeasuredWidth();
		setMeasuredDimension(square, square);
	}
}
