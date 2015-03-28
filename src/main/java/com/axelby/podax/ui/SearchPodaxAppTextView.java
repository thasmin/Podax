package com.axelby.podax.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.TextView;

import com.axelby.podax.R;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class SearchPodaxAppTextView extends TextView implements Target {
	public SearchPodaxAppTextView(Context context) {
		super(context);
	}

	public SearchPodaxAppTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SearchPodaxAppTextView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
		float density = getResources().getDisplayMetrics().density;
		int px = (int) (50 * density);
		Bitmap scaled = Bitmap.createScaledBitmap(bitmap, px, px, true);
		setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(getResources(), scaled), null, null, null);
	}

	@Override
	public void onBitmapFailed(Drawable errorDrawable) {
		float density = getResources().getDisplayMetrics().density;
		int px = (int) (50 * density);
		Bitmap podax = BitmapFactory.decodeResource(getResources(), R.drawable.ic_menu_podax);
		Bitmap scaled = Bitmap.createScaledBitmap(podax, px, px, true);
		setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(getResources(), scaled), null, null, null);
	}

	@Override
	public void onPrepareLoad(Drawable placeHolderDrawable) {

	}
}
