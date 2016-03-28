package com.axelby.podax;

import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableList;
import android.databinding.ViewDataBinding;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.Collection;

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

	@BindingAdapter({"app:children", "app:childLayout"})
	public static <T> void setChildren(ViewGroup parent, Collection<T> children, @LayoutRes int layoutId) {
		if (children == null) {
			parent.removeAllViews();
			return;
		}

		// called 3 times: on initial load with just title, then when loaded from db, then when update intent received
		parent.removeAllViews();
		for (T child : children)
			addBoundChild(parent, layoutId, child, -1);

		if (children instanceof ObservableList<?>) {
			ObservableList<T> observables = (ObservableList<T>) children;
			observables.addOnListChangedCallback(new ObservableList.OnListChangedCallback<ObservableList<T>>() {
				@Override
				public void onChanged(ObservableList<T> sender) {
					for (T child : sender)
						addBoundChild(parent, layoutId, child, -1);
				}

				@Override
				public void onItemRangeChanged(ObservableList<T> sender, int positionStart, int itemCount) {
					for (int i = positionStart; i < positionStart + itemCount; ++i) {
						parent.removeViewAt(i);
						addBoundChild(parent, layoutId, sender.get(i), i);
					}
				}

				@Override
				public void onItemRangeInserted(ObservableList<T> sender, int positionStart, int itemCount) {
					for (int i = positionStart; i < positionStart + itemCount; ++i)
						addBoundChild(parent, layoutId, sender.get(i), i);
				}

				@Override
				public void onItemRangeMoved(ObservableList<T> sender, int fromPosition, int toPosition, int itemCount) {
					for (int i = fromPosition; i < fromPosition + itemCount; ++i)
						parent.removeViewAt(i);
					for (int i = toPosition; i < toPosition + itemCount; ++i)
						addBoundChild(parent, layoutId, sender.get(i), i);
				}

				@Override
				public void onItemRangeRemoved(ObservableList<T> sender, int positionStart, int itemCount) {
					for (int i = positionStart; i < positionStart + itemCount; ++i)
						parent.removeViewAt(i);
				}
			});
		}
	}

	private static <T> void addBoundChild(ViewGroup parent, @LayoutRes int layoutId, T child, int position) {
		ViewDataBinding v = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), layoutId, parent, false);
		v.setVariable(com.axelby.podax.BR.episode, child);
		v.executePendingBindings();
		if (position == -1)
			parent.addView(v.getRoot());
		else
			parent.addView(v.getRoot(), position);
	}

}
