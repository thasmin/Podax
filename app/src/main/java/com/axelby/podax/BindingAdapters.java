package com.axelby.podax;

import android.databinding.BindingAdapter;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

@SuppressWarnings("unused")
public class BindingAdapters {

	@BindingAdapter("android:enabled")
	public static void setEnabled(View view, boolean isEnabled) {
		view.setEnabled(isEnabled);
	}

	@BindingAdapter("app:picassoImageUrl")
	public static void loadImageUrlViaPicasso(ImageView image, String url) {
		if (url == null || url.length() == 0)
			return;
		Picasso.with(image.getContext()).load(url).fit().into(image);
	}

	@BindingAdapter("android:onChange")
	public static void setOnChangeListener(CompoundButton button, CompoundButton.OnCheckedChangeListener listener) {
		button.setOnCheckedChangeListener(listener);
	}

}
