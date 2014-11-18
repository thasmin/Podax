package com.axelby.podax;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.View;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

public class URLImageGetter implements Html.ImageGetter {
	View _container;

	public URLImageGetter(View t) {
		this._container = t;
	}

	public Drawable getDrawable(String source) {
		final DelayedDrawable drawable = new DelayedDrawable(_container.getResources());

		Helper.getImageLoader(_container.getContext()).get(source, new ImageLoader.ImageListener() {
			@Override
			public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
				Bitmap bitmap = imageContainer.getBitmap();
				if (bitmap == null || _container.getResources() == null)
					return;

				BitmapDrawable result = new BitmapDrawable(_container.getResources(), bitmap);
				DisplayMetrics metrics = _container.getResources().getDisplayMetrics();
				result.setBounds(0, 0, bitmap.getScaledWidth(metrics), bitmap.getScaledHeight(metrics));
				drawable.setDrawable(result);

				_container.invalidate();
			}

			@Override
			public void onErrorResponse(VolleyError volleyError) {
			}
		});

		return drawable;
	}

	public class DelayedDrawable extends BitmapDrawable {
		private Drawable _drawable;

        public DelayedDrawable(Resources resources) {
            super(resources, (Bitmap) null);
        }

		public void setDrawable(Drawable drawable) {
			_drawable = drawable;
			setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
		}

		@Override
		public void draw(Canvas canvas) {
			// override the draw to facilitate refresh function later
			if(_drawable != null) {
				_drawable.draw(canvas);
			}
		}
	}
}
